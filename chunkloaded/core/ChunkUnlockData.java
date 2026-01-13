package chunkloaded.core;

import chunkloaded.advancement.AdvancementCreditManager;
import chunkloaded.advancement.PlayerProgressionData;
import net.minecraft.class_1923;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2505;
import net.minecraft.class_2507;
import net.minecraft.class_2519;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Persistent storage for chunk unlock progression data.
 * <p>
 * Saves and loads player credit data and rewarded advancements using NBT files.
 * This data persists across game sessions.
 * <p>
 * Uses a simplified file-based approach with NbtIo for direct file
 * serialization,
 * avoiding complex PersistentState API issues.
 */
public class ChunkUnlockData {

    private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");
    private static final String FILE_NAME = "chunklocked_data.nbt";
    private static final int DATA_VERSION = 3; // Updated to v3 for global chunk storage

    private final AdvancementCreditManager creditManager;
    private final Path dataFile;
    private boolean isDirty = false;

    /**
     * Stores globally unlocked chunks (shared across all players).
     * In multiplayer, chunks are unlocked globally - if one player unlocks it, all
     * can access it.
     */
    private final Set<class_1923> globalUnlockedChunks = new HashSet<>();

    /**
     * Creates a new ChunkUnlockData instance with the specified credit manager.
     *
     * @param creditManager The credit manager to associate with this data
     * @param worldDir      The world directory to store data in
     */
    public ChunkUnlockData(AdvancementCreditManager creditManager, Path worldDir) {
        this.creditManager = creditManager;
        this.dataFile = worldDir.resolve(FILE_NAME);
    }

    /**
     * Gets or creates the ChunkUnlockData for the server.
     *
     * @param server        The Minecraft server
     * @param creditManager The credit manager to associate with this data
     * @return The loaded or newly created ChunkUnlockData
     */
    public static ChunkUnlockData getOrCreate(MinecraftServer server, AdvancementCreditManager creditManager) {
        // Use the world-specific save directory, NOT the run directory
        // This ensures data is per-world, not shared across all worlds
        Path worldDir = server.method_3831()
                .resolve("saves")
                .resolve(server.method_27728().method_150());
        ChunkUnlockData data = new ChunkUnlockData(creditManager, worldDir);
        data.load();
        return data;
    }

