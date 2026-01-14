package chunklocked.core.area;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PlayableArea class.
 */
public class PlayableAreaTest {
    private Set<ChunkPos> singleChunk;
    private Set<ChunkPos> adjacentChunks;
    private Set<ChunkPos> disconnectedChunks;

    @BeforeEach
    void setUp() {
        singleChunk = new HashSet<>();
        singleChunk.add(new ChunkPos(0, 0));

        adjacentChunks = new HashSet<>();
        adjacentChunks.add(new ChunkPos(0, 0));
        adjacentChunks.add(new ChunkPos(1, 0));
        adjacentChunks.add(new ChunkPos(0, 1));
        adjacentChunks.add(new ChunkPos(1, 1));

        disconnectedChunks = new HashSet<>();
        disconnectedChunks.add(new ChunkPos(0, 0));
        disconnectedChunks.add(new ChunkPos(5, 5));
    }

    @Test
    void testPlayableAreaCreation_SingleChunk_Success() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertEquals(1, area.getAreaId());
        assertEquals(1, area.getChunkCount());
        assertTrue(area.getChunks().contains(new ChunkPos(0, 0)));
    }

    @Test
    void testPlayableAreaCreation_MultipleChunks_Success() {
        PlayableArea area = new PlayableArea(1, adjacentChunks);

        assertEquals(1, area.getAreaId());
        assertEquals(4, area.getChunkCount());
        assertTrue(area.getChunks().contains(new ChunkPos(0, 0)));
        assertTrue(area.getChunks().contains(new ChunkPos(1, 0)));
    }

    @Test
    void testPlayableAreaCreation_NullChunks_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new PlayableArea(1, null));
    }

    @Test
    void testPlayableAreaCreation_EmptyChunks_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new PlayableArea(1, new HashSet<>()));
    }

    @Test
    void testPlayableAreaCreation_ContainsNullChunk_ThrowsException() {
        Set<ChunkPos> chunksWithNull = new HashSet<>(singleChunk);
        chunksWithNull.add(null);

        assertThrows(NullPointerException.class, () -> new PlayableArea(1, chunksWithNull));
    }

    @Test
    void testGetCenter_SingleChunk_ReturnsChunk() {
        PlayableArea area = new PlayableArea(1, singleChunk);
        ChunkPos center = area.getCenter();

        assertEquals(0, center.x);
        assertEquals(0, center.z);
    }

    @Test
    void testGetCenter_2x2Grid_ReturnsCentroid() {
        PlayableArea area = new PlayableArea(1, adjacentChunks);
        ChunkPos center = area.getCenter();

        // Average of (0,0), (1,0), (0,1), (1,1) = (0.5, 0.5) rounded
        assertEquals(0, center.x);
        assertEquals(0, center.z);
    }

    @Test
    void testGetCenter_AsymmetricShape_ReturnsCentroid() {
        Set<ChunkPos> chunks = new HashSet<>();
        chunks.add(new ChunkPos(0, 0));
        chunks.add(new ChunkPos(10, 0));
        chunks.add(new ChunkPos(0, 10));

        PlayableArea area = new PlayableArea(1, chunks);
        ChunkPos center = area.getCenter();

        // Average of (0,0), (10,0), (0,10) = (3.33, 3.33) rounded to (3, 3)
        assertEquals(3, center.x);
        assertEquals(3, center.z);
    }

    @Test
    void testGetCenter_NegativeCoordinates_ReturnsCentroid() {
        Set<ChunkPos> chunks = new HashSet<>();
        chunks.add(new ChunkPos(-5, -5));
        chunks.add(new ChunkPos(-3, -5));
        chunks.add(new ChunkPos(-5, -3));

        PlayableArea area = new PlayableArea(1, chunks);
        ChunkPos center = area.getCenter();

        // Average of (-5,-5), (-3,-5), (-5,-3) = (-4.33, -4.33) rounded
        assertEquals(-4, center.x);
        assertEquals(-4, center.z);
    }

    @Test
    void testContains_ChunkExists_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, adjacentChunks);

        assertTrue(area.contains(new ChunkPos(0, 0)));
        assertTrue(area.contains(new ChunkPos(1, 1)));
    }

    @Test
    void testContains_ChunkNotExists_ReturnsFalse() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertFalse(area.contains(new ChunkPos(1, 1)));
        assertFalse(area.contains(new ChunkPos(10, 10)));
    }

    @Test
    void testContains_NullChunk_ThrowsException() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertThrows(NullPointerException.class, () -> area.contains(null));
    }

    @Test
    void testIsAdjacentTo_ChunkAtRight_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertTrue(area.isAdjacentTo(new ChunkPos(1, 0)));
    }

    @Test
    void testIsAdjacentTo_ChunkAtLeft_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertTrue(area.isAdjacentTo(new ChunkPos(-1, 0)));
    }

    @Test
    void testIsAdjacentTo_ChunkAtTop_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertTrue(area.isAdjacentTo(new ChunkPos(0, 1)));
    }

    @Test
    void testIsAdjacentTo_ChunkAtBottom_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertTrue(area.isAdjacentTo(new ChunkPos(0, -1)));
    }

    @Test
    void testIsAdjacentTo_ChunkDiagonal_ReturnsFalse() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertFalse(area.isAdjacentTo(new ChunkPos(1, 1)));
        assertFalse(area.isAdjacentTo(new ChunkPos(-1, -1)));
    }

    @Test
    void testIsAdjacentTo_ChunkFarAway_ReturnsFalse() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertFalse(area.isAdjacentTo(new ChunkPos(10, 10)));
    }

    @Test
    void testIsAdjacentTo_WithMultipleChunks_ReturnsTrue() {
        PlayableArea area = new PlayableArea(1, adjacentChunks);

        // Adjacent to (0,0)
        assertTrue(area.isAdjacentTo(new ChunkPos(2, 0)));
        // Adjacent to (1,1)
        assertTrue(area.isAdjacentTo(new ChunkPos(2, 1)));
    }

    @Test
    void testIsAdjacentTo_NullChunk_ThrowsException() {
        PlayableArea area = new PlayableArea(1, singleChunk);

        assertThrows(NullPointerException.class, () -> area.isAdjacentTo(null));
    }

    @Test
    void testGetChunks_ReturnsImmutableSet() {
        PlayableArea area = new PlayableArea(1, singleChunk);
        Set<ChunkPos> chunks = area.getChunks();

        assertThrows(UnsupportedOperationException.class, () -> chunks.add(new ChunkPos(5, 5)));
    }

    @Test
    void testEquality_SameContent_AreEqual() {
        PlayableArea area1 = new PlayableArea(1, singleChunk);
        PlayableArea area2 = new PlayableArea(1, new HashSet<>(singleChunk));

        assertEquals(area1, area2);
        assertEquals(area1.hashCode(), area2.hashCode());
    }

    @Test
    void testEquality_DifferentId_NotEqual() {
        PlayableArea area1 = new PlayableArea(1, singleChunk);
        PlayableArea area2 = new PlayableArea(2, singleChunk);

        assertNotEquals(area1, area2);
    }

    @Test
    void testEquality_DifferentChunks_NotEqual() {
        PlayableArea area1 = new PlayableArea(1, singleChunk);
        PlayableArea area2 = new PlayableArea(1, adjacentChunks);

        assertNotEquals(area1, area2);
    }

    @Test
    void testToString_ContainsRelevantInfo() {
        PlayableArea area = new PlayableArea(42, adjacentChunks);
        String str = area.toString();

        assertTrue(str.contains("42"));
        assertTrue(str.contains("4"));
    }
}
