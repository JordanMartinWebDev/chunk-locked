package chunklocked.core.area;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AreaDetector class.
 * 
 * Tests verify BFS-based connected component detection for chunk areas.
 * 
 * See: https://fabricmc.net/wiki/tutorial:testing
 */
public class AreaDetectorTest {
    private AreaDetector detector;
    private Set<ChunkPos> emptySet;
    private Set<ChunkPos> singleChunk;
    private Set<ChunkPos> twoByTwoGrid;
    private Set<ChunkPos> disconnected;
    private Set<ChunkPos> lShape;
    private Set<ChunkPos> largeContiguous;
    private Set<ChunkPos> negativeCoordinates;
    private Set<ChunkPos> mixedCoordinates;

    @BeforeEach
    void setUp() {
        detector = new AreaDetector();

        emptySet = new HashSet<>();

        singleChunk = new HashSet<>();
        singleChunk.add(new ChunkPos(0, 0));

        twoByTwoGrid = new HashSet<>();
        twoByTwoGrid.add(new ChunkPos(0, 0));
        twoByTwoGrid.add(new ChunkPos(1, 0));
        twoByTwoGrid.add(new ChunkPos(0, 1));
        twoByTwoGrid.add(new ChunkPos(1, 1));

        disconnected = new HashSet<>();
        disconnected.add(new ChunkPos(0, 0));
        disconnected.add(new ChunkPos(5, 5));

        lShape = new HashSet<>();
        lShape.add(new ChunkPos(0, 0));
        lShape.add(new ChunkPos(0, 1));
        lShape.add(new ChunkPos(0, 2));
        lShape.add(new ChunkPos(1, 0));

        largeContiguous = new HashSet<>();
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                largeContiguous.add(new ChunkPos(x, z));
            }
        }

        negativeCoordinates = new HashSet<>();
        negativeCoordinates.add(new ChunkPos(-2, -2));
        negativeCoordinates.add(new ChunkPos(-1, -2));
        negativeCoordinates.add(new ChunkPos(-2, -1));
        negativeCoordinates.add(new ChunkPos(-1, -1));

        mixedCoordinates = new HashSet<>();
        mixedCoordinates.add(new ChunkPos(-1, -1));
        mixedCoordinates.add(new ChunkPos(0, 0));
        mixedCoordinates.add(new ChunkPos(1, 1));
    }

    @Test
    void testDetectAreas_EmptySet_ReturnsEmptyList() {
        List<PlayableArea> areas = detector.detectAreas(emptySet);

        assertEquals(0, areas.size());
    }

    @Test
    void testDetectAreas_SingleChunk_ReturnsOneArea() {
        List<PlayableArea> areas = detector.detectAreas(singleChunk);

        assertEquals(1, areas.size());
        assertEquals(1, areas.get(0).getChunkCount());
        assertTrue(areas.get(0).contains(new ChunkPos(0, 0)));
    }

    @Test
    void testDetectAreas_2x2Grid_ReturnsOneArea() {
        List<PlayableArea> areas = detector.detectAreas(twoByTwoGrid);

        assertEquals(1, areas.size());
        assertEquals(4, areas.get(0).getChunkCount());
    }

    @Test
    void testDetectAreas_DisconnectedChunks_ReturnsTwoAreas() {
        List<PlayableArea> areas = detector.detectAreas(disconnected);

        assertEquals(2, areas.size());
        assertEquals(1, areas.get(0).getChunkCount());
        assertEquals(1, areas.get(1).getChunkCount());
    }

    @Test
    void testDetectAreas_LShape_ReturnsOneArea() {
        List<PlayableArea> areas = detector.detectAreas(lShape);

        assertEquals(1, areas.size());
        assertEquals(4, areas.get(0).getChunkCount());
    }

    @Test
    void testDetectAreas_LargeContiguousRegion_ReturnsOneArea() {
        List<PlayableArea> areas = detector.detectAreas(largeContiguous);

        assertEquals(1, areas.size());
        assertEquals(100, areas.get(0).getChunkCount());
    }

    @Test
    void testDetectAreas_NegativeCoordinates_ReturnsOneArea() {
        List<PlayableArea> areas = detector.detectAreas(negativeCoordinates);

        assertEquals(1, areas.size());
        assertEquals(4, areas.get(0).getChunkCount());
    }

    @Test
    void testDetectAreas_MixedCoordinates_ReturnsSeparateAreas() {
        // (-1,-1) to (0,0) are adjacent
        // (0,0) to (1,1) are not adjacent (diagonal)
        List<PlayableArea> areas = detector.detectAreas(mixedCoordinates);

        assertEquals(2, areas.size());
    }

    @Test
    void testDetectAreas_NullInput_ThrowsException() {
        assertThrows(NullPointerException.class, () -> detector.detectAreas(null));
    }

    @Test
    void testDetectAreas_SetContainsNull_ThrowsException() {
        Set<ChunkPos> chunksWithNull = new HashSet<>(singleChunk);
        chunksWithNull.add(null);

        assertThrows(NullPointerException.class, () -> detector.detectAreas(chunksWithNull));
    }

    @Test
    void testCaching_SecondCall_ReturnsCached() {
        List<PlayableArea> areas1 = detector.detectAreas(singleChunk);
        assertTrue(detector.isCacheValid());

        List<PlayableArea> areas2 = detector.detectAreas(singleChunk);

        assertEquals(areas1.size(), areas2.size());
        assertEquals(areas1.get(0).getChunkCount(), areas2.get(0).getChunkCount());
    }

    @Test
    void testCaching_DifferentSet_Recomputes() {
        List<PlayableArea> areas1 = detector.detectAreas(singleChunk);
        assertEquals(1, areas1.size());

        List<PlayableArea> areas2 = detector.detectAreas(twoByTwoGrid);
        assertEquals(1, areas2.size());
        assertEquals(4, areas2.get(0).getChunkCount());
    }

    @Test
    void testInvalidateCache_AfterDetect_MakesInvalid() {
        detector.detectAreas(singleChunk);
        assertTrue(detector.isCacheValid());

        detector.invalidateCache();
        assertFalse(detector.isCacheValid());
    }

    @Test
    void testInvalidateCache_ClearsData() {
        detector.detectAreas(singleChunk);
        assertEquals(1, detector.getCachedAreaCount());

        detector.invalidateCache();
        assertEquals(0, detector.getCachedAreaCount());
    }

    @Test
    void testFindAreaContainingChunk_ChunkExists_ReturnsArea() {
        detector.detectAreas(twoByTwoGrid);
        Optional<PlayableArea> area = detector.findAreaContainingChunk(new ChunkPos(0, 0));

        assertTrue(area.isPresent());
        assertEquals(4, area.get().getChunkCount());
    }

    @Test
    void testFindAreaContainingChunk_ChunkNotExists_ReturnsEmpty() {
        detector.detectAreas(singleChunk);
        Optional<PlayableArea> area = detector.findAreaContainingChunk(new ChunkPos(5, 5));

        assertFalse(area.isPresent());
    }

    @Test
    void testFindAreaContainingChunk_NoDetectionCalled_ReturnsEmpty() {
        Optional<PlayableArea> area = detector.findAreaContainingChunk(new ChunkPos(0, 0));

        assertFalse(area.isPresent());
    }

    @Test
    void testFindAreaContainingChunk_NullChunk_ThrowsException() {
        detector.detectAreas(singleChunk);

        assertThrows(NullPointerException.class, () -> detector.findAreaContainingChunk(null));
    }

    @Test
    void testFindAdjacentAreas_ChunkAdjacentToArea_ReturnsArea() {
        detector.detectAreas(disconnected);
        List<PlayableArea> adjacent = detector.findAdjacentAreas(new ChunkPos(1, 0));

        assertEquals(1, adjacent.size());
        assertTrue(adjacent.get(0).contains(new ChunkPos(0, 0)));
    }

    @Test
    void testFindAdjacentAreas_ChunkNotAdjacent_ReturnsEmpty() {
        detector.detectAreas(disconnected);
        List<PlayableArea> adjacent = detector.findAdjacentAreas(new ChunkPos(3, 3));

        assertEquals(0, adjacent.size());
    }

    @Test
    void testFindAdjacentAreas_ChunkBetweenTwoAreas_ReturnsBothAreas() {
        Set<ChunkPos> chunks = new HashSet<>();
        chunks.add(new ChunkPos(0, 0));
        chunks.add(new ChunkPos(2, 0));

        detector.detectAreas(chunks);
        List<PlayableArea> adjacent = detector.findAdjacentAreas(new ChunkPos(1, 0));

        assertEquals(2, adjacent.size());
    }

    @Test
    void testFindAdjacentAreas_NullChunk_ThrowsException() {
        detector.detectAreas(singleChunk);

        assertThrows(NullPointerException.class, () -> detector.findAdjacentAreas(null));
    }

    @Test
    void testGetCachedAreas_ReturnsCopy() {
        detector.detectAreas(singleChunk);
        List<PlayableArea> areas1 = detector.getCachedAreas();
        List<PlayableArea> areas2 = detector.getCachedAreas();

        assertEquals(areas1.size(), areas2.size());
        assertNotSame(areas1, areas2);
    }

    @Test
    void testGetCachedAreaCount_BeforeDetect_ReturnsZero() {
        assertEquals(0, detector.getCachedAreaCount());
    }

    @Test
    void testGetCachedAreaCount_AfterDetect_ReturnsCorrectCount() {
        detector.detectAreas(disconnected);
        assertEquals(2, detector.getCachedAreaCount());
    }

    @Test
    void testAreaIds_AreUnique() {
        List<PlayableArea> areas = detector.detectAreas(disconnected);

        int id1 = areas.get(0).getAreaId();
        int id2 = areas.get(1).getAreaId();

        assertNotEquals(id1, id2);
    }

    @Test
    void testDetectAreas_Performance_LargeDataset() {
        Set<ChunkPos> largeSet = new HashSet<>();
        for (int x = 0; x < 50; x++) {
            for (int z = 0; z < 50; z++) {
                largeSet.add(new ChunkPos(x, z));
            }
        }

        long startTime = System.currentTimeMillis();
        List<PlayableArea> areas = detector.detectAreas(largeSet);
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(1, areas.size());
        assertEquals(2500, areas.get(0).getChunkCount());
        assertTrue(duration < 500, "Area detection should complete in less than 500ms");
    }

    @Test
    void testComplexDisconnectedScenario_MultipleDisconnectedRegions() {
        Set<ChunkPos> chunks = new HashSet<>();
        // Area 1: 2x2 grid at origin
        chunks.add(new ChunkPos(0, 0));
        chunks.add(new ChunkPos(1, 0));
        chunks.add(new ChunkPos(0, 1));
        chunks.add(new ChunkPos(1, 1));

        // Area 2: Single chunk far away
        chunks.add(new ChunkPos(10, 10));

        // Area 3: Another 2x2 grid
        chunks.add(new ChunkPos(20, 20));
        chunks.add(new ChunkPos(21, 20));
        chunks.add(new ChunkPos(20, 21));
        chunks.add(new ChunkPos(21, 21));

        List<PlayableArea> areas = detector.detectAreas(chunks);

        assertEquals(3, areas.size());
        assertTrue(areas.stream().anyMatch(a -> a.getChunkCount() == 4 && a.contains(new ChunkPos(0, 0))));
        assertTrue(areas.stream().anyMatch(a -> a.getChunkCount() == 1 && a.contains(new ChunkPos(10, 10))));
        assertTrue(areas.stream().anyMatch(a -> a.getChunkCount() == 4 && a.contains(new ChunkPos(20, 20))));
    }

    @Test
    void testRedetectionAfterInvalidation_SameResult() {
        List<PlayableArea> areas1 = detector.detectAreas(twoByTwoGrid);
        assertEquals(1, areas1.size());
        assertEquals(4, areas1.get(0).getChunkCount());

        detector.invalidateCache();
        List<PlayableArea> areas2 = detector.detectAreas(twoByTwoGrid);

        assertEquals(areas1.size(), areas2.size());
        assertEquals(areas1.get(0).getChunkCount(), areas2.get(0).getChunkCount());
    }

    @Test
    void testAreasDoNotShareChunks_MultipleAreas() {
        List<PlayableArea> areas = detector.detectAreas(disconnected);

        Set<ChunkPos> allChunks = new HashSet<>();
        for (PlayableArea area : areas) {
            for (ChunkPos chunk : area.getChunks()) {
                assertFalse(allChunks.contains(chunk), "Chunk " + chunk + " appears in multiple areas");
                allChunks.add(chunk);
            }
        }

        assertEquals(disconnected.size(), allChunks.size());
    }
}
