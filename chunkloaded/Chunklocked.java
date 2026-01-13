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
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Chunklocked implements ModInitializer {
	public static final String MOD_ID = "chunk-locked";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * The custom semi-transparent barrier block for chunk boundaries.
	 * Initialized during mod initialization to avoid static initialization issues.
	 */
	public static class_2248 BARRIER_BLOCK_V2;

	/**
	 * The global advancement credit manager.
	 * Tracks player progression and awards credits for advancement completions.
	 */
	private static AdvancementCreditManager creditManager;

	/**
	 * The configuration manager for advancement rewards.
	 */
	private static ConfigManager configManager;

	/**
	 * The persistent data storage for chunk unlock progression.
	 */
	private static ChunkUnlockData persistentData;

	/**
	 * The chunk manager facade for unlock operations.
	 */
	private static ChunkManager chunkManager;

	/**
	 * The chunk access manager for border enforcement.
	 */
	private static ChunkAccessManager chunkAccessManager;

	/**
	 * The barrier manager for physical chunk borders.
	 */
	private static ChunkBarrierManager barrierManager;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Chunk Locked mod...");

		// Register custom blocks FIRST (before any other code uses them)
		// Use copy() from glass block to avoid "Block id not set" error
		BARRIER_BLOCK_V2 = class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "barrier_block_v2"),
				new BarrierBlockV2(net.minecraft.class_4970.class_2251.method_9630(net.minecraft.class_2246.field_10555)
						.method_9629(-1.0f, 3600000.0f) // Unbreakable hardness, max blast resistance
						.method_42327()));
		LOGGER.info("Registered custom barrier block: barrier_block_v2");

		// Register network packets FIRST before any handlers
		registerPackets();

		// Register block break protection to prevent holes in barriers
		chunkloaded.border.BlockBreakProtection.register();

		// Register Nether portal detection for auto-unlock
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

		// Load configuration into credit manager
		creditManager.loadConfig(config);

		// Register server lifecycle events for persistence BEFORE advancement callbacks
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Server started, loading chunk unlock data...");
			persistentData = ChunkUnlockData.getOrCreate(server, creditManager);

			// Initialize chunk manager and access manager
			chunkManager = new ChunkManager(persistentData, creditManager);
			chunkAccessManager = new ChunkAccessManager(chunkManager);

			// Initialize barrier manager
			barrierManager = new ChunkBarrierManager();

			LOGGER.info("Chunk unlock data loaded successfully");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server stopping, ensuring chunk unlock data is saved...");
			if (persistentData != null) {
				persistentData.markDirtyAndSave();
			}
		});

		// Register advancement completion listener AFTER lifecycle events
		AdvancementCompletedCallback.EVENT.register((player, advancement, criterionName) -> {
			LOGGER.info("Player {} completed advancement: {} (criterion: {})",
					player.method_5477().getString(),
					advancement.comp_1919(),
					criterionName);

			// Award credits for the advancement
			creditManager.onAdvancementCompleted(player, advancement, criterionName);

			// Sync updated credits to client
			var playerData = creditManager.getPlayerData(player.method_5667());
			if (playerData != null) {
				NotificationManager.syncCreditsToClient(player,
						playerData.getAvailableCredits(),
						playerData.getTotalAdvancementsCompleted());
			}

			// Mark data as dirty to trigger save
			// CRITICAL FIX: Check if persistentData is available, as it may not be
			// initialized yet
			// in very early advancement completions (e.g., immediate spawn advancements)
			if (persistentData != null) {
				persistentData.markDirtyAndSave();
				LOGGER.debug("Saved advancement completion for player {}", player.method_5477().getString());
			} else {
				LOGGER.warn("persistentData is null during advancement completion for {}! " +
						"Data may not be saved! This usually means SERVER_STARTED event hasn't fired yet.",
						player.method_5477().getString());
				// The data will still be in creditManager's playerData map and will be saved
				// when SERVER_STARTED initializes persistentData
			}
		});

		// Register player join event to sync initial credit data
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.method_32311();
			LOGGER.info("=== Player {} joined ===", player.method_5477().getString());

			// Initialize managers if not already done (for singleplayer/integrated server)
			if (chunkManager == null || chunkAccessManager == null || barrierManager == null) {
				LOGGER.info("Managers not initialized, initializing now for integrated server...");
				if (persistentData == null) {
					LOGGER.info("Loading ChunkUnlockData from disk...");
					persistentData = ChunkUnlockData.getOrCreate(server, creditManager);
					LOGGER.info("ChunkUnlockData loaded, creditManager has {} players", creditManager.getPlayerCount());
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
				LOGGER.info("Managers initialized successfully for integrated server");
			}

			// Get player's current credit data
			var playerData = creditManager.getPlayerData(player.method_5667());
			if (playerData != null) {
				NotificationManager.syncCreditsToClient(player,
						playerData.getAvailableCredits(),
						playerData.getTotalAdvancementsCompleted());
				LOGGER.debug("Credit data synced to {}", player.method_5477().getString());
			} else {
				LOGGER.debug("No credit data found for {}; syncing zero credits", player.method_5477().getString());
				NotificationManager.syncCreditsToClient(player, 0, 0);
			}

			// Auto-unlock spawn chunk ONLY if this is the very first player in the world
			// (i.e., no chunks are unlocked globally yet) OR if player spawns outside
			// unlocked area
			LOGGER.info("Checking global chunk state for world initialization...");
			if (chunkAccessManager != null && chunkManager != null) {
				// Check if ANY chunks are unlocked globally (by any player)
				Set<net.minecraft.class_1923> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
				LOGGER.info("World has {} total unlocked chunks across all players", globalUnlockedChunks.size());

				// Calculate player's spawn chunk
				int chunkX = ((int) Math.floor(player.method_23317())) >> 4;
				int chunkZ = ((int) Math.floor(player.method_23321())) >> 4;
				net.minecraft.class_1923 spawnChunk = new net.minecraft.class_1923(chunkX, chunkZ);

				LOGGER.info("Player {} spawned at chunk [{}, {}]", player.method_5477().getString(), chunkX, chunkZ);

				if (globalUnlockedChunks.isEmpty()) {
					// This is the FIRST player ever - initialize the spawn area
					LOGGER.info("First player ever detected! Initializing 2x2 spawn area...");

					// Unlock a 2x2 chunk area centered on spawn
					net.minecraft.class_1923[] spawnChunks = {
							new net.minecraft.class_1923(chunkX, chunkZ), // Current chunk
							new net.minecraft.class_1923(chunkX + 1, chunkZ), // East
							new net.minecraft.class_1923(chunkX, chunkZ + 1), // South
							new net.minecraft.class_1923(chunkX + 1, chunkZ + 1) // Southeast
					};

					// Force unlock all 4 spawn chunks for this player
					for (net.minecraft.class_1923 chunk : spawnChunks) {
						chunkManager.forceUnlockChunk(player.method_5667(), chunk, server.method_30002());
					}

					LOGGER.info("Auto-unlocked 2x2 spawn area [{}, {}] to [{}, {}] for first player {}",
							chunkX, chunkZ, chunkX + 1, chunkZ + 1, player.method_5477().getString());

					player.method_64398(net.minecraft.class_2561.method_43470(
							"§aWelcome! Your 2x2 spawn area has been unlocked. Complete advancements to unlock more chunks!"));
				} else if (!globalUnlockedChunks.contains(spawnChunk)) {
					// Player spawned OUTSIDE the unlocked area - unlock 2x2 around their spawn
					LOGGER.warn("Player {} spawned in LOCKED chunk [{}, {}]! Unlocking 2x2 area around spawn...",
							player.method_5477().getString(), chunkX, chunkZ);

					net.minecraft.class_1923[] spawnChunks = {
							new net.minecraft.class_1923(chunkX, chunkZ), // Current chunk
							new net.minecraft.class_1923(chunkX + 1, chunkZ), // East
							new net.minecraft.class_1923(chunkX, chunkZ + 1), // South
							new net.minecraft.class_1923(chunkX + 1, chunkZ + 1) // Southeast
					};

					for (net.minecraft.class_1923 chunk : spawnChunks) {
						chunkManager.forceUnlockChunk(player.method_5667(), chunk, server.method_30002());
					}

					LOGGER.info("Emergency spawn unlock: 2x2 area [{}, {}] to [{}, {}] for player {}",
							chunkX, chunkZ, chunkX + 1, chunkZ + 1, player.method_5477().getString());

					player.method_64398(net.minecraft.class_2561.method_43470(
							"§eYour spawn was outside the safe area. Unlocked a 2x2 area around you!"));
				} else {
					// World already has unlocked chunks and player spawned safely inside
					LOGGER.info("Player {} joining existing world with {} unlocked chunks - spawned in unlocked chunk [{}, {}]",
							player.method_5477().getString(), globalUnlockedChunks.size(), chunkX, chunkZ);
				}

				// Always initialize barriers using global state (for both first and subsequent
				// players)
				LOGGER.info("Initializing barriers using global state...");
				chunkManager.initializeBarriersForPlayer(server.method_30002(), player.method_5667());
				LOGGER.info("Barriers initialized for player {}", player.method_5477().getString());
			}
		});

		// Register server lifecycle events for persistence
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Server started, loading chunk unlock data...");
			persistentData = ChunkUnlockData.getOrCreate(server, creditManager);

			// Initialize chunk manager and access manager
			chunkManager = new ChunkManager(persistentData, creditManager);
			chunkAccessManager = new ChunkAccessManager(chunkManager);

			// Initialize barrier manager
			barrierManager = new ChunkBarrierManager();

			LOGGER.info("Chunk unlock data loaded successfully");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server stopping, ensuring chunk unlock data is saved...");
			if (persistentData != null) {
				persistentData.markDirtyAndSave();
			}
		});

		LOGGER.info("Chunk Locked mod initialized successfully");
	}

	/**
	 * Registers custom network packets.
	 * Must be called before any packet handlers are registered.
	 */
	private void registerPackets() {
		// Register clientbound (server -> client) packets
		PayloadTypeRegistry.playS2C().register(CreditUpdatePacket.ID, CreditUpdatePacket.CODEC);

		LOGGER.debug("Registered network packets");
	}

	/**
	 * Gets the global credit manager instance.
	 *
	 * @return The credit manager
	 */
	public static AdvancementCreditManager getCreditManager() {
		return creditManager;
	}

	/**
	 * Gets the global configuration manager instance.
	 *
	 * @return The configuration manager
	 */
	public static ConfigManager getConfigManager() {
		return configManager;
	}

	/**
	 * Gets the persistent data storage instance.
	 *
	 * @return The persistent data storage, or null if server hasn't started
	 */
	public static ChunkUnlockData getPersistentData() {
		return persistentData;
	}

	/**
	 * Gets the chunk manager instance.
	 *
	 * @return The chunk manager, or null if server hasn't started
	 */
	public static ChunkManager getChunkManager() {
		return chunkManager;
	}

	/**
	 * Gets the chunk access manager instance for border enforcement.
	 *
	 * @return The chunk access manager, or null if server hasn't started
	 */

	/**
	 * Gets the barrier manager instance for physical chunk borders.
	 *
	 * @return The barrier manager, or null if server hasn't started
	 */
	public static ChunkBarrierManager getBarrierManager() {
		return barrierManager;
	}

	public static ChunkAccessManager getChunkAccessManager() {
		return chunkAccessManager;
	}
}