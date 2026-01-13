package chunkloaded.border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import net.minecraft.class_1923;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3218;

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
    public final List<class_2338> positions;
    public final class_3218 world;
    public final long createdTime;

    public BarrierBatch(class_3218 world) {
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
    public boolean addPosition(class_2338 pos) {
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
    public Set<class_1923> getAffectedChunks() {
      Set<class_1923> chunks = new HashSet<>();
      for (class_2338 pos : positions) {
        chunks.add(new class_1923(pos));
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

    class_3218 world = batch.world;
    long startTime = System.currentTimeMillis();

    // Place all blocks in the batch
    for (class_2338 pos : batch.positions) {
      class_2680 existingState = world.method_8320(pos);
      if (existingState.method_26215() || existingState.method_45474()) {
        world.method_8652(pos, class_2246.field_10499.method_9564(), 2);
      }
    }

    // setBlockState handles chunk marking automatically
    Set<class_1923> affectedChunks = batch.getAffectedChunks();

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
  public static void removeBatch(List<class_2338> positions, class_3218 world) {
    if (positions == null || positions.isEmpty() || world == null) {
      return;
    }

    long startTime = System.currentTimeMillis();
    Set<class_1923> affectedChunks = new HashSet<>();

    // Remove all barriers
    for (class_2338 pos : positions) {
      class_2680 state = world.method_8320(pos);
      if (state.method_26204() == class_2246.field_10499) {
        world.method_8652(pos, class_2246.field_10124.method_9564(), 2);
        affectedChunks.add(new class_1923(pos));
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
  public static BarrierBatch createBatch(class_3218 world) {
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
  public static List<BarrierBatch> splitIntoBatches(class_3218 world, List<class_2338> positions) {
    List<BarrierBatch> batches = new ArrayList<>();
    BarrierBatch currentBatch = createBatch(world);

    for (class_2338 pos : positions) {
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
