package chunkloaded.border;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.class_1923;
import net.minecraft.class_2818;
import net.minecraft.class_3218;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lazy loading of barriers when chunks are loaded.
 * 
 * <p>
 * Instead of placing all barriers immediately (which might cause lag),
 * this system defers barrier placement until the target chunk is loaded.
 * When a chunk loads, we check if any adjacent locked chunks need barriers,
 * and place them then.
 * </p>
 * 
 * <p>
 * Reduces server startup time and prevents placement issues with unloaded
 * chunks.
 * </p>
 */
public class LazyBarrierLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(LazyBarrierLoader.class);

  private final ChunkBarrierManager barrierManager;
  private final Set<class_1923> barriersAwaitingChunks = ConcurrentHashMap.newKeySet();

  public LazyBarrierLoader(ChunkBarrierManager barrierManager) {
    this.barrierManager = barrierManager;
  }

  /**
   * Registers the chunk load event listener.
   * Call this once during mod initialization.
   */
  public void registerChunkLoadListener() {
    ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
      onChunkLoad(world, chunk);
    });

    LOGGER.info("Registered lazy barrier loader for chunk load events");
  }

  /**
   * Called when a chunk loads. Checks if any adjacent chunks need barriers
   * now that this chunk is loaded.
   * 
   * @param world The server world
   * @param chunk The chunk that was loaded
   */
  private void onChunkLoad(class_3218 world, class_2818 chunk) {
    if (world == null || chunk == null) {
      return;
    }

    // Only process in Overworld
    if (!world.method_27983().equals(net.minecraft.class_1937.field_25179)) {
      return;
    }

    class_1923 loadedChunk = chunk.method_12004();

    // Check all 4 adjacent chunks
    class_1923[] adjacentPositions = {
        new class_1923(loadedChunk.field_9181, loadedChunk.field_9180 - 1), // North
        new class_1923(loadedChunk.field_9181, loadedChunk.field_9180 + 1), // South
        new class_1923(loadedChunk.field_9181 + 1, loadedChunk.field_9180), // East
        new class_1923(loadedChunk.field_9181 - 1, loadedChunk.field_9180) // West
    };

    for (class_1923 adjacent : adjacentPositions) {
      if (barriersAwaitingChunks.contains(adjacent)) {
        LOGGER.info("Adjacent locked chunk {} is now loaded, placing pending barriers", adjacent);
        barriersAwaitingChunks.remove(adjacent);
        // Barriers will be placed on next barrier update from ChunkManager
      }
    }

    LOGGER.debug("Processed chunk load for {}", loadedChunk);
  }

  /**
   * Records that a chunk is waiting for its adjacent chunks to load.
   * Used when deferring barrier placement.
   * 
   * @param chunkPos The locked chunk awaiting adjacent chunk loads
   */
  public void markChunkAwaitingLoad(class_1923 chunkPos) {
    barriersAwaitingChunks.add(chunkPos);
    LOGGER.debug("Marked chunk {} as awaiting adjacent chunk loads", chunkPos);
  }

  /**
   * Gets the count of chunks awaiting chunk load before barriers are placed.
   * Useful for debugging and statistics.
   * 
   * @return Count of deferred barrier placements
   */
  public int getPendingBarrierCount() {
    return barriersAwaitingChunks.size();
  }

  /**
   * Checks if a chunk is awaiting load for barriers.
   * 
   * @param chunkPos The chunk position to check
   * @return true if this chunk is awaiting adjacent chunks to load
   */
  public boolean isAwaitingLoad(class_1923 chunkPos) {
    return barriersAwaitingChunks.contains(chunkPos);
  }
}