    /**
     * Loads the data from the NBT file, handling both v1 (legacy) and v2 formats.
     */
    public void load() {
        if (!java.nio.file.Files.exists(dataFile)) {
            LOGGER.info("No existing chunk unlock data found at {}, starting fresh", dataFile.toAbsolutePath());
            return;
        }

        try {
            LOGGER.info("Loading chunk unlock data from {}", dataFile.toAbsolutePath());
            // Use the NbtIo method with a NbtSizeTracker - allow unlimited size
            class_2487 nbt = class_2507.method_30613(dataFile, new class_2505(Long.MAX_VALUE, Integer.MAX_VALUE));

            // Check data version
            int version = nbt.method_10550("DataVersion").orElse(0);
            if (version == 1) {
                LOGGER.info("Detected legacy chunk unlock data (v1). Will migrate to v2 on next save.");
            } else if (version != DATA_VERSION && version != 0) {
                LOGGER.warn("Loading chunk unlock data with version {} (expected {}). Migration may be needed.",
                        version, DATA_VERSION);
            }

            // Load player data
            if (nbt.method_10545("Players")) {
                class_2487 playersNbt = nbt.method_10562("Players").orElse(new class_2487());
                for (String uuidString : playersNbt.method_10541()) {
                    try {
                        UUID playerUuid = UUID.fromString(uuidString);
                        class_2487 playerNbt = playersNbt.method_10562(uuidString).orElse(new class_2487());

                        // Load credits (validate >= 0)
                        int credits = Math.max(0, playerNbt.method_10550("Credits").orElse(0));

                        // Load total advancements
                        int totalAdvancements = Math.max(0, playerNbt.method_10550("TotalAdvancements").orElse(0));

                        // Create progression data
                        PlayerProgressionData progressionData = new PlayerProgressionData();
                        progressionData.setAvailableCredits(credits);
                        progressionData.setTotalAdvancementsCompleted(totalAdvancements);

                        // Load rewarded advancements
                        if (playerNbt.method_10545("RewardedAdvancements")) {
                            class_2499 rewardedList = playerNbt.method_10554("RewardedAdvancements").orElse(new class_2499());
                            for (int i = 0; i < rewardedList.size(); i++) {
                                String advancementId = rewardedList.method_10608(i).orElse("");
                                if (!advancementId.isEmpty()) {
                                    // Mark as rewarded without incrementing total
                                    progressionData.markAdvancementRewarded(advancementId);
                                }
                            }
                            // Correct the total since markAdvancementRewarded increments it
                            progressionData.setTotalAdvancementsCompleted(totalAdvancements);
                        }

                        // Load progression data into credit manager
                        creditManager.loadPlayerData(playerUuid, progressionData);

                        LOGGER.debug("Loaded progression data for player {}: {} credits, {} advancements",
                                playerUuid, credits, totalAdvancements);

                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in chunk unlock data: {}", uuidString, e);
                    }
                }
            }

            // Load global unlocked chunks (v3 format)
            // First try new v3 format (GlobalUnlockedChunks)
            if (nbt.method_10545("GlobalUnlockedChunks")) {
                class_2499 chunksList = nbt.method_10554("GlobalUnlockedChunks").orElse(new class_2499());
                for (int i = 0; i < chunksList.size(); i++) {
                    class_2487 chunkNbt = chunksList.method_10602(i).orElse(new class_2487());
                    int x = chunkNbt.method_10550("x").orElse(0);
                    int z = chunkNbt.method_10550("z").orElse(0);
                    globalUnlockedChunks.add(new class_1923(x, z));
                }
                LOGGER.info("Loaded {} globally unlocked chunks (v3 format)", globalUnlockedChunks.size());
            } else {
                // Migration: Aggregate all per-player chunks from v2 format into global set
                LOGGER.info("Migrating from v2 (per-player) to v3 (global) chunk format...");
                if (nbt.method_10545("Players")) {
                    class_2487 playersNbt = nbt.method_10562("Players").orElse(new class_2487());
                    int totalMigrated = 0;
                    for (String uuidString : playersNbt.method_10541()) {
                        class_2487 playerNbt = playersNbt.method_10562(uuidString).orElse(new class_2487());
                        if (playerNbt.method_10545("UnlockedChunks")) {
                            class_2499 chunksList = playerNbt.method_10554("UnlockedChunks").orElse(new class_2499());
                            for (int i = 0; i < chunksList.size(); i++) {
                                class_2487 chunkNbt = chunksList.method_10602(i).orElse(new class_2487());
                                int x = chunkNbt.method_10550("x").orElse(0);
                                int z = chunkNbt.method_10550("z").orElse(0);
                                if (globalUnlockedChunks.add(new class_1923(x, z))) {
                                    totalMigrated++;
                                }
                            }
                        }
                    }
                    LOGGER.info("Migrated {} unique chunks from v2 per-player format", totalMigrated);
                }
            }

            LOGGER.info("Loaded chunk unlock data for {} players with {} global unlocked chunks",
                    creditManager.getPlayerCount(), globalUnlockedChunks.size());

        } catch (IOException e) {
            LOGGER.error("Failed to load chunk unlock data", e);
        }
    }

