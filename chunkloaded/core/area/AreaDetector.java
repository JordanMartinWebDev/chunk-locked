package chunkloaded.core.area;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import net.minecraft.class_1923;

/**
 * Detects connected components (playable areas) in a set of unlocked chunks.
 * <p>
 * Uses a breadth-first search (BFS) algorithm to identify contiguous regions
 * of chunks. The detector maintains a cache of detected areas that is
 * invalidated
 * when the underlying chunk set is modified.
 * <p>
 * This class is thread-safe for read operations but not for concurrent
 * modifications.
 * External synchronization is required if used from multiple threads.
 */
public class AreaDetector {
  private final Map<class_1923, Integer> chunkCache;
  private List<PlayableArea> cachedAreas;
  private boolean cacheValid = false;
  private int nextAreaId = 0;

  /**
   * Creates a new AreaDetector instance.
   */
  public AreaDetector() {
    this.chunkCache = new HashMap<>();
    this.cachedAreas = new ArrayList<>();
  }

  /**
   * Detects all playable areas in the given set of unlocked chunks.
   * <p>
   * Results are cached until {@link #invalidateCache()} is called.
   *
   * @param unlockedChunks the set of unlocked chunks to analyze
   * @return a list of detected playable areas
   * @throws NullPointerException if unlockedChunks is null or contains null
   *                              elements
   */
  public List<PlayableArea> detectAreas(@NotNull Set<class_1923> unlockedChunks) {
    Objects.requireNonNull(unlockedChunks, "unlockedChunks cannot be null");

    if (unlockedChunks.isEmpty()) {
      cachedAreas = new ArrayList<>();
      cacheValid = true;
      return cachedAreas;
    }

    // Check if we need to recompute
    if (cacheValid && chunkCache.size() == unlockedChunks.size()) {
      boolean cacheHitsAll = unlockedChunks.stream().allMatch(chunkCache::containsKey);
      if (cacheHitsAll) {
        return new ArrayList<>(cachedAreas);
      }
    }

    // Recompute areas
    List<PlayableArea> areas = new ArrayList<>();
    Set<class_1923> visited = new HashSet<>();
    nextAreaId = 0;

    for (class_1923 chunk : unlockedChunks) {
      if (!visited.contains(chunk)) {
        PlayableArea area = bfs(chunk, unlockedChunks, visited);
        areas.add(area);
        nextAreaId++;
      }
    }

    // Update cache
    this.cachedAreas = areas;
    this.cacheValid = true;
    this.chunkCache.clear();
    for (class_1923 chunk : unlockedChunks) {
      this.chunkCache.put(chunk, nextAreaId);
    }

    return new ArrayList<>(areas);
  }

  /**
   * Performs a breadth-first search from the starting chunk to find all connected
   * chunks.
   *
   * @param start     the starting chunk position
   * @param allChunks the set of all unlocked chunks
   * @param visited   the set of already visited chunks (modified in-place)
   * @return a PlayableArea containing all connected chunks
   */
  private PlayableArea bfs(@NotNull class_1923 start, @NotNull Set<class_1923> allChunks,
      @NotNull Set<class_1923> visited) {
    Set<class_1923> areaChunks = new HashSet<>();
    Queue<class_1923> queue = new LinkedList<>();

    queue.add(start);
    visited.add(start);
    areaChunks.add(start);

    while (!queue.isEmpty()) {
      class_1923 current = queue.poll();

      // Check all 4 adjacent chunks (up, down, left, right)
      class_1923[] adjacent = {
          new class_1923(current.field_9181 + 1, current.field_9180),
          new class_1923(current.field_9181 - 1, current.field_9180),
          new class_1923(current.field_9181, current.field_9180 + 1),
          new class_1923(current.field_9181, current.field_9180 - 1)
      };

      for (class_1923 neighbor : adjacent) {
        if (allChunks.contains(neighbor) && !visited.contains(neighbor)) {
          visited.add(neighbor);
          areaChunks.add(neighbor);
          queue.add(neighbor);
        }
      }
    }

    return new PlayableArea(nextAreaId, areaChunks);
  }

  /**
   * Finds the playable area that contains the given chunk.
   *
   * @param chunk the chunk position to search for
   * @return the playable area containing the chunk, or empty Optional if not
   *         found
   * @throws NullPointerException if chunk is null
   */
  public Optional<PlayableArea> findAreaContainingChunk(@NotNull class_1923 chunk) {
    Objects.requireNonNull(chunk, "chunk cannot be null");

    for (PlayableArea area : cachedAreas) {
      if (area.contains(chunk)) {
        return Optional.of(area);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds all playable areas that are adjacent to the given chunk.
   *
   * @param chunk the chunk position to check adjacency for
   * @return a list of areas adjacent to the chunk
   * @throws NullPointerException if chunk is null
   */
  public List<PlayableArea> findAdjacentAreas(@NotNull class_1923 chunk) {
    Objects.requireNonNull(chunk, "chunk cannot be null");

    List<PlayableArea> adjacent = new ArrayList<>();
    for (PlayableArea area : cachedAreas) {
      if (area.isAdjacentTo(chunk)) {
        adjacent.add(area);
      }
    }
    return adjacent;
  }

  /**
   * Invalidates the current cache, forcing recomputation on the next call
   * to {@link #detectAreas(Set)}.
   */
  public void invalidateCache() {
    cacheValid = false;
    cachedAreas.clear();
    chunkCache.clear();
    nextAreaId = 0;
  }

  /**
   * Checks if the cache is currently valid.
   *
   * @return true if the cache contains valid data, false otherwise
   */
  public boolean isCacheValid() {
    return cacheValid;
  }

  /**
   * Gets the number of areas in the current cache.
   *
   * @return the number of cached areas, or 0 if cache is invalid
   */
  public int getCachedAreaCount() {
    return cachedAreas.size();
  }

  /**
   * Gets the cached areas list.
   * <p>
   * Returns a copy to prevent external modification.
   *
   * @return a copy of the cached areas list
   */
  public List<PlayableArea> getCachedAreas() {
    return new ArrayList<>(cachedAreas);
  }
}
