package chunkloaded.core;

import chunkloaded.Chunklocked;
import chunkloaded.advancement.AdvancementCreditManager;
import chunkloaded.border.ChunkBarrierManager;
import chunkloaded.core.area.AreaDetector;
import chunkloaded.core.area.PlayableArea;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Public API facade for the chunk unlock system.
 * 
 * Provides a unified interface for querying and modifying chunk unlock state,
 * spending credits, and managing playable areas. All credit-based operations
 * are atomic and validate preconditions before modifying state.
 * 
 * Thread-safe for concurrent read operations. Write operations should be
 * synchronized by callers (typically the server thread).
 */
public class ChunkManager {

  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  private final ChunkUnlockData chunkData;
  private final AdvancementCreditManager creditManager;
  private final AreaDetector areaDetector;

  /**
   * Creates a new ChunkManager with the specified data stores.
   *
   * @param chunkData     The persistent chunk unlock storage
   * @param creditManager The credit management system
   */
  public ChunkManager(ChunkUnlockData chunkData, AdvancementCreditManager creditManager) {
    this.chunkData = chunkData;
    this.creditManager = creditManager;
    this.areaDetector = new AreaDetector();
  }

  /**
   * Gets all globally unlocked chunks.
   * In multiplayer, chunks are shared - when unlocked by any player, all can
   * access it.
   *
   * @return A set of all globally unlocked chunk positions
   */
  public Set<ChunkPos> getGlobalUnlockedChunks() {
    return chunkData.getGlobalUnlockedChunks();
  }

  /**
   * Calculates frontier chunks (locked but adjacent to ANY unlocked chunk
   * globally).
   * This ensures barriers are placed correctly in multi-player scenarios.
   *
   * @return Set of frontier chunk positions across all players
   */
  public Set<ChunkPos> calculateGlobalFrontierChunks() {
    Set<ChunkPos> allUnlockedChunks = getGlobalUnlockedChunks();
    Set<ChunkPos> frontier = new HashSet<>();

    // For each unlocked chunk (any player), check all 4 cardinal neighbors
    for (ChunkPos unlocked : allUnlockedChunks) {
      ChunkPos[] neighbors = {
          new ChunkPos(unlocked.x, unlocked.z - 1), // North
          new ChunkPos(unlocked.x, unlocked.z + 1), // South
          new ChunkPos(unlocked.x + 1, unlocked.z), // East
          new ChunkPos(unlocked.x - 1, unlocked.z) // West
      };

      for (ChunkPos neighbor : neighbors) {
        // Add to frontier if not already unlocked (by any player)
        if (!allUnlockedChunks.contains(neighbor)) {
          frontier.add(neighbor);
        }
      }
    }

    return frontier;
  }

  // ========== CHUNK QUERY OPERATIONS ==========

  /**
   * Checks if a chunk is unlocked globally.
   * In multiplayer, chunks are shared - this checks the global unlock state.
   *
   * @param player The player UUID (unused, kept for API compatibility)
   * @param pos    The chunk position
   * @return true if the chunk is unlocked, false otherwise
   * @deprecated Use isChunkUnlockedGlobally(ChunkPos) instead
   */
  @Deprecated
  public boolean isChunkUnlocked(UUID player, ChunkPos pos) {
    return chunkData.isChunkUnlocked(pos);
  }

  /**
   * Checks if a chunk is unlocked globally.
   *
   * @param pos The chunk position
   * @return true if the chunk is unlocked, false otherwise
   */
  public boolean isChunkUnlockedGlobally(ChunkPos pos) {
    return chunkData.isChunkUnlocked(pos);
  }

  /**
   * Gets all globally unlocked chunks.
   * In multiplayer, all players share the same unlocked chunks.
   *
   * @param player The player UUID (unused, kept for API compatibility)
   * @return An immutable set of all unlocked chunk positions
   * @deprecated Use getGlobalUnlockedChunks() instead
   */
  @Deprecated
  public Set<ChunkPos> getPlayerChunks(UUID player) {
    return chunkData.getGlobalUnlockedChunks();
  }