    /**
     * Saves the data to the NBT file in v3 format.
     */
    public void save() {
        try {
            class_2487 nbt = new class_2487();

            // Write data version
            nbt.method_10569("DataVersion", DATA_VERSION);

            // Write player data (credits and advancements)
            class_2487 playersNbt = new class_2487();
            for (Map.Entry<UUID, PlayerProgressionData> entry : creditManager.getAllPlayerData().entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerProgressionData progressionData = entry.getValue();

                class_2487 playerNbt = new class_2487();

                // Write credits
                playerNbt.method_10569("Credits", progressionData.getAvailableCredits());

                // Write total advancements
                playerNbt.method_10569("TotalAdvancements", progressionData.getTotalAdvancementsCompleted());

                // Write rewarded advancements
                class_2499 rewardedList = new class_2499();
                for (String advancementId : progressionData.getRewardedAdvancements()) {
                    rewardedList.add(class_2519.method_23256(advancementId));
                }
                playerNbt.method_10566("RewardedAdvancements", rewardedList);

                playersNbt.method_10566(playerUuid.toString(), playerNbt);
            }
            nbt.method_10566("Players", playersNbt);

            // Write global unlocked chunks (v3 format)
            class_2499 chunksList = new class_2499();
            for (class_1923 chunk : globalUnlockedChunks) {
                class_2487 chunkNbt = new class_2487();
                chunkNbt.method_10569("x", chunk.field_9181);
                chunkNbt.method_10569("z", chunk.field_9180);
                chunksList.add(chunkNbt);
            }
            nbt.method_10566("GlobalUnlockedChunks", chunksList);

            // Create parent directory if needed
            java.nio.file.Files.createDirectories(dataFile.getParent());

            // Write to file using NbtIo with Path parameter
            class_2507.method_30614(nbt, dataFile);

            LOGGER.debug("Saved chunk unlock data: {} players, {} global chunks",
                    creditManager.getPlayerCount(), globalUnlockedChunks.size());
            isDirty = false;

        } catch (IOException e) {
            LOGGER.error("Failed to save chunk unlock data", e);
        }
    }

    /**
     * Marks this data as dirty, indicating it needs to be saved.
     */
    public void markDirty() {
        this.isDirty = true;
    }

    /**
     * Marks this data as dirty and immediately saves it.
     */
    public void markDirtyAndSave() {
        markDirty();
        save();
    }

    /**
     * Checks if this data is dirty (needs saving).
     *
     * @return true if the data needs to be saved
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Gets all player UUIDs that have progression data loaded.
     *
     * @return A set of all player UUIDs with credit/advancement data
     */
    public Set<UUID> getAllPlayerUuids() {
        return creditManager.getAllPlayerData().keySet();
    }

    // ========== GLOBAL CHUNK STORAGE OPERATIONS ==========

    /**
     * Unlocks a chunk globally. If the chunk is already unlocked, this is a no-op.
     * In multiplayer, chunks are shared - when unlocked by any player, all can
     * access it.
     *
     * @param chunkPos The chunk position to unlock
     */
    public void unlockChunk(class_1923 chunkPos) {
        if (globalUnlockedChunks.add(chunkPos)) {
            markDirty();
        }
    }

    /**
     * Checks if a chunk is unlocked globally.
     *
     * @param chunkPos The chunk position to check
     * @return true if the chunk is unlocked, false otherwise
     */
    public boolean isChunkUnlocked(class_1923 chunkPos) {
        return globalUnlockedChunks.contains(chunkPos);
    }

    /**
     * Locks (removes) a chunk that was previously unlocked.
     * If the chunk is not unlocked, this is a no-op.
     *
     * @param chunkPos The chunk position to lock
     */
    public void lockChunk(class_1923 chunkPos) {
        if (globalUnlockedChunks.remove(chunkPos)) {
            markDirty();
        }
    }

    /**
     * Gets all globally unlocked chunks.
     * Returns an immutable copy to prevent external modification.
     *
     * @return An immutable set of all unlocked chunk positions
     */
    public Set<class_1923> getGlobalUnlockedChunks() {
        return Set.copyOf(globalUnlockedChunks);
    }

    /**
     * Clears all unlocked chunks globally.
     */
    public void clearAllChunks() {
        if (!globalUnlockedChunks.isEmpty()) {
            globalUnlockedChunks.clear();
            markDirty();
        }
    }
}