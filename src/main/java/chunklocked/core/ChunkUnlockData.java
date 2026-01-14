package chunklocked.core;

import chunklocked.advancement.AdvancementCreditManager;
import chunklocked.advancement.PlayerProgressionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
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
    private final Set<ChunkPos> globalUnlockedChunks = new HashSet<>();

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
        Path worldDir = server.getServerDirectory()
                .resolve("saves")
                .resolve(server.getWorldData().getLevelName());
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
            // Use the NbtIo method with a NbtAccounter - allow unlimited size
            CompoundTag nbt = NbtIo.readCompressed(dataFile, NbtAccounter.unlimitedHeap());

            // Check data version
            int version = nbt.getInt("DataVersion").orElse(0);
            if (version == 1) {
                LOGGER.info("Detected legacy chunk unlock data (v1). Will migrate to v2 on next save.");
            } else if (version != DATA_VERSION && version != 0) {
                LOGGER.warn("Loading chunk unlock data with version {} (expected {}). Migration may be needed.",
                        version, DATA_VERSION);
            }

            // Load player data
            if (nbt.contains("Players")) {
                CompoundTag playersNbt = nbt.getCompound("Players").orElse(new CompoundTag());
                for (String uuidString : playersNbt.keySet()) {
                    try {
                        UUID playerUuid = UUID.fromString(uuidString);
                        CompoundTag playerNbt = playersNbt.getCompound(uuidString).orElse(new CompoundTag());

                        // Load credits (validate >= 0)
                        int credits = Math.max(0, playerNbt.getInt("Credits").orElse(0));

                        // Load total advancements
                        int totalAdvancements = Math.max(0, playerNbt.getInt("TotalAdvancements").orElse(0));

                        // Create progression data
                        PlayerProgressionData progressionData = new PlayerProgressionData();
                        progressionData.setAvailableCredits(credits);
                        progressionData.setTotalAdvancementsCompleted(totalAdvancements);

                        // Load rewarded advancements
                        if (playerNbt.contains("RewardedAdvancements")) {
                            ListTag rewardedList = playerNbt.getList("RewardedAdvancements").orElse(new ListTag());
                            for (int i = 0; i < rewardedList.size(); i++) {
                                String advancementId = rewardedList.getString(i).orElse("");
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
            if (nbt.contains("GlobalUnlockedChunks")) {
                ListTag chunksList = nbt.getList("GlobalUnlockedChunks").orElse(new ListTag());
                for (int i = 0; i < chunksList.size(); i++) {
                    CompoundTag chunkNbt = chunksList.getCompound(i).orElse(new CompoundTag());
                    int x = chunkNbt.getInt("x").orElse(0);
                    int z = chunkNbt.getInt("z").orElse(0);
                    globalUnlockedChunks.add(new ChunkPos(x, z));
                }
                LOGGER.info("Loaded {} globally unlocked chunks (v3 format)", globalUnlockedChunks.size());
            } else {
                // Migration: Aggregate all per-player chunks from v2 format into global set
                LOGGER.info("Migrating from v2 (per-player) to v3 (global) chunk format...");
                if (nbt.contains("Players")) {
                    CompoundTag playersNbt = nbt.getCompound("Players").orElse(new CompoundTag());
                    int totalMigrated = 0;
                    for (String uuidString : playersNbt.keySet()) {
                        CompoundTag playerNbt = playersNbt.getCompound(uuidString).orElse(new CompoundTag());
                        if (playerNbt.contains("UnlockedChunks")) {
                            ListTag chunksList = playerNbt.getList("UnlockedChunks").orElse(new ListTag());
                            for (int i = 0; i < chunksList.size(); i++) {
                                CompoundTag chunkNbt = chunksList.getCompound(i).orElse(new CompoundTag());
                                int x = chunkNbt.getInt("x").orElse(0);
                                int z = chunkNbt.getInt("z").orElse(0);
                                if (globalUnlockedChunks.add(new ChunkPos(x, z))) {
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
            CompoundTag nbt = new CompoundTag();

            // Write data version
            nbt.putInt("DataVersion", DATA_VERSION);

            // Write player data (credits and advancements)
            CompoundTag playersNbt = new CompoundTag();
            for (Map.Entry<UUID, PlayerProgressionData> entry : creditManager.getAllPlayerData().entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerProgressionData progressionData = entry.getValue();

                CompoundTag playerNbt = new CompoundTag();

                // Write credits
                playerNbt.putInt("Credits", progressionData.getAvailableCredits());

                // Write total advancements
                playerNbt.putInt("TotalAdvancements", progressionData.getTotalAdvancementsCompleted());

                // Write rewarded advancements
                ListTag rewardedList = new ListTag();
                for (String advancementId : progressionData.getRewardedAdvancements()) {
                    rewardedList.add(StringTag.valueOf(advancementId));
                }
                playerNbt.put("RewardedAdvancements", rewardedList);

                playersNbt.put(playerUuid.toString(), playerNbt);
            }
            nbt.put("Players", playersNbt);

            // Write global unlocked chunks (v3 format)
            ListTag chunksList = new ListTag();
            for (ChunkPos chunk : globalUnlockedChunks) {
                CompoundTag chunkNbt = new CompoundTag();
                chunkNbt.putInt("x", chunk.x);
                chunkNbt.putInt("z", chunk.z);
                chunksList.add(chunkNbt);
            }
            nbt.put("GlobalUnlockedChunks", chunksList);

            // Create parent directory if needed
            java.nio.file.Files.createDirectories(dataFile.getParent());

            // Write to file using NbtIo with Path parameter
            NbtIo.writeCompressed(nbt, dataFile);

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
    public void unlockChunk(ChunkPos chunkPos) {
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
    public boolean isChunkUnlocked(ChunkPos chunkPos) {
        return globalUnlockedChunks.contains(chunkPos);
    }

    /**
     * Locks (removes) a chunk that was previously unlocked.
     * If the chunk is not unlocked, this is a no-op.
     *
     * @param chunkPos The chunk position to lock
     */
    public void lockChunk(ChunkPos chunkPos) {
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
    public Set<ChunkPos> getGlobalUnlockedChunks() {
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