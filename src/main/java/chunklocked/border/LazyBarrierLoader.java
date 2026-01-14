package chunklocked.border;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
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
  private final Set<ChunkPos> barriersAwaitingChunks = ConcurrentHashMap.newKeySet();

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
  private void onChunkLoad(ServerLevel world, LevelChunk chunk) {
    if (world == null || chunk == null) {
      return;
    }

    // Only process in Overworld
    if (!world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
      return;
    }

    ChunkPos loadedChunk = chunk.getPos();

    // Check all 4 adjacent chunks
    ChunkPos[] adjacentPositions = {
        new ChunkPos(loadedChunk.x, loadedChunk.z - 1), // North
        new ChunkPos(loadedChunk.x, loadedChunk.z + 1), // South
        new ChunkPos(loadedChunk.x + 1, loadedChunk.z), // East
        new ChunkPos(loadedChunk.x - 1, loadedChunk.z) // West
    };

    for (ChunkPos adjacent : adjacentPositions) {
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
  public void markChunkAwaitingLoad(ChunkPos chunkPos) {
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
  public boolean isAwaitingLoad(ChunkPos chunkPos) {
    return barriersAwaitingChunks.contains(chunkPos);
  }
}
