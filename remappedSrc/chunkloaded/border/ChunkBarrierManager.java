package chunkloaded.border;

import chunkloaded.Chunklocked;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages barrier block placement and removal at chunk boundaries.
 * Creates physical walls using minecraft:barrier blocks to prevent
 * movement into locked chunks.
 * 
 * <p>
 * Barriers are placed at chunk boundaries between locked and unlocked
 * chunks from Y=-64 to Y=319 (full world height). Selective collision
 * is handled by BarrierBlockCollisionMixin to allow mobs/water/items
 * through while blocking players.
 * </p>
 * 
 * <p>
 * Thread-safe: Uses ConcurrentHashMap for multi-player environments.
 * </p>
 */
public class ChunkBarrierManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkBarrierManager.class);

  // Level height constants for 1.21
  private static final int MIN_WORLD_Y = -64;
  private static final int MAX_WORLD_Y = 319;

  // Chunk size in blocks
  private static final int CHUNK_SIZE = 16;

  /**
   * Tracks all barrier blocks managed by ChunkLocked.
   * Key: ChunkPos of the locked chunk
   * Value: Set of BlockPos where barriers are placed
   */
  private final Map<ChunkPos, Set<BlockPos>> chunkBarriers = new ConcurrentHashMap<>();

  /**
   * Global set of all barrier block positions for fast lookup.
   * Used by BarrierBlockCollisionMixin to identify ChunkLocked barriers.
   */
  private final Set<BlockPos> allBarrierPositions = ConcurrentHashMap.newKeySet();

  /**
   * Places barrier blocks around a locked chunk at boundaries with unlocked
   * chunks.
   * Smart placement: only creates barriers between locked/unlocked pairs.
   * 
   * @param world          The server world
   * @param lockedChunk    The chunk position to surround with barriers
   * @param unlockedChunks Set of all unlocked chunk positions for adjacency check
   */
  public void placeBarriers(ServerLevel world, ChunkPos lockedChunk, Set<ChunkPos> unlockedChunks) {
    LOGGER.info("placeBarriers called for chunk [{}, {}]", lockedChunk.x, lockedChunk.z);
    if (world == null || lockedChunk == null || unlockedChunks == null) {
      LOGGER.warn("Cannot place barriers: null parameter provided");
      return;
    }

    Set<BlockPos> barriers = new HashSet<>();

    // Check all 4 cardinal directions
    ChunkPos north = new ChunkPos(lockedChunk.x, lockedChunk.z - 1);
    ChunkPos south = new ChunkPos(lockedChunk.x, lockedChunk.z + 1);
    ChunkPos east = new ChunkPos(lockedChunk.x + 1, lockedChunk.z);
    ChunkPos west = new ChunkPos(lockedChunk.x - 1, lockedChunk.z);

    // Only place barriers on sides that border unlocked chunks
    if (unlockedChunks.contains(north)) {
      barriers.addAll(createBarrierWall(world, lockedChunk, Direction.NORTH));
    }
    if (unlockedChunks.contains(south)) {
      barriers.addAll(createBarrierWall(world, lockedChunk, Direction.SOUTH));
    }
    if (unlockedChunks.contains(east)) {
      barriers.addAll(createBarrierWall(world, lockedChunk, Direction.EAST));
    }
    if (unlockedChunks.contains(west)) {
      barriers.addAll(createBarrierWall(world, lockedChunk, Direction.WEST));
    }

    if (!barriers.isEmpty()) {
      chunkBarriers.put(lockedChunk, barriers);
      allBarrierPositions.addAll(barriers);
      LOGGER.debug("Placed {} barriers around chunk [{}, {}]",
          barriers.size(), lockedChunk.x, lockedChunk.z);
    }
  }

  /**
   * Clears ALL barrier blocks from the world and resets internal tracking.
   * Used before global barrier reinitialization to prevent duplicate barriers.
   * 
   * @param world The server world
   */
  public void clearAllBarriers(ServerLevel world) {
    if (world == null) {
      LOGGER.warn("Cannot clear barriers: null world provided");
      return;
    }

    int totalRemoved = 0;

    // Remove all tracked barriers from the world
    for (Map.Entry<ChunkPos, Set<BlockPos>> entry : chunkBarriers.entrySet()) {
      for (BlockPos pos : entry.getValue()) {
        world.setBlockState(pos, Blocks.AIR.defaultBlockState());
        totalRemoved++;
      }
    }

    // Clear internal tracking
    chunkBarriers.clear();
    allBarrierPositions.clear();

    LOGGER.info("Cleared {} barrier blocks from world", totalRemoved);
  }

  /**
   * Removes all barrier blocks for a specific chunk.
   * Called when a chunk is unlocked.
   * 
   * Scans all barrier positions at chunk boundaries (not just tracked ones)
   * to remove barriers that may have been created via block break replacement.
   * 
   * @param world The server world
   * @param chunk The chunk position to remove barriers from
   */
  public void removeBarriers(ServerLevel world, ChunkPos chunk) {
    if (world == null || chunk == null) {
      LOGGER.warn("Cannot remove barriers: null parameter provided");
      return;
    }

    // First, remove tracked barriers
    Set<BlockPos> trackedBarriers = chunkBarriers.remove(chunk);
    int removedFromTracking = 0;
    if (trackedBarriers != null && !trackedBarriers.isEmpty()) {
      for (BlockPos pos : trackedBarriers) {
        world.setBlockState(pos, Blocks.AIR.defaultBlockState());
        allBarrierPositions.remove(pos);
        removedFromTracking++;
      }
    }

    // Second, scan all 4 edges of the chunk for stray barriers that may have
    // been placed via block break replacement and not tracked
    int removedFromScan = removeStrayBarriers(world, chunk);

    LOGGER.debug("Removed {} tracked + {} stray barriers from chunk [{}, {}]",
        removedFromTracking, removedFromScan, chunk.x, chunk.z);
  }

  /**
   * Scans chunk edges for stray barrier blocks and removes them.
   * These barriers may have been created via block break replacement and not
   * tracked.
   * 
   * @param world The server world
   * @param chunk The chunk to scan
   * @return Number of stray barriers removed
   */
  private int removeStrayBarriers(ServerLevel world, ChunkPos chunk) {
    int removed = 0;
    int chunkWorldX = chunk.x * CHUNK_SIZE;
    int chunkWorldZ = chunk.z * CHUNK_SIZE;

    // Scan all 4 edges at full world height
    // North edge (Z = chunkWorldZ)
    for (int x = chunkWorldX; x < chunkWorldX + CHUNK_SIZE; x++) {
      for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
        BlockPos pos = new BlockPos(x, y, chunkWorldZ);
        if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
          world.setBlockState(pos, Blocks.AIR.defaultBlockState());
          allBarrierPositions.remove(pos);
          removed++;
        }
      }
    }

    // South edge (Z = chunkWorldZ + 15)
    for (int x = chunkWorldX; x < chunkWorldX + CHUNK_SIZE; x++) {
      for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
        BlockPos pos = new BlockPos(x, y, chunkWorldZ + CHUNK_SIZE - 1);
        if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
          world.setBlockState(pos, Blocks.AIR.defaultBlockState());
          allBarrierPositions.remove(pos);
          removed++;
        }
      }
    }

    // West edge (X = chunkWorldX)
    for (int z = chunkWorldZ; z < chunkWorldZ + CHUNK_SIZE; z++) {
      for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
        BlockPos pos = new BlockPos(chunkWorldX, y, z);
        if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
          world.setBlockState(pos, Blocks.AIR.defaultBlockState());
          allBarrierPositions.remove(pos);
          removed++;
        }
      }
    }

    // East edge (X = chunkWorldX + 15)
    for (int z = chunkWorldZ; z < chunkWorldZ + CHUNK_SIZE; z++) {
      for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
        BlockPos pos = new BlockPos(chunkWorldX + CHUNK_SIZE - 1, y, z);
        if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
          world.setBlockState(pos, Blocks.AIR.defaultBlockState());
          allBarrierPositions.remove(pos);
          removed++;
        }
      }
    }

    if (removed > 0) {
      LOGGER.debug("Scanned and removed {} stray barriers from chunk edges", removed);
    }

    return removed;
  }

  /**
   * Removes stray barriers at the boundary between two specific chunks.
   * Useful when updating barriers between a newly unlocked chunk and its
   * neighbors.
   * 
   * @param world  The server world
   * @param chunk1 First chunk position
   * @param chunk2 Second chunk position (must be adjacent to chunk1)
   * @return Number of barriers removed
   */
  public int removeStrayBarriersAtBoundary(ServerLevel world, ChunkPos chunk1, ChunkPos chunk2) {
    if (world == null || chunk1 == null || chunk2 == null) {
      return 0;
    }

    int removed = 0;

    // Determine which direction chunk2 is from chunk1
    if (chunk2.x == chunk1.x && chunk2.z == chunk1.z - 1) {
      // chunk2 is north of chunk1
      int x1World = chunk1.x * CHUNK_SIZE;
      int z1World = chunk1.z * CHUNK_SIZE;
      for (int x = x1World; x < x1World + CHUNK_SIZE; x++) {
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
          BlockPos pos = new BlockPos(x, y, z1World);
          if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
            world.setBlockState(pos, Blocks.AIR.defaultBlockState());
            allBarrierPositions.remove(pos);
            removed++;
          }
        }
      }
    } else if (chunk2.x == chunk1.x && chunk2.z == chunk1.z + 1) {
      // chunk2 is south of chunk1
      int x1World = chunk1.x * CHUNK_SIZE;
      int z1World = chunk1.z * CHUNK_SIZE;
      for (int x = x1World; x < x1World + CHUNK_SIZE; x++) {
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
          BlockPos pos = new BlockPos(x, y, z1World + CHUNK_SIZE - 1);
          if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
            world.setBlockState(pos, Blocks.AIR.defaultBlockState());
            allBarrierPositions.remove(pos);
            removed++;
          }
        }
      }
    } else if (chunk2.x == chunk1.x - 1 && chunk2.z == chunk1.z) {
      // chunk2 is west of chunk1
      int x1World = chunk1.x * CHUNK_SIZE;
      int z1World = chunk1.z * CHUNK_SIZE;
      for (int z = z1World; z < z1World + CHUNK_SIZE; z++) {
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
          BlockPos pos = new BlockPos(x1World, y, z);
          if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
            world.setBlockState(pos, Blocks.AIR.defaultBlockState());
            allBarrierPositions.remove(pos);
            removed++;
          }
        }
      }
    } else if (chunk2.x == chunk1.x + 1 && chunk2.z == chunk1.z) {
      // chunk2 is east of chunk1
      int x1World = chunk1.x * CHUNK_SIZE;
      int z1World = chunk1.z * CHUNK_SIZE;
      for (int z = z1World; z < z1World + CHUNK_SIZE; z++) {
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
          BlockPos pos = new BlockPos(x1World + CHUNK_SIZE - 1, y, z);
          if (world.getBlockState(pos).isOf(Chunklocked.BARRIER_BLOCK_V2)) {
            world.setBlockState(pos, Blocks.AIR.defaultBlockState());
            allBarrierPositions.remove(pos);
            removed++;
          }
        }
      }
    }

    if (removed > 0) {
      LOGGER.debug("Removed {} stray barriers at boundary between {} and {}", removed, chunk1, chunk2);
    }

    return removed;
  }

  /**
   * Updates barriers after a chunk unlock.
   * Removes barriers for the unlocked chunk and updates adjacent chunks.
   * 
   * @param world             The server world
   * @param unlockedChunk     The chunk that was just unlocked
   * @param allUnlockedChunks Complete set of all unlocked chunks (including newly
   *                          unlocked)
   * @param allLockedChunks   Complete set of all locked chunks (excluding newly
   *                          unlocked)
   */
  public void updateBarriers(ServerLevel world, ChunkPos unlockedChunk,
      Set<ChunkPos> allUnlockedChunks, Set<ChunkPos> allLockedChunks) {
    if (world == null || unlockedChunk == null) {
      LOGGER.warn("Cannot update barriers: null parameter provided");
      return;
    }

    // Remove barriers from the newly unlocked chunk
    removeBarriers(world, unlockedChunk);

    // Update barriers for adjacent locked chunks
    ChunkPos[] adjacentChunks = {
        new ChunkPos(unlockedChunk.x, unlockedChunk.z - 1), // North
        new ChunkPos(unlockedChunk.x, unlockedChunk.z + 1), // South
        new ChunkPos(unlockedChunk.x + 1, unlockedChunk.z), // East
        new ChunkPos(unlockedChunk.x - 1, unlockedChunk.z) // West
    };

    for (ChunkPos adjacent : adjacentChunks) {
      if (allLockedChunks != null && allLockedChunks.contains(adjacent)) {
        // Remove old barriers for this locked chunk
        removeBarriers(world, adjacent);
        // Recreate barriers with updated unlock state
        placeBarriers(world, adjacent, allUnlockedChunks);
      }
    }

    LOGGER.debug("Updated barriers after unlocking chunk [{}, {}]",
        unlockedChunk.x, unlockedChunk.z);
  }

  /**
   * Determines if a block position should have a barrier based on chunk
   * boundaries.
   * A position should have a barrier if it's at the edge of a chunk boundary
   * where one side is unlocked and the other side is locked (using GLOBAL unlock
   * status).
   * 
   * @param world      The server world
   * @param pos        The block position to check
   * @param playerUuid The UUID of the player (unused, kept for API compatibility)
   * @param wasBarrier Whether the broken block was a barrier block
   * @return true if this position should have a barrier block
   */
  public boolean shouldHaveBarrier(ServerLevel world, BlockPos pos, java.util.UUID playerUuid, boolean wasBarrier) {
    if (world == null || pos == null) {
      return false;
    }

    // Get the chunk manager to check lock status
    var chunkManager = chunkloaded.Chunklocked.getChunkManager();
    if (chunkManager == null) {
      return false;
    }

    // Determine which chunk this position is in
    ChunkPos currentChunk = new ChunkPos(pos);
    int localX = pos.getX() & 15; // Position within chunk (0-15)
    int localZ = pos.getZ() & 15;

    LOGGER.info("Checking shouldHaveBarrier at {} (chunk={}, local={},{}) wasBarrier={}",
        pos, currentChunk, localX, localZ, wasBarrier);

    // Check if this position is at a chunk boundary (edge)
    boolean atNorthEdge = (localZ == 0);
    boolean atSouthEdge = (localZ == 15);
    boolean atWestEdge = (localX == 0);
    boolean atEastEdge = (localX == 15);

    if (!atNorthEdge && !atSouthEdge && !atWestEdge && !atEastEdge) {
      LOGGER.info("Not at chunk boundary (localX={}, localZ={})", localX, localZ);
      return false; // Not at a boundary
    }

    // Get GLOBAL unlocked chunks (not per-player)
    Set<ChunkPos> globalUnlockedChunks = chunkManager.getGlobalUnlockedChunks();
    boolean currentUnlocked = globalUnlockedChunks.contains(currentChunk);

    LOGGER.info("Current chunk {} is {} (global state)", currentChunk, currentUnlocked ? "unlocked" : "locked");

    // CRITICAL: We need to handle two cases:
    // 1. If a BARRIER block was broken at a boundary, ALWAYS replace it (prevents
    // breaking through)
    // 2. If TERRAIN was broken, only replace in LOCKED chunks (allows mining in
    // unlocked chunks)
    //
    // This prevents the exploit where players break barrier blocks from the
    // unlocked side
    // while still allowing free mining of terrain in unlocked areas.

    if (wasBarrier) {
      LOGGER.info("A barrier block was broken - checking if this is a locked/unlocked boundary");
      // For barrier blocks, we need to check if there's a boundary regardless of
      // which side
    } else if (currentUnlocked) {
      // For terrain blocks in unlocked chunks, player can mine freely
      LOGGER.info("Current chunk is unlocked and block was not a barrier - player can mine terrain freely");
      return false;
    }

    // If we get here, either:
    // - A barrier was broken at a boundary (needs replacement)
    // - Terrain was broken in a locked chunk (needs barrier to prevent escape)
    if (atNorthEdge) {
      ChunkPos north = new ChunkPos(currentChunk.x, currentChunk.z - 1);
      boolean northUnlocked = globalUnlockedChunks.contains(north);
      LOGGER.info("North edge: chunk {} is {} (global state)", north, northUnlocked ? "unlocked" : "locked");

      if (wasBarrier) {
        // Barrier blocks should exist at any locked/unlocked boundary
        if (currentUnlocked != northUnlocked) {
          LOGGER.info("Barrier needed at north boundary (barrier broken at locked/unlocked edge)");
          return true;
        }
      } else {
        // Terrain: only block if in locked chunk next to unlocked
        if (northUnlocked) {
          LOGGER.info("Barrier needed at north boundary (terrain in locked chunk adjacent to unlocked)");
          return true;
        }
      }
    }
    if (atSouthEdge) {
      ChunkPos south = new ChunkPos(currentChunk.x, currentChunk.z + 1);
      boolean southUnlocked = globalUnlockedChunks.contains(south);
      LOGGER.info("South edge: chunk {} is {} (global state)", south, southUnlocked ? "unlocked" : "locked");

      if (wasBarrier) {
        // Barrier blocks should exist at any locked/unlocked boundary
        if (currentUnlocked != southUnlocked) {
          LOGGER.info("Barrier needed at south boundary (barrier broken at locked/unlocked edge)");
          return true;
        }
      } else {
        // Terrain: only block if in locked chunk next to unlocked
        if (southUnlocked) {
          LOGGER.info("Barrier needed at south boundary (terrain in locked chunk adjacent to unlocked)");
          return true;
        }
      }
    }
    if (atWestEdge) {
      ChunkPos west = new ChunkPos(currentChunk.x - 1, currentChunk.z);
      boolean westUnlocked = globalUnlockedChunks.contains(west);
      LOGGER.info("West edge: chunk {} is {} (global state)", west, westUnlocked ? "unlocked" : "locked");

      if (wasBarrier) {
        // Barrier blocks should exist at any locked/unlocked boundary
        if (currentUnlocked != westUnlocked) {
          LOGGER.info("Barrier needed at west boundary (barrier broken at locked/unlocked edge)");
          return true;
        }
      } else {
        // Terrain: only block if in locked chunk next to unlocked
        if (westUnlocked) {
          LOGGER.info("Barrier needed at west boundary (terrain in locked chunk adjacent to unlocked)");
          return true;
        }
      }
    }
    if (atEastEdge) {
      ChunkPos east = new ChunkPos(currentChunk.x + 1, currentChunk.z);
      boolean eastUnlocked = globalUnlockedChunks.contains(east);
      LOGGER.info("East edge: chunk {} is {} (global state)", east, eastUnlocked ? "unlocked" : "locked");

      if (wasBarrier) {
        // Barrier blocks should exist at any locked/unlocked boundary
        if (currentUnlocked != eastUnlocked) {
          LOGGER.info("Barrier needed at east boundary (barrier broken at locked/unlocked edge)");
          return true;
        }
      } else {
        // Terrain: only block if in locked chunk next to unlocked
        if (eastUnlocked) {
          LOGGER.info("Barrier needed at east boundary (terrain in locked chunk adjacent to unlocked)");
          return true;
        }
      }
    }

    LOGGER.info("No barrier needed at this position");
    return false;
  }

  /**
   * Gets the total number of barrier blocks currently placed.
   * Useful for debugging and statistics.
   * 
   * @return Total count of active barrier blocks
   */
  public int getBarrierCount() {
    return allBarrierPositions.size();
  }

  /**
   * Clears all barrier data without removing blocks from the world.
   * Used for cleanup or reinitialization scenarios.
   */
  public void clearBarrierTracking() {
    chunkBarriers.clear();
    allBarrierPositions.clear();
    LOGGER.debug("Cleared all barrier tracking data");
  }

  /**
   * Creates a barrier wall on one side of a chunk.
   * 
   * @param world     The server world
   * @param chunk     The chunk to create a wall for
   * @param direction Which side of the chunk to place the wall
   * @return Set of BlockPos where barriers were placed
   */
  private Set<BlockPos> createBarrierWall(ServerLevel world, ChunkPos chunk, Direction direction) {
    Set<BlockPos> barriers = new HashSet<>();

    // Calculate world coordinates for this chunk
    int chunkWorldX = chunk.x * CHUNK_SIZE;
    int chunkWorldZ = chunk.z * CHUNK_SIZE;

    int wallX = chunkWorldX; // Default value
    int wallZ = chunkWorldZ; // Default value
    int parallelStart, parallelEnd;
    boolean isXAxis;

    switch (direction) {
      case NORTH -> {
        // North wall: Z = chunkWorldZ (first block of locked chunk, at north edge)
        wallZ = chunkWorldZ;
        parallelStart = chunkWorldX;
        parallelEnd = chunkWorldX + CHUNK_SIZE;
        isXAxis = true;
      }
      case SOUTH -> {
        // South wall: Z = chunkWorldZ + 15 (last block of locked chunk, at south edge)
        wallZ = chunkWorldZ + CHUNK_SIZE - 1;
        parallelStart = chunkWorldX;
        parallelEnd = chunkWorldX + CHUNK_SIZE;
        isXAxis = true;
      }
      case EAST -> {
        // East wall: X = chunkWorldX + 15 (last block of locked chunk, at east edge)
        wallX = chunkWorldX + CHUNK_SIZE - 1;
        parallelStart = chunkWorldZ;
        parallelEnd = chunkWorldZ + CHUNK_SIZE;
        isXAxis = false;
      }
      case WEST -> {
        // West wall: X = chunkWorldX (first block of locked chunk, at west edge)
        wallX = chunkWorldX;
        parallelStart = chunkWorldZ;
        parallelEnd = chunkWorldZ + CHUNK_SIZE;
        isXAxis = false;
      }
      default -> throw new IllegalArgumentException("Invalid direction: " + direction);
    }

    // Place barrier blocks from MIN_WORLD_Y to MAX_WORLD_Y
    for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
      for (int parallel = parallelStart; parallel < parallelEnd; parallel++) {
        BlockPos pos;
        if (isXAxis) {
          // North or South wall (vary X)
          pos = new BlockPos(parallel, y, wallZ);
        } else {
          // East or West wall (vary Z)
          pos = new BlockPos(wallX, y, parallel);
        }

        // Only place barriers in air or replaceable blocks (not solid terrain)
        BlockState existingState = world.getBlockState(pos);
        if (existingState.isAir() || existingState.isReplaceable()) {
          world.setBlockState(pos, Chunklocked.BARRIER_BLOCK_V2.defaultBlockState());
          barriers.add(pos);
        }
        // If there's already a solid block, it acts as a natural barrier - skip it
      }
    }

    return barriers;
  }

  /**
   * Cardinal directions for barrier wall placement.
   */
  private enum Direction {
    NORTH, SOUTH, EAST, WEST
  }
}