  /**
   * Gets the count of globally unlocked chunks.
   *
   * @param player The player UUID (unused, kept for API compatibility)
   * @return The number of globally unlocked chunks
   * @deprecated Use getGlobalChunkCount() instead
   */
  @Deprecated
  public int getPlayerChunkCount(UUID player) {
    return chunkData.getGlobalUnlockedChunks().size();
  }

  /**
   * Gets the count of globally unlocked chunks.
   *
   * @return The number of globally unlocked chunks
   */
  public int getGlobalChunkCount() {
    return chunkData.getGlobalUnlockedChunks().size();
  }

  // ========== AREA QUERY OPERATIONS ==========

  /**
   * Gets all contiguous playable areas (connected components) globally.
   * Triggers area detection if not cached.
   * In multiplayer, all players share the same unlocked areas.
   *
   * @param player The player UUID (unused, kept for API compatibility)
   * @return A list of detected playable areas
   * @deprecated Use getGlobalAreas() instead
   */
  @Deprecated
  public List<PlayableArea> getPlayerAreas(UUID player) {
    Set<ChunkPos> unlockedChunks = chunkData.getGlobalUnlockedChunks();
    return areaDetector.detectAreas(unlockedChunks);
  }

  /**
   * Gets all contiguous playable areas (connected components) globally.
   *
   * @return A list of detected playable areas
   */
  public List<PlayableArea> getGlobalAreas() {
    Set<ChunkPos> unlockedChunks = chunkData.getGlobalUnlockedChunks();
    return areaDetector.detectAreas(unlockedChunks);
  }

  /**
   * Gets the playable area containing a specific chunk, if any.
   * Uses global unlock state.
   *
   * @param player The player UUID (unused, kept for API compatibility)
   * @param chunk  The chunk position
   * @return An Optional containing the area if found
   * @deprecated Use getGlobalAreaContainingChunk(ChunkPos) instead
   */
  @Deprecated
  public Optional<PlayableArea> getAreaContainingChunk(UUID player, ChunkPos chunk) {
    Set<ChunkPos> unlockedChunks = chunkData.getGlobalUnlockedChunks();
    List<PlayableArea> areas = areaDetector.detectAreas(unlockedChunks);
    return areas.stream()
        .filter(area -> area.contains(chunk))
        .findFirst();
  }

  /**
   * Gets the globally playable area containing a specific chunk, if any.
   *
   * @param chunk The chunk position
   * @return An Optional containing the area if found
   */
  public Optional<PlayableArea> getGlobalAreaContainingChunk(ChunkPos chunk) {
    Set<ChunkPos> unlockedChunks = chunkData.getGlobalUnlockedChunks();
    List<PlayableArea> areas = areaDetector.detectAreas(unlockedChunks);
    return areas.stream()
        .filter(area -> area.contains(chunk))
        .findFirst();
  }

  // ========== CREDIT QUERY OPERATIONS ==========

  /**
   * Gets the available credits for a player.
   *
   * @param player The player UUID
   * @return The number of available credits
   */
  public int getAvailableCredits(UUID player) {
    return creditManager.getPlayerData(player).getAvailableCredits();
  }

  /**
   * Gets the total advancements completed by a player.
   *
   * @param player The player UUID
   * @return The count of completed advancements
   */
  public int getTotalAdvancements(UUID player) {
    return creditManager.getPlayerData(player).getTotalAdvancementsCompleted();
  }

  // ========== CREDIT-BASED UNLOCK OPERATIONS ==========

