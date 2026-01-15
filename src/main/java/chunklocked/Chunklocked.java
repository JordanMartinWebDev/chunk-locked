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

			// Initialize spawn area for first player
			handleInitialPlayerSpawn(player, server.overworld());
		});

		// Register player respawn event - this fires AFTER the new player entity is
		// created
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			LOGGER.info("=== Player {} respawned (alive={}) ===", newPlayer.getName().getString(), alive);

			// Get the world from the new player
			net.minecraft.server.level.ServerLevel world = (net.minecraft.server.level.ServerLevel) newPlayer.level();

			// Handle respawn location check
			handlePlayerRespawn(newPlayer, world);
		});

		LOGGER.info("Chunk Locked mod initialized successfully");
	}

	/**
	 * Checks if a player is spawning in a locked chunk during first join.
	 * Only handles initial world setup, NOT respawns.
	 * 
	 * @param player The player to check
	 * @param world  The world the player is in (must be overworld)
	 */
	public static void handleInitialPlayerSpawn(net.minecraft.server.level.ServerPlayer player,
			net.minecraft.server.level.ServerLevel world) {
		// Only handle overworld spawns
		if (!world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
			return;
		}

		// Check if managers are initialized
		if (chunkAccessManager == null || chunkManager == null) {
			return;
		}

		Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();

		// If first player ever, unlock initial spawn area
		if (globalUnlockedChunks.isEmpty()) {
			int chunkX = ((int) Math.floor(player.getX())) >> 4;
			int chunkZ = ((int) Math.floor(player.getZ())) >> 4;

			LOGGER.info("First player ever detected! Initializing 2x2 spawn area...");
			ChunkPos[] spawnChunks = {
					new ChunkPos(chunkX, chunkZ),
					new ChunkPos(chunkX + 1, chunkZ),
					new ChunkPos(chunkX, chunkZ + 1),
					new ChunkPos(chunkX + 1, chunkZ + 1)
			};
			for (ChunkPos chunk : spawnChunks) {
				chunkManager.forceUnlockChunk(player.getUUID(), chunk, world);
			}
			player.sendSystemMessage(Component.literal(
					"§aWelcome! Your 2x2 spawn area has been unlocked. Complete advancements to unlock more chunks!"));
			chunkManager.initializeBarriersForPlayer(world, player.getUUID());
		}
	}

	/**
	 * Handles player respawn after death. If the respawn location is in a locked
	 * chunk,
	 * teleports the player to the nearest unlocked chunk instead.
	 * This preserves normal bed/spawn point behavior when in unlocked chunks.
	 * 
	 * @param player The player who respawned
	 * @param world  The world the player respawned in
	 */
	public static void handlePlayerRespawn(net.minecraft.server.level.ServerPlayer player,
			net.minecraft.server.level.ServerLevel world) {
		LOGGER.info("=== handlePlayerRespawn called for player {} ===", player.getName().getString());

		// Only handle overworld respawns
		if (!world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
			LOGGER.info("Not in overworld, skipping respawn handling");
			return;
		}

		// Check if managers are initialized
		if (chunkAccessManager == null || chunkManager == null) {
			LOGGER.warn("Managers not initialized! Cannot handle respawn.");
			return;
		}

		// Check if player respawned in a locked chunk
		Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
		int chunkX = ((int) Math.floor(player.getX())) >> 4;
		int chunkZ = ((int) Math.floor(player.getZ())) >> 4;
		ChunkPos respawnChunk = new ChunkPos(chunkX, chunkZ);

		LOGGER.info("Player respawned at chunk {} (pos: {}, {})", respawnChunk, player.getX(), player.getZ());
		LOGGER.info("Is chunk unlocked? {}", globalUnlockedChunks.contains(respawnChunk));

		// If respawn location (bed/world spawn) is in an unlocked chunk, do nothing
		// This preserves normal bed respawn behavior
		if (globalUnlockedChunks.contains(respawnChunk)) {
			LOGGER.debug("Player {} respawned in unlocked chunk {} - no relocation needed",
					player.getName().getString(), respawnChunk);
			return;
		}

		// Respawn location is in a locked chunk - need to relocate player
		if (!globalUnlockedChunks.contains(respawnChunk)) {
			LOGGER.info("Player {} respawned in LOCKED chunk {}! Finding safe location...",
					player.getName().getString(), respawnChunk);

			// Find the nearest unlocked chunk
			ChunkPos nearestUnlocked = findNearestUnlockedChunk(respawnChunk, globalUnlockedChunks);

			if (nearestUnlocked != null) {
				// Teleport to center of the safe chunk at a safe Y level
				double safeX = nearestUnlocked.getMinBlockX() + 8.0;
				double safeZ = nearestUnlocked.getMinBlockZ() + 8.0;
				double safeY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						(int) safeX, (int) safeZ) + 1.0;

				player.teleportTo(safeX, safeY, safeZ);
				player.sendSystemMessage(Component.literal(
						"§eYour respawn point was in a locked area. Teleported to the nearest unlocked chunk!"));

				LOGGER.info("Teleported player to safe chunk {} at ({}, {}, {})",
						nearestUnlocked, safeX, safeY, safeZ);
			} else {
				// No unlocked chunks found (shouldn't happen if initial spawn worked)
				LOGGER.error("No unlocked chunks found for respawn! This shouldn't happen.");
				player.sendSystemMessage(Component.literal(
						"§cError: No safe spawn location found. Contact an admin."));
			}
		}
	}

	/**
	 * Finds the nearest unlocked chunk to a given position.
	 * Uses a spiral search pattern to find the closest unlocked chunk.
	 * 
	 * @param origin         The starting chunk position
	 * @param unlockedChunks Set of all unlocked chunks
	 * @return The nearest unlocked chunk, or null if none found within search
	 *         radius
	 */
	private static ChunkPos findNearestUnlockedChunk(ChunkPos origin, Set<ChunkPos> unlockedChunks) {
		if (unlockedChunks.isEmpty()) {
			return null;
		}

		// First check if any adjacent chunks are unlocked (most common case)
		ChunkPos[] adjacent = {
				new ChunkPos(origin.x - 1, origin.z),
				new ChunkPos(origin.x + 1, origin.z),
				new ChunkPos(origin.x, origin.z - 1),
				new ChunkPos(origin.x, origin.z + 1)
		};
		for (ChunkPos adj : adjacent) {
			if (unlockedChunks.contains(adj)) {
				return adj;
			}
		}

		// Search in expanding square spiral up to 16 chunks away
		int maxRadius = 16;
		ChunkPos nearest = null;
		double nearestDistSq = Double.MAX_VALUE;

		for (int radius = 1; radius <= maxRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					// Only check chunks on the current radius perimeter
					if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}

					ChunkPos candidate = new ChunkPos(origin.x + dx, origin.z + dz);
					if (unlockedChunks.contains(candidate)) {
						double distSq = dx * dx + dz * dz;
						if (distSq < nearestDistSq) {
							nearestDistSq = distSq;
							nearest = candidate;
						}
					}
				}
			}
			// If we found something at this radius, return it (closest possible)
			if (nearest != null) {
				return nearest;
			}
		}

		// If nothing found in search radius, just return any unlocked chunk
		return unlockedChunks.iterator().next();
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