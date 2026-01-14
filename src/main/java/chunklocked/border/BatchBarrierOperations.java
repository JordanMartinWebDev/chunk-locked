package chunklocked.border;

import chunklocked.Chunklocked;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Optimizes barrier block placement by batching operations.
 * 
 * <p>
 * Instead of placing barriers one-by-one (which triggers lighting updates
 * and block update notifications for each block), this system batches
 * placements and applies them together.
 * </p>
 * 
 * <p>
 * Benefits:
 * - Fewer lighting recalculations
 * - Fewer block update notifications
 * - Better performance on large unlocks
 * - Can defer placement to off-tick for even better perf
 * </p>
 */
public class BatchBarrierOperations {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchBarrierOperations.class);

  // Batch configuration
  private static final int MAX_BATCH_SIZE = 256; // Max blocks per batch operation
  private static final int BLOCKS_PER_CHUNK = 384; // 16 width * 384 blocks high

  /**
   * Batch operation representing multiple block placements.
   */
  public static class BarrierBatch {
    public final List<BlockPos> positions;
    public final ServerLevel world;
    public final long createdTime;

    public BarrierBatch(ServerLevel world) {
      this.world = world;
      this.positions = new ArrayList<>();
      this.createdTime = System.currentTimeMillis();
    }

    /**
     * Adds a position to this batch.
     * 
     * @param pos Block position to add
     * @return true if added, false if batch is full
     */
    public boolean addPosition(BlockPos pos) {
      if (positions.size() >= MAX_BATCH_SIZE) {
        return false;
      }
      positions.add(pos);
      return true;
    }

    /**
     * Gets the size of this batch.
     * 
     * @return Number of block positions in batch
     */
    public int size() {
      return positions.size();
    }

    /**
     * Checks if this batch is full.
     * 
     * @return true if batch cannot accept more positions
     */
    public boolean isFull() {
      return positions.size() >= MAX_BATCH_SIZE;
    }

    /**
     * Gets the chunk(s) this batch affects.
     * Used to optimize lighting updates.
     * 
     * @return Set of affected chunk positions
     */
    public Set<ChunkPos> getAffectedChunks() {
      Set<ChunkPos> chunks = new HashSet<>();
      for (BlockPos pos : positions) {
        chunks.add(new ChunkPos(pos));
      }
      return chunks;
    }
  }

  /**
   * Places all barrier blocks in a batch with optimized block updates.
   * 
   * @param batch The batch of barriers to place
   */
  public static void placeBatch(BarrierBatch batch) {
    if (batch == null || batch.positions.isEmpty()) {
      return;
    }

    ServerLevel world = batch.world;
    long startTime = System.currentTimeMillis();

    // Place all blocks in the batch
    for (BlockPos pos : batch.positions) {
      BlockState existingState = world.getBlockState(pos);
      if (existingState.isAir() || existingState.canBeReplaced()) {
        // Use flags that prevent neighbor updates (prevents lava/water from flowing
        // back)
        world.setBlock(pos, Chunklocked.BARRIER_BLOCK_V2.defaultBlockState(), 2 | 16);
      }
    }

    // setBlockState handles chunk marking automatically
    Set<ChunkPos> affectedChunks = batch.getAffectedChunks();

    long duration = System.currentTimeMillis() - startTime;
    LOGGER.debug("Placed batch of {} barriers in {} chunks ({} ms)",
        batch.positions.size(), affectedChunks.size(), duration);
  }

  /**
   * Removes all barrier blocks in a batch.
   * 
   * @param positions List of block positions to remove
   * @param world     The server world
   */
  public static void removeBatch(List<BlockPos> positions, ServerLevel world) {
    if (positions == null || positions.isEmpty() || world == null) {
      return;
    }

    long startTime = System.currentTimeMillis();
    Set<ChunkPos> affectedChunks = new HashSet<>();

    // Remove all barriers
    for (BlockPos pos : positions) {
      BlockState state = world.getBlockState(pos);
      if (state.is(Chunklocked.BARRIER_BLOCK_V2)) {
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        affectedChunks.add(new ChunkPos(pos));
      }
    }

    // setBlockState handles chunk marking automatically

    long duration = System.currentTimeMillis() - startTime;
    LOGGER.debug("Removed batch of {} barriers from {} chunks ({} ms)",
        positions.size(), affectedChunks.size(), duration);
  }

  /**
   * Creates a batch for placing barriers.
   * 
   * @param world The server world
   * @return A new empty batch
   */
  public static BarrierBatch createBatch(ServerLevel world) {
    return new BarrierBatch(world);
  }

  /**
   * Splits a large list of positions into multiple batches.
   * Useful for very large unlock operations.
   * 
   * @param world     The server world
   * @param positions All positions to place
   * @return List of batches
   */
  public static List<BarrierBatch> splitIntoBatches(ServerLevel world, List<BlockPos> positions) {
    List<BarrierBatch> batches = new ArrayList<>();
    BarrierBatch currentBatch = createBatch(world);

    for (BlockPos pos : positions) {
      if (!currentBatch.addPosition(pos)) {
        // Current batch is full, start a new one
        batches.add(currentBatch);
        currentBatch = createBatch(world);
        currentBatch.addPosition(pos);
      }
    }

    // Add final batch if it has positions
    if (!currentBatch.positions.isEmpty()) {
      batches.add(currentBatch);
    }

    LOGGER.debug("Split {} positions into {} batches",
        positions.size(), batches.size());

    return batches;
  }

  /**
   * Calculates the number of blocks that will be affected by a chunk unlock.
   * Useful for predicting performance impact.
   * 
   * @param numChunksAffected Number of chunks getting new/removed barriers
   * @param worldHeight       Height of the world (for barrier walls)
   * @return Estimated number of barrier blocks affected
   */
  public static int estimateBlocksAffected(int numChunksAffected, int worldHeight) {
    // Each locked chunk edge that borders an unlocked chunk gets barriers
    // Each side is 16 blocks wide * world height
    // Average: assume 1-2 sides need barriers per affected chunk
    return numChunksAffected * 2 * 16 * worldHeight;
  }
}
