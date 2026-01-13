package chunkloaded.core.area;

import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a contiguous region of unlocked chunks that form a playable area.
 * <p>
 * A playable area is a connected component of unlocked chunks, where chunks are
 * considered connected if they are adjacent (sharing an edge in 2D space).
 * <p>
 * This class is immutable after construction.
 */
public class PlayableArea {
  private final int areaId;
  private final Set<ChunkPos> chunks;
  private final ChunkPos centerChunk;

  /**
   * Creates a new PlayableArea with the given ID and chunks.
   *
   * @param areaId the unique identifier for this area
   * @param chunks the set of ChunkPos that belong to this area (must not be
   *               empty)
   * @throws IllegalArgumentException if chunks is null or empty
   * @throws NullPointerException     if any chunk in the set is null
   */
  public PlayableArea(int areaId, @NotNull Set<ChunkPos> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      throw new IllegalArgumentException("PlayableArea must contain at least one chunk");
    }

    // Validate no null chunks
    if (chunks.stream().anyMatch(Objects::isNull)) {
      throw new NullPointerException("Chunk set contains null entries");
    }

    this.areaId = areaId;
    this.chunks = Collections.unmodifiableSet(new HashSet<>(chunks));
    this.centerChunk = calculateCenter();
  }

  /**
   * Calculates the center chunk of this area (approximate centroid).
   * <p>
   * The center is calculated as the average X and Z coordinates of all chunks,
   * rounded to the nearest chunk position.
   *
   * @return the approximate center chunk
   */
  private ChunkPos calculateCenter() {
    long sumX = 0;
    long sumZ = 0;

    for (ChunkPos pos : chunks) {
      sumX += pos.x;
      sumZ += pos.z;
    }

    int centerX = (int) (sumX / chunks.size());
    int centerZ = (int) (sumZ / chunks.size());

    return new ChunkPos(centerX, centerZ);
  }

  /**
   * Gets the unique identifier for this area.
   *
   * @return the area ID
   */
  public int getAreaId() {
    return areaId;
  }

  /**
   * Gets an immutable view of all chunks in this area.
   *
   * @return an immutable set of chunks
   */
  public Set<ChunkPos> getChunks() {
    return chunks;
  }

  /**
   * Gets the number of chunks in this area.
   *
   * @return the chunk count
   */
  public int getChunkCount() {
    return chunks.size();
  }

  /**
   * Gets the approximate center chunk of this area.
   *
   * @return the center chunk position
   */
  public ChunkPos getCenter() {
    return centerChunk;
  }

  /**
   * Checks if a specific chunk is contained in this area.
   *
   * @param pos the chunk position to check
   * @return true if the chunk is in this area, false otherwise
   */
  public boolean contains(@NotNull ChunkPos pos) {
    Objects.requireNonNull(pos, "ChunkPos cannot be null");
    return chunks.contains(pos);
  }

  /**
   * Checks if a chunk position is adjacent to this area.
   * <p>
   * A chunk is considered adjacent if it shares an edge (not diagonal) with
   * any chunk in this area.
   *
   * @param pos the chunk position to check
   * @return true if the chunk is adjacent to any chunk in this area
   */
  public boolean isAdjacentTo(@NotNull ChunkPos pos) {
    Objects.requireNonNull(pos, "ChunkPos cannot be null");

    for (ChunkPos chunk : chunks) {
      if (isAdjacent(chunk, pos)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if two chunk positions are adjacent (share an edge).
   *
   * @param pos1 the first chunk position
   * @param pos2 the second chunk position
   * @return true if they are adjacent, false otherwise
   */
  private boolean isAdjacent(@NotNull ChunkPos pos1, @NotNull ChunkPos pos2) {
    int dx = Math.abs(pos1.x - pos2.x);
    int dz = Math.abs(pos1.z - pos2.z);

    // Adjacent means: (dx == 1 && dz == 0) || (dx == 0 && dz == 1)
    return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
  }

  /**
   * Returns a string representation of this area.
   *
   * @return a string containing the area ID, chunk count, and center
   */
  @Override
  public String toString() {
    return "PlayableArea{" +
        "id=" + areaId +
        ", chunks=" + chunks.size() +
        ", center=" + centerChunk +
        '}';
  }

  /**
   * Checks equality with another object.
   * <p>
   * Two areas are equal if they have the same ID and contain the same chunks.
   *
   * @param obj the object to compare
   * @return true if equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof PlayableArea))
      return false;
    PlayableArea other = (PlayableArea) obj;
    return areaId == other.areaId && chunks.equals(other.chunks);
  }

  /**
   * Returns the hash code for this area.
   *
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(areaId, chunks);
  }
}
