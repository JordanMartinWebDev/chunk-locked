package chunklocked.core;

import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages chunk access control and validation for the world border system.
 * 
 * This class provides O(1) lookup performance for chunk access checks and
 * handles adjacency validation for unlock operations. It serves as the
 * enforcement layer for the chunk-based progression system.
 * 
 * <p>
 * Thread-safe for concurrent read operations. Write operations should be
 * synchronized by callers (typically the server thread).
 * </p>
 * 
 * <p>
 * <strong>Performance Characteristics:</strong>
 * </p>
 * <ul>
 * <li>canAccessChunk: O(1) - uses HashMap + HashSet lookup</li>
 * <li>isAdjacentToUnlockedChunk: O(1) - checks 4 neighbors with
 * HashSet.contains()</li>
 * <li>getUnlockedChunks: O(1) - returns reference to immutable view</li>
 * </ul>
 * 
 * @see ChunkManager For chunk unlock operations
 * @see ChunkUnlockData For persistent storage
 */
public class ChunkAccessManager {

  private final ChunkManager chunkManager;

  /**
   * Creates a new ChunkAccessManager.
   *
   * @param chunkManager The chunk manager providing unlock state data
   */
  public ChunkAccessManager(@NotNull ChunkManager chunkManager) {
    this.chunkManager = chunkManager;
  }

  /**
   * Checks if a player can access a specific chunk.
   * 
   * <p>
   * This is the primary enforcement method for the border system. A player
   * can access a chunk if and only if it is globally unlocked (unlocked by any
   * player).
   * In multiplayer, chunks are shared resources.
   * </p>
   * 
   * <p>
   * <strong>Performance:</strong> O(1) lookup time using HashSet
   * </p>
   *
   * @param playerId The player's UUID (unused, all players share the same
   *                 unlocked chunks)
   * @param pos      The chunk position to check
   * @return true if the chunk is globally unlocked, false otherwise
   * @throws NullPointerException if pos is null
   */
  public boolean canAccessChunk(@NotNull UUID playerId, @NotNull ChunkPos pos) {
    if (pos == null) {
      throw new NullPointerException("Chunk position cannot be null");
    }

    return chunkManager.isChunkUnlockedGlobally(pos);
  }

  /**
   * Checks if a chunk is adjacent to any globally unlocked chunk.
   * 
   * <p>
   * Adjacency is defined as 4-directional connectivity (north, south, east,
   * west). Diagonal adjacency does not count.
   * </p>
   * 
   * <p>
   * This method is used for validating chunk unlock requests to ensure
   * playable areas remain connected. Uses global unlock state.
   * </p>
   * 
   * <p>
   * <strong>Performance:</strong> O(1) - checks 4 neighbors with HashSet lookups
   * </p>
   *
   * @param playerId The player's UUID (unused, all players share the same
   *                 unlocked chunks)
   * @param pos      The chunk position to check
   * @return true if the chunk is adjacent to at least one globally unlocked chunk
   * @throws NullPointerException if pos is null
   */
  public boolean isAdjacentToUnlockedChunk(@NotNull UUID playerId, @NotNull ChunkPos pos) {
    if (pos == null) {
      throw new NullPointerException("Chunk position cannot be null");
    }

    Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();

    // Check all 4 cardinal directions (north, south, east, west)
    ChunkPos north = new ChunkPos(pos.x, pos.z - 1);
    ChunkPos south = new ChunkPos(pos.x, pos.z + 1);
    ChunkPos east = new ChunkPos(pos.x + 1, pos.z);
    ChunkPos west = new ChunkPos(pos.x - 1, pos.z);

    return globalUnlockedChunks.contains(north) ||
        globalUnlockedChunks.contains(south) ||
        globalUnlockedChunks.contains(east) ||
        globalUnlockedChunks.contains(west);
  }

  /**
   * Gets all globally unlocked chunks.
   * 
   * <p>
   * In multiplayer, all players share the same unlocked chunks.
   * Returns an immutable copy to prevent external modifications.
   * </p>
   * 
   * <p>
   * <strong>Performance:</strong> O(1) - returns reference to immutable view
   * </p>
   *
   * @param playerId The player's UUID (unused, all players share the same
   *                 unlocked chunks)
   * @return An immutable set of globally unlocked chunk positions
   */
  @NotNull
  public Set<ChunkPos> getUnlockedChunks(@NotNull UUID playerId) {
    return chunkManager.getGlobalUnlockedChunks();
  }

  /**
   * Gets all chunks adjacent to globally unlocked chunks.
   * 
   * <p>
   * These are the chunks that can potentially be unlocked next, forming
   * the "frontier" of playable areas. Uses global unlock state.
   * </p>
   * 
   * <p>
   * <strong>Performance:</strong> O(n) where n is the number of globally
   * unlocked chunks, typically acceptable for UI rendering.
   * </p>
   *
   * @param playerId The player's UUID (unused, all players share the same
   *                 frontier)
   * @return A set of chunk positions that are adjacent to globally unlocked
   *         chunks
   */
  @NotNull
  public Set<ChunkPos> getAdjacentLockedChunks(@NotNull UUID playerId) {
    Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
    Set<ChunkPos> adjacentLocked = new HashSet<>();

    // For each globally unlocked chunk, check all 4 neighbors
    for (ChunkPos unlocked : globalUnlockedChunks) {
      ChunkPos north = new ChunkPos(unlocked.x, unlocked.z - 1);
      ChunkPos south = new ChunkPos(unlocked.x, unlocked.z + 1);
      ChunkPos east = new ChunkPos(unlocked.x + 1, unlocked.z);
      ChunkPos west = new ChunkPos(unlocked.x - 1, unlocked.z);

      // Add neighbors that are NOT already unlocked
      if (!globalUnlockedChunks.contains(north))
        adjacentLocked.add(north);
      if (!globalUnlockedChunks.contains(south))
        adjacentLocked.add(south);
      if (!globalUnlockedChunks.contains(east))
        adjacentLocked.add(east);
      if (!globalUnlockedChunks.contains(west))
        adjacentLocked.add(west);
    }

    return adjacentLocked;
  }

  /**
   * Validates if a chunk can be unlocked.
   * 
   * <p>
   * A chunk can be unlocked if:
   * </p>
   * <ul>
   * <li>It is not already globally unlocked</li>
   * <li>It is adjacent to at least one globally unlocked chunk (maintains
   * connectivity)</li>
   * </ul>
   * 
   * <p>
   * Note: This method does NOT check credit availability. Credit checks
   * should be performed separately by the unlock operation handler.
   * </p>
   *
   * @param playerId The player's UUID (unused, all players share the same
   *                 frontier)
   * @param pos      The chunk position to validate
   * @return true if the chunk can be unlocked (adjacency-wise)
   * @throws NullPointerException if pos is null
   */
  public boolean canUnlockChunk(@NotNull UUID playerId, @NotNull ChunkPos pos) {
    if (pos == null) {
      throw new NullPointerException("Chunk position cannot be null");
    }

    // Can't unlock if already globally unlocked
    if (canAccessChunk(playerId, pos)) {
      return false;
    }

    // Must be adjacent to at least one globally unlocked chunk (connectivity
    // requirement)
    // Exception: If NO chunks are unlocked globally, allow any chunk (initial
    // unlock)
    Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
    if (globalUnlockedChunks.isEmpty()) {
      return true; // Initial unlock - any chunk is valid
    }

    return isAdjacentToUnlockedChunk(playerId, pos);
  }

  /**
   * Gets the count of globally unlocked chunks.
   * 
   * <p>
   * Useful for statistics and UI display.
   * </p>
   *
   * @param playerId The player's UUID (unused, returns global count for all
   *                 players)
   * @return The count of globally unlocked chunks
   */
  public int getUnlockedChunkCount(@NotNull UUID playerId) {
    return chunkManager.getGlobalChunkCount();
  }
}