  /**
   * Attempts to unlock a chunk for a player by spending 1 credit.
   * 
   * This operation is atomic: either the chunk is unlocked and credit is spent,
   * or neither happens. The operation fails if:
   * - The player has no available credits
   * - The chunk is already unlocked
   * 
   * Area detection cache is invalidated on successful unlock.
   *
   * @param player The player UUID
   * @param pos    The chunk position to unlock
   * @return true if the unlock succeeded, false if it failed (insufficient
   *         credits or already unlocked)
   */
  public boolean tryUnlockChunk(UUID player, ChunkPos pos) {
    // CRITICAL: Check if chunk is globally unlocked (by ANY player)
    // In multiplayer, chunks are shared - if Player 1 unlocked it, Player 2 can't
    // unlock it again
    if (isChunkUnlockedGlobally(pos)) {
      LOGGER.debug("Chunk {} already unlocked globally (cannot unlock again)", pos);
      return false;
    }

    // Check if player has credits
    int credits = getAvailableCredits(player);
    if (credits <= 0) {
      LOGGER.debug("Player {} attempted to unlock chunk {} but has no credits", player, pos);
      return false;
    }

    // Spend credit
    creditManager.getPlayerData(player).spendCredits(1);

    // Unlock chunk globally
    chunkData.unlockChunk(pos);
    chunkData.markDirty();

    // Invalidate area cache
    areaDetector.invalidateCache();

    LOGGER.info("Player {} unlocked chunk {} globally (credits remaining: {})", player, pos,
        getAvailableCredits(player));
    return true;
  }

  /**
   * Attempts to unlock multiple adjacent chunks for a player.
   * Unlocks as many chunks as possible with available credits.
   *
   * @param player    The player UUID
   * @param positions The chunk positions to unlock
   * @return The count of successfully unlocked chunks
   */
  public int tryUnlockChunks(UUID player, Set<ChunkPos> positions) {
    int unlockedCount = 0;
    for (ChunkPos pos : positions) {
      if (tryUnlockChunk(player, pos)) {
        unlockedCount++;
      } else {
        // Stop if unlock fails (likely due to insufficient credits)
        break;
      }
    }
    return unlockedCount;
  }

  // ========== ADMIN OPERATIONS (No credit cost) ==========

  /**
   * Admin operation: Unlocks a chunk without spending credits.
   * Useful for initial setup or moderation.
   *
   * @param player The player UUID
   * @param pos    The chunk position to unlock
   */
  public void forceUnlockChunk(UUID player, ChunkPos pos) {
    forceUnlockChunk(player, pos, null);
  }

  /**
   * Admin operation: Unlocks a chunk without spending credits.
   * Optionally updates barriers in the specified world.
   *
   * @param player The player UUID (for logging purposes)
   * @param pos    The chunk position to unlock
   * @param world  The server world (null to skip barrier updates)
   */
  public void forceUnlockChunk(UUID player, ChunkPos pos, ServerLevel world) {
    if (!isChunkUnlockedGlobally(pos)) {
      chunkData.unlockChunk(pos);
      chunkData.markDirty();
      areaDetector.invalidateCache();

      // Update barriers if world is provided
      if (world != null) {
        updateBarriersForUnlock(world, player, pos);
      }

      LOGGER.info("Admin force-unlocked chunk {} globally (requested by player {})", pos, player);
    }
  }

  /**
   * Admin operation: Unlocks multiple chunks without spending credits.
   *
   * @param player    The player UUID (for logging purposes)
   * @param positions The chunk positions to unlock
   * @return The count of unlocked chunks
   */
  public int forceUnlockChunks(UUID player, Set<ChunkPos> positions) {
    int unlockedCount = 0;
    for (ChunkPos pos : positions) {
      if (!isChunkUnlockedGlobally(pos)) {
        chunkData.unlockChunk(pos);
        unlockedCount++;
      }
    }
    if (unlockedCount > 0) {
      chunkData.markDirty();
      areaDetector.invalidateCache();
    }
    return unlockedCount;
  }

  /**
   * Admin operation: Locks a previously unlocked chunk.
   *
   * @param player The player UUID (for logging purposes)
   * @param pos    The chunk position to lock
   */
  public void lockChunk(UUID player, ChunkPos pos) {
    if (isChunkUnlockedGlobally(pos)) {
      chunkData.lockChunk(pos);
      chunkData.markDirty();
      areaDetector.invalidateCache();
      LOGGER.info("Locked chunk {} globally (requested by player {})", pos, player);
    }
  }

  /**
   * Admin operation: Locks multiple chunks.
   *
   * @param player    The player UUID (for logging purposes)
   * @param positions The chunk positions to lock
   * @return The count of locked chunks
   */
  public int lockChunks(UUID player, Set<ChunkPos> positions) {
    int lockedCount = 0;
    for (ChunkPos pos : positions) {
      if (isChunkUnlockedGlobally(pos)) {
        chunkData.lockChunk(pos);
        lockedCount++;
      }
    }
    if (lockedCount > 0) {
      chunkData.markDirty();
      areaDetector.invalidateCache();
    }
    return lockedCount;
  }

  // ========== CREDIT OPERATIONS ==========

  /**
   * Admin operation: Adds credits to a player's account.
   *
   * @param player The player UUID
   * @param amount The number of credits to add (must be positive)
   * @throws IllegalArgumentException if amount is negative
   */
  public void addCredits(UUID player, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Credit amount must be non-negative");
    }
    if (amount > 0) {
      creditManager.getPlayerData(player).addCredits(amount);
      LOGGER.info("Added {} credits to player {} (total: {})", amount, player, getAvailableCredits(player));
    }
  }

  /**
   * Admin operation: Sets a player's credits to a specific amount.
   *
   * @param player The player UUID
   * @param amount The new credit amount (must be non-negative)
   * @throws IllegalArgumentException if amount is negative
   */
  public void setCredits(UUID player, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Credit amount must be non-negative");
    }
    int currentCredits = getAvailableCredits(player);
    int difference = amount - currentCredits;

    if (difference > 0) {
      creditManager.getPlayerData(player).addCredits(difference);
    } else if (difference < 0) {
      creditManager.getPlayerData(player).spendCredits(-difference);
    }

    LOGGER.info("Set credits for player {} to {}", player, amount);
  }

  /**
   * Admin operation: Clears all globally unlocked chunks.
   * WARNING: This affects ALL players in multiplayer!
   *
   * @param player The player UUID (for logging purposes)
   */
  public void clearPlayerChunks(UUID player) {
    int beforeCount = getGlobalChunkCount();
    chunkData.clearAllChunks();
    chunkData.markDirty();
    areaDetector.invalidateCache();
    LOGGER.info("Cleared {} global chunks (requested by player {})", beforeCount, player);
  }

  // ========== PERSISTENCE OPERATIONS ==========

  /**
   * Saves all data to persistent storage.
   * Called periodically by the server lifecycle hooks.
   */
  public void save() {
    chunkData.save();
  }

  /**
   * Loads all data from persistent storage.
   * Called when the world starts up.
   */
  public void load() {
    chunkData.load();
  }

  /**
   * Checks if data needs to be saved.
   *
   * @return true if there are unsaved changes
   */
  public boolean isDirty() {
    return chunkData.isDirty();
  }

  // ========== BARRIER MANAGEMENT ==========

  /**
   * Updates barriers after a chunk unlock.
   * Removes barriers from the unlocked chunk and updates adjacent chunks.
   * 
   * @param world         The server world
   * @param player        The player UUID
   * @param unlockedChunk The chunk that was just unlocked
   */
  /**
   * Public method to update barriers after a chunk unlock.
   * Called when a chunk is unlocked to remove barriers and update frontier
   * barriers.
   *
   * @param world         The server world
   * @param player        The player UUID
   * @param unlockedChunk The newly unlocked chunk position
   */
  public void updateBarriersAfterUnlock(ServerLevel world, UUID player, ChunkPos unlockedChunk) {
    updateBarriersForUnlock(world, player, unlockedChunk);
  }

  private void updateBarriersForUnlock(ServerLevel world, UUID player, ChunkPos unlockedChunk) {
    ChunkBarrierManager barrierManager = Chunklocked.getBarrierManager();
    if (barrierManager == null) {
      LOGGER.warn("Barrier manager not initialized, skipping barrier updates");
      return;
    }

    // CRITICAL: Use GLOBAL unlocked chunks, not per-player!
    // When any player unlocks a chunk, we need to consider ALL unlocked chunks
    // from ALL players to correctly place/remove barriers
    Set<ChunkPos> allUnlocked = getGlobalUnlockedChunks();
    LOGGER.info("Updating barriers after unlock: {} global unlocked chunks", allUnlocked.size());

    // Calculate frontier using global state (locked chunks adjacent to ANY unlocked
    // chunk)
    Set<ChunkPos> allLocked = calculateGlobalFrontierChunks();
    LOGGER.info("Frontier has {} locked chunks adjacent to unlocked areas", allLocked.size());

    // Update barriers (removes barriers from unlocked chunk, updates adjacent)
    barrierManager.updateBarriers(world, unlockedChunk, allUnlocked, allLocked);
  }

  /**
   * Places initial barriers for all locked chunks adjacent to unlocked chunks.
   * Should be called when a player joins or when barriers need to be regenerated.
   * 
   * @param world  The server world
   * @param player The player UUID
   */
  public void initializeBarriersForPlayer(ServerLevel world, UUID player) {
    LOGGER.info("=== initializeBarriersForPlayer START for player {} ===", player);
    ChunkBarrierManager barrierManager = Chunklocked.getBarrierManager();
    if (barrierManager == null) {
      LOGGER.warn("Barrier manager not initialized, skipping barrier initialization");
      return;
    }

    // CRITICAL: Clear all existing barriers before reinitializing with global state
    // This prevents duplicate/stale barriers from previous partial initializations
    LOGGER.info("Clearing all existing barriers before global reinitialization...");
    barrierManager.clearAllBarriers(world);

    // Get GLOBAL unlocked chunks (all players) and frontier (locked but adjacent)
    Set<ChunkPos> globalUnlockedChunks = getGlobalUnlockedChunks();
    LOGGER.info("Global state: {} total unlocked chunks across all players", globalUnlockedChunks.size());

    Set<ChunkPos> globalFrontierChunks = calculateGlobalFrontierChunks();
    LOGGER.info("Global frontier: {} frontier chunks for barrier placement", globalFrontierChunks.size());

    // Place barriers using GLOBAL state (not just this player's state)
    for (ChunkPos lockedChunk : globalFrontierChunks) {
      LOGGER.debug("Placing barriers around frontier chunk [{}, {}]", lockedChunk.x, lockedChunk.z);
      barrierManager.placeBarriers(world, lockedChunk, globalUnlockedChunks);
    }

    LOGGER.info("=== initializeBarriersForPlayer DONE - Initialized barriers using global frontier ===");
  }

  /**
   * Calculates the frontier chunks (locked chunks adjacent to unlocked chunks)
   * for a specific player.
   * Note: For barrier placement, use calculateGlobalFrontierChunks() instead.
   * 
   * @param player The player UUID
   * @return Set of frontier chunk positions
   */
  public Set<ChunkPos> calculateFrontierChunks(UUID player) {
    Set<ChunkPos> unlockedChunks = getPlayerChunks(player);
    Set<ChunkPos> frontier = new HashSet<>();

    // For each unlocked chunk, check all 4 cardinal neighbors
    for (ChunkPos unlocked : unlockedChunks) {
      ChunkPos[] neighbors = {
          new ChunkPos(unlocked.x, unlocked.z - 1), // North
          new ChunkPos(unlocked.x, unlocked.z + 1), // South
          new ChunkPos(unlocked.x + 1, unlocked.z), // East
          new ChunkPos(unlocked.x - 1, unlocked.z) // West
      };

      for (ChunkPos neighbor : neighbors) {
        // Add to frontier if not already unlocked
        if (!unlockedChunks.contains(neighbor)) {
          frontier.add(neighbor);
        }
      }
    }

    return frontier;
  }
}
