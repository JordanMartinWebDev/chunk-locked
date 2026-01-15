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
    private static final int DATA_VERSION = 5; // Updated to v5 for starter items tracking

    private final AdvancementCreditManager creditManager;
    private final Path dataFile;
    private boolean isDirty = false;

    /**
     * The difficulty mode for this world (DISABLED, EASY, or EXTREME).
     * Default is DISABLED (mod inactive).
     */
    private ChunklockedMode mode = ChunklockedMode.DISABLED;

    /**
     * Flag to ensure mode is set only once (at world creation).
     * Once true, attempting to change the mode will throw an exception.
     */
    private boolean modeSetAtCreation = false;

    /**
     * Tracks which players have received starter items.
     * Maps player UUID to whether they've received the items.
     * Only applies to EASY and EXTREME modes.
     */
    private final Set<UUID> playersWithStarterItems = new HashSet<>();

    /**
     * Controls whether barriers are enabled or disabled.
     * When false, barriers are removed from the world.
     * This is an emergency escape hatch for players stuck by barrier bugs.
     * Default is true (barriers enabled).
     */
    private boolean barriersEnabled = true;

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
                LOGGER.info("Detected legacy chunk unlock data (v1). Will migrate to v4 on next save.");
            } else if (version != DATA_VERSION && version != 0) {
                LOGGER.warn("Loading chunk unlock data with version {} (expected {}). Migration may be needed.",
                        version, DATA_VERSION);
            }

            // Load mode (v4 feature, defaults to DISABLED for older saves)
            if (nbt.contains("Mode")) {
                String modeString = nbt.getString("Mode").orElse("DISABLED");
                try {
                    this.mode = ChunklockedMode.valueOf(modeString);
                    this.modeSetAtCreation = true; // Mark as set to prevent changes
                    LOGGER.info("Loaded ChunklockedMode: {}", this.mode);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid mode '{}' in save data, defaulting to DISABLED", modeString);
                    this.mode = ChunklockedMode.DISABLED;
                }
            } else {
                LOGGER.info("No mode found in save data (pre-v4), defaulting to DISABLED");
                this.mode = ChunklockedMode.DISABLED;
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

            // Load starter items tracking (v5 feature)
            if (nbt.contains("StarterItemsGiven")) {
                ListTag starterItemsList = nbt.getList("StarterItemsGiven").orElse(new ListTag());
                for (int i = 0; i < starterItemsList.size(); i++) {
                    String uuidString = starterItemsList.getString(i).orElse("");
                    if (!uuidString.isEmpty()) {
                        try {
                            playersWithStarterItems.add(UUID.fromString(uuidString));
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in starter items list: {}", uuidString);
                        }
                    }
                }
                LOGGER.info("Loaded {} players with starter items", playersWithStarterItems.size());
            }

            // Load barriers enabled flag (v5 feature, defaults to true for compatibility)
            if (nbt.contains("BarriersEnabled")) {
                barriersEnabled = nbt.getBoolean("BarriersEnabled").orElse(true);
                LOGGER.info("Barriers enabled: {}", barriersEnabled);
            } else {
                barriersEnabled = true; // Default to enabled for older saves
                LOGGER.debug("No barriers flag in save data, defaulting to enabled");
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

            // Write mode (v4 feature)
            nbt.putString("Mode", mode.name());

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

            // Write starter items tracking (v5 feature)
            ListTag starterItemsList = new ListTag();
            for (UUID playerId : playersWithStarterItems) {
                starterItemsList.add(StringTag.valueOf(playerId.toString()));
            }
            nbt.put("StarterItemsGiven", starterItemsList);

            // Write barriers enabled flag (v5 feature)
            nbt.putBoolean("BarriersEnabled", barriersEnabled);

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

    // ========== MODE MANAGEMENT ==========

    /**
     * Gets the current difficulty mode for this world.
     *
     * @return The ChunklockedMode (DISABLED, EASY, or EXTREME)
     */
    public ChunklockedMode getMode() {
        return mode;
    }

    /**
     * Sets the difficulty mode for this world.
     * <p>
     * <b>IMPORTANT</b>: This can only be called ONCE, typically at world creation.
     * Subsequent calls will throw an IllegalStateException to prevent gameplay
     * exploits.
     * <p>
     * The mode determines:
     * <ul>
     * <li>Whether the mod is active (EASY/EXTREME) or disabled (DISABLED)</li>
     * <li>How many credits are awarded for advancements</li>
     * <li>Whether starter items are given</li>
     * </ul>
     *
     * @param mode The mode to set
     * @throws IllegalStateException    if the mode has already been set
     * @throws IllegalArgumentException if mode is null
     */
    public void setMode(ChunklockedMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("ChunklockedMode cannot be null");
        }

        if (modeSetAtCreation) {
            throw new IllegalStateException(
                    "Cannot change ChunklockedMode after world creation. Current mode: " + this.mode);
        }

        this.mode = mode;
        this.modeSetAtCreation = true;
        markDirty();
        LOGGER.info("ChunklockedMode set to {} (immutable)", mode);
    }

    /**
     * Checks if the mode has been set and is immutable.
     *
     * @return true if the mode has been set and cannot be changed
     */
    public boolean isModeImmutable() {
        return modeSetAtCreation;
    }

    // ========== STARTER ITEMS MANAGEMENT ==========

    /**
     * Checks if a player has already received starter items.
     *
     * @param playerId The player's UUID
     * @return true if the player has received starter items, false otherwise
     */
    public boolean hasReceivedStarterItems(UUID playerId) {
        return playersWithStarterItems.contains(playerId);
    }

    /**
     * Marks a player as having received starter items.
     * <p>
     * This prevents the player from receiving starter items again.
     *
     * @param playerId The player's UUID
     */
    public void markStarterItemsGiven(UUID playerId) {
        if (playersWithStarterItems.add(playerId)) {
            markDirty();
            LOGGER.debug("Marked player {} as having received starter items", playerId);
        }
    }

    /**
     * Gets the count of players who have received starter items.
     *
     * @return The number of players with starter items
     */
    public int getStarterItemsGivenCount() {
        return playersWithStarterItems.size();
    }

    // ========== BARRIER MANAGEMENT ==========

    /**
     * Checks if barriers are currently enabled.
     *
     * @return true if barriers are enabled (default), false if disabled
     */
    public boolean areBarriersEnabled() {
        return barriersEnabled;
    }

    /**
     * Sets whether barriers are enabled.
     * <p>
     * This is an emergency toggle for when players get stuck due to barrier bugs.
     * When disabled, barriers are removed from the world but progression still
     * works.
     *
     * @param enabled true to enable barriers, false to disable
     */
    public void setBarriersEnabled(boolean enabled) {
        if (this.barriersEnabled != enabled) {
            this.barriersEnabled = enabled;
            markDirty();
            LOGGER.info("Barriers {}", enabled ? "enabled" : "disabled");
        }
    }
}