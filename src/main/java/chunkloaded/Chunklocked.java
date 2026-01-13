package chunkloaded;

import chunkloaded.advancement.AdvancementCompletedCallback;
import chunkloaded.advancement.AdvancementCreditManager;
import chunkloaded.advancement.NotificationManager;
import chunkloaded.block.BarrierBlockV2;
import chunkloaded.border.ChunkBarrierManager;
import chunkloaded.command.ChunklockedCommand;
import chunkloaded.config.AdvancementRewardConfig;
import chunkloaded.config.ConfigManager;
import chunkloaded.core.ChunkAccessManager;
import chunkloaded.core.ChunkManager;
import chunkloaded.core.ChunkUnlockData;
import chunkloaded.network.CreditUpdatePacket;
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

import java.util.Set;

public class Chunklocked implements ModInitializer {
	public static final String MOD_ID = "chunk-locked";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
						.noLootTable()));
		LOGGER.info("Registered custom barrier block: barrier_block_v2");

		// Register network packets
		registerPackets();

		// Register block break protection
		chunkloaded.border.BlockBreakProtection.register();

		// Register Nether portal detection
		chunkloaded.portal.NetherPortalDetection.register();

		// Register locked chunk penalty system
		chunkloaded.penalty.LockedChunkPenaltySystem.register();

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

			creditManager.onAdvancementCompleted(player, advancement, criterionName);

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