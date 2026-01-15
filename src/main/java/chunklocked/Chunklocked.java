package chunklocked;

import chunklocked.advancement.AdvancementCompletedCallback;
import chunklocked.advancement.AdvancementCreditManager;
import chunklocked.advancement.NotificationManager;
import chunklocked.block.BarrierBlockV2;
import chunklocked.border.ChunkBarrierManager;
import chunklocked.command.ChunklockedCommand;
import chunklocked.config.AdvancementRewardConfig;
import chunklocked.config.ConfigManager;
import chunklocked.core.ChunkAccessManager;
import chunklocked.core.ChunkManager;
import chunklocked.core.ChunkUnlockData;
import chunklocked.core.ChunklockedMode;
import chunklocked.core.ChunklockedWorldPresets;
import chunklocked.core.StarterItemsManager;
import chunklocked.network.CreditUpdatePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

public class Chunklocked implements ModInitializer {
	public static final String MOD_ID = "chunk-locked";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Creates an Identifier for this mod with the given path.
	 * 
	 * @param path The path component of the identifier
	 * @return An Identifier with namespace "chunk-locked" and the given path
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/**
	 * The custom semi-transparent barrier block for chunk boundaries.
	 */
	public static Block BARRIER_BLOCK_V2;

	private static AdvancementCreditManager creditManager;
	private static ConfigManager configManager;
	private static ChunkUnlockData persistentData;
	private static ChunkManager chunkManager;
	private static ChunkAccessManager chunkAccessManager;
	private static ChunkBarrierManager barrierManager;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Chunk Locked mod...");

		// Initialize world preset keys (actual presets are defined in data packs)
		ChunklockedWorldPresets.register();

		// Create ResourceKey for the block
		Identifier blockId = Identifier.fromNamespaceAndPath(MOD_ID, "barrier_block_v2");
		ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), blockId);

		// Register custom barrier block - must setId() before constructing the Block
		BARRIER_BLOCK_V2 = Registry.register(
				BuiltInRegistries.BLOCK,
				blockKey,
				new BarrierBlockV2(BlockBehaviour.Properties.of()
						.setId(blockKey)
						.mapColor(net.minecraft.world.level.material.MapColor.NONE)
						.strength(-1.0f, 3600000.0f)
						.noOcclusion()
						.noLootTable()
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
						.forceSolidOn()
						.isRedstoneConductor((state, level, pos) -> true)));
		LOGGER.info("Registered custom barrier block: barrier_block_v2");

		// Register network packets
		registerPackets();

		// Register block break protection
		chunklocked.border.BlockBreakProtection.register();

		// NonPlayerBlockChangeProtection removed - we now replace leaves during initial
		// barrier placement
		// This eliminates the need for scanning since leaves are the only naturally
		// decaying blocks

		// Register Nether portal detection
		chunklocked.portal.NetherPortalDetection.register();

		// Register locked chunk penalty system
		chunklocked.penalty.LockedChunkPenaltySystem.register();

		// Register commands
		CommandRegistrationCallback.EVENT.register(ChunklockedCommand::register);

		// Initialize configuration manager
		configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir());

		// Load configuration
		AdvancementRewardConfig config = configManager.loadConfig();
		LOGGER.info("Loaded configuration: {}", config);

		// Initialize the credit manager
		creditManager = new AdvancementCreditManager();
		creditManager.loadConfig(config);

		// Register server lifecycle events
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Server started, loading chunk unlock data...");
			persistentData = ChunkUnlockData.getOrCreate(server, creditManager);

			// Auto-detect mode from world settings on first load (brand new world)
			if (persistentData.getMode() == ChunklockedMode.DISABLED &&
					persistentData.getGlobalUnlockedChunks().isEmpty()) {
				ChunklockedMode detectedMode = detectModeFromWorldSettings(server);
				if (detectedMode != ChunklockedMode.DISABLED) {
					LOGGER.info("Auto-detected Chunklocked mode from world settings: {}", detectedMode);
					persistentData.setMode(detectedMode);
					persistentData.markDirtyAndSave();
				}
			}

			LOGGER.info("Chunklocked mode: {}", persistentData.getMode());

			chunkManager = new ChunkManager(persistentData, creditManager);
			chunkAccessManager = new ChunkAccessManager(chunkManager);
			barrierManager = new ChunkBarrierManager();
			LOGGER.info("Chunk unlock data loaded successfully");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server stopping, ensuring chunk unlock data is saved...");
			if (persistentData != null) {
				persistentData.markDirtyAndSave();
			}
		});

		// Register advancement completion listener
		AdvancementCompletedCallback.EVENT.register((player, advancement, criterionName) -> {
			LOGGER.info("Player {} completed advancement: {} (criterion: {})",
					player.getName().getString(),
					advancement.id(),
					criterionName);

			// Get the current mode from persistent data
			ChunklockedMode mode = (persistentData != null)
					? persistentData.getMode()
					: ChunklockedMode.DISABLED;

			creditManager.onAdvancementCompleted(player, advancement, criterionName, mode);

			var playerData = creditManager.getPlayerData(player.getUUID());
			if (playerData != null) {
				NotificationManager.syncCreditsToClient(player,
						playerData.getAvailableCredits(),
						playerData.getTotalAdvancementsCompleted());
			}

			if (persistentData != null) {
				persistentData.markDirtyAndSave();
			}
		});

		// Register player join event
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.getPlayer();
			LOGGER.info("=== Player {} joined ===", player.getName().getString());

			// Initialize managers if needed
			if (chunkManager == null || chunkAccessManager == null || barrierManager == null) {
				if (persistentData == null) {
					persistentData = ChunkUnlockData.getOrCreate(server, creditManager);
				}
				if (chunkManager == null) {
					chunkManager = new ChunkManager(persistentData, creditManager);
				}
				if (chunkAccessManager == null) {
					chunkAccessManager = new ChunkAccessManager(chunkManager);
				}
				if (barrierManager == null) {
					barrierManager = new ChunkBarrierManager();
				}
			}

			// Give starter items if applicable
			if (persistentData != null) {
				ChunklockedMode mode = persistentData.getMode();
				UUID playerId = player.getUUID();

				if (StarterItemsManager.shouldGiveStarterItems(mode,
						persistentData.hasReceivedStarterItems(playerId))) {
					StarterItemsManager.giveStarterItems(player);
					persistentData.markStarterItemsGiven(playerId);
					persistentData.markDirtyAndSave();
					LOGGER.info("Gave starter items to first-time player: {}",
							player.getName().getString());
				}
			}

			// Sync credit data to client
			var playerData = creditManager.getPlayerData(player.getUUID());
			if (playerData != null) {
				NotificationManager.syncCreditsToClient(player,
						playerData.getAvailableCredits(),
						playerData.getTotalAdvancementsCompleted());
			} else {
				NotificationManager.syncCreditsToClient(player, 0, 0);
			}

			// Auto-unlock spawn area logic
			if (chunkAccessManager != null && chunkManager != null) {
				Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
				int chunkX = ((int) Math.floor(player.getX())) >> 4;
				int chunkZ = ((int) Math.floor(player.getZ())) >> 4;
				ChunkPos spawnChunk = new ChunkPos(chunkX, chunkZ);

				if (globalUnlockedChunks.isEmpty()) {
					LOGGER.info("First player ever detected! Initializing 2x2 spawn area...");
					ChunkPos[] spawnChunks = {
							new ChunkPos(chunkX, chunkZ),
							new ChunkPos(chunkX + 1, chunkZ),
							new ChunkPos(chunkX, chunkZ + 1),
							new ChunkPos(chunkX + 1, chunkZ + 1)
					};
					for (ChunkPos chunk : spawnChunks) {
						chunkManager.forceUnlockChunk(player.getUUID(), chunk, server.overworld());
					}
					player.sendSystemMessage(Component.literal(
							"§aWelcome! Your 2x2 spawn area has been unlocked. Complete advancements to unlock more chunks!"));
				} else if (!globalUnlockedChunks.contains(spawnChunk)) {
					LOGGER.warn("Player spawned in LOCKED chunk! Unlocking 2x2 area...");
					ChunkPos[] spawnChunks = {
							new ChunkPos(chunkX, chunkZ),
							new ChunkPos(chunkX + 1, chunkZ),
							new ChunkPos(chunkX, chunkZ + 1),
							new ChunkPos(chunkX + 1, chunkZ + 1)
					};
					for (ChunkPos chunk : spawnChunks) {
						chunkManager.forceUnlockChunk(player.getUUID(), chunk, server.overworld());
					}
					player.sendSystemMessage(Component.literal(
							"§eYour spawn was outside the safe area. Unlocked a 2x2 area around you!"));
				}

				chunkManager.initializeBarriersForPlayer(server.overworld(), player.getUUID());
			}
		});

		LOGGER.info("Chunk Locked mod initialized successfully");
	}

	private void registerPackets() {
		PayloadTypeRegistry.playS2C().register(CreditUpdatePacket.TYPE, CreditUpdatePacket.CODEC);
		LOGGER.debug("Registered network packets");
	}

	/**
	 * Detects Chunklocked mode from temporary marker files created by the client.
	 * The WorldPresetsMixin creates marker files when our presets are added to the
	 * UI.
	 * 
	 * @param server The server instance
	 * @return Detected mode or DISABLED if no marker files found
	 */
	private static ChunklockedMode detectModeFromWorldSettings(net.minecraft.server.MinecraftServer server) {
		try {
			// Use Fabric Loader API to get game directory - works reliably across all
			// environments
			Path gameDir = FabricLoader.getInstance().getGameDir();

			LOGGER.info("Detecting mode from marker files...");
			LOGGER.info("  Game directory: {}", gameDir);

			Path easyMarker = gameDir.resolve("chunklocked_preset_chunk-locked_easy.tmp");
			Path extremeMarker = gameDir.resolve("chunklocked_preset_chunk-locked_extreme.tmp");

			LOGGER.info("  Looking for Easy marker at: {}", easyMarker);
			LOGGER.info("  Looking for Extreme marker at: {}", extremeMarker);
			LOGGER.info("  Easy marker exists: {}", java.nio.file.Files.exists(easyMarker));
			LOGGER.info("  Extreme marker exists: {}", java.nio.file.Files.exists(extremeMarker));

			ChunklockedMode detectedMode = ChunklockedMode.DISABLED;

			if (java.nio.file.Files.exists(easyMarker)) {
				String content = java.nio.file.Files.readString(easyMarker).trim();
				if (content.equals("chunk-locked:easy")) {
					detectedMode = ChunklockedMode.EASY;
					LOGGER.info("Detected Easy mode from marker file");
				}
				// Clean up the marker file
				java.nio.file.Files.delete(easyMarker);
			}

			if (java.nio.file.Files.exists(extremeMarker)) {
				String content = java.nio.file.Files.readString(extremeMarker).trim();
				if (content.equals("chunk-locked:extreme")) {
					// Extreme takes precedence if both markers exist (shouldn't happen)
					detectedMode = ChunklockedMode.EXTREME;
					LOGGER.info("Detected Extreme mode from marker file");
				}
				// Clean up the marker file
				java.nio.file.Files.delete(extremeMarker);
			}

			if (detectedMode == ChunklockedMode.DISABLED) {
				LOGGER.debug("No Chunklocked marker files found - normal world");
			}

			return detectedMode;

		} catch (Exception e) {
			LOGGER.error("Failed to detect mode from marker files", e);
			return ChunklockedMode.DISABLED;
		}
	}

	public static AdvancementCreditManager getCreditManager() {
		return creditManager;
	}

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	public static ChunkUnlockData getPersistentData() {
		return persistentData;
	}

	public static ChunkManager getChunkManager() {
		return chunkManager;
	}

	public static ChunkAccessManager getChunkAccessManager() {
		return chunkAccessManager;
	}

	public static ChunkBarrierManager getBarrierManager() {
		return barrierManager;
	}
}