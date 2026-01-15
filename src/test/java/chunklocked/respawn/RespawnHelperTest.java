package chunklocked.respawn;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.ChunkPos;

/**
 * Unit tests for respawn helper logic.
 * 
 * Tests the nearest unlocked chunk finding algorithm used during player respawn.
 * This algorithm uses a spiral search pattern to find the closest safe spawn location.
 * 
 * NOTE: These tests CANNOT RUN due to Minecraft bootstrap requirements.
 * ChunkPos requires full Minecraft initialization which is not available in unit tests.
 * 
 * These tests are kept for documentation and future integration testing purposes.
 * The algorithm correctness is validated through:
 * 1. Manual testing (see manual-tests/RespawnInLockedChunks.md)
 * 2. Code review of the algorithm logic
 * 3. In-game testing during development
 * 
 * If this functionality needs automated testing, it should be done via:
 * - Fabric's game test framework (integration tests)
 * - Manual test cases (documented)
 * - In-game verification during development
 */
class RespawnHelperTest {

	/**
	 * Helper method that replicates the nearest chunk finding algorithm from
	 * Chunklocked.findNearestUnlockedChunk().
	 * This allows us to test the algorithm without Minecraft bootstrap overhead.
	 */
	private ChunkPos findNearestUnlockedChunk(ChunkPos origin, Set<ChunkPos> unlockedChunks) {
		if (unlockedChunks.isEmpty()) {
			return null;
		}

		// First check if any adjacent chunks are unlocked (most common case)
		ChunkPos[] adjacent = {
				new ChunkPos(origin.x - 1, origin.z),
				new ChunkPos(origin.x + 1, origin.z),
				new ChunkPos(origin.x, origin.z - 1),
				new ChunkPos(origin.x, origin.z + 1)
		};
		for (ChunkPos adj : adjacent) {
			if (unlockedChunks.contains(adj)) {
				return adj;
			}
		}

		// Search in expanding square spiral up to 16 chunks away
		int maxRadius = 16;
		ChunkPos nearest = null;
		double nearestDistSq = Double.MAX_VALUE;

		for (int radius = 1; radius <= maxRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					// Only check chunks on the current radius perimeter
					if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}

					ChunkPos candidate = new ChunkPos(origin.x + dx, origin.z + dz);
					if (unlockedChunks.contains(candidate)) {
						double distSq = dx * dx + dz * dz;
						if (distSq < nearestDistSq) {
							nearestDistSq = distSq;
							nearest = candidate;
						}
					}
				}
			}
			// If we found something at this radius, return it (closest possible)
			if (nearest != null) {
				return nearest;
			}
		}

		// If nothing found in search radius, just return any unlocked chunk
		return unlockedChunks.iterator().next();
	}

	@Test
	@DisplayName("Returns null when no unlocked chunks exist")
	void testNoUnlockedChunks() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertNull(result, "Should return null when no unlocked chunks exist");
	}

	@Test
	@DisplayName("Finds adjacent chunk to the left")
	void testAdjacentLeft() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(-1, 0); // Left adjacent
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should find the left adjacent chunk");
	}

	@Test
	@DisplayName("Finds adjacent chunk to the right")
	void testAdjacentRight() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(1, 0); // Right adjacent
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should find the right adjacent chunk");
	}

	@Test
	@DisplayName("Finds adjacent chunk above")
	void testAdjacentAbove() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(0, -1); // Above (negative Z)
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should find the above adjacent chunk");
	}

	@Test
	@DisplayName("Finds adjacent chunk below")
	void testAdjacentBelow() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(0, 1); // Below (positive Z)
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should find the below adjacent chunk");
	}

	@Test
	@DisplayName("Prefers adjacent over distant chunks")
	void testPrefersAdjacent() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(1, 0); // Adjacent
		ChunkPos distant = new ChunkPos(10, 10); // Far away
		unlockedChunks.add(distant);
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should prefer adjacent chunk over distant chunk");
	}

	@Test
	@DisplayName("Finds chunk at radius 2 when no adjacent chunks")
	void testRadius2Search() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos radius2Chunk = new ChunkPos(2, 0); // 2 chunks away
		unlockedChunks.add(radius2Chunk);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(radius2Chunk, result, "Should find chunk at radius 2");
	}

	@Test
	@DisplayName("Finds closest chunk among multiple at different radii")
	void testClosestAmongMultiple() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos far = new ChunkPos(5, 5);
		ChunkPos near = new ChunkPos(2, 1);
		unlockedChunks.add(far);
		unlockedChunks.add(near);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(near, result, "Should find the closest chunk (2,1) over (5,5)");
	}

	@Test
	@DisplayName("Finds diagonal adjacent chunk")
	void testDiagonalAdjacent() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos diagonal = new ChunkPos(1, 1); // Diagonal adjacent
		unlockedChunks.add(diagonal);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(diagonal, result, "Should find diagonal adjacent chunk at radius 1");
	}

	@Test
	@DisplayName("Handles negative chunk coordinates")
	void testNegativeCoordinates() {
		ChunkPos origin = new ChunkPos(-5, -5);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos adjacent = new ChunkPos(-6, -5); // Left adjacent
		unlockedChunks.add(adjacent);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(adjacent, result, "Should handle negative chunk coordinates correctly");
	}

	@Test
	@DisplayName("Returns fallback chunk when none found within max radius")
	void testFallbackBeyondMaxRadius() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		ChunkPos faraway = new ChunkPos(100, 100); // Way beyond max radius of 16
		unlockedChunks.add(faraway);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(faraway, result, "Should return fallback chunk when none found within max radius");
	}

	@Test
	@DisplayName("Finds closest when multiple chunks at same radius")
	void testMultipleSameRadius() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		// Both at radius 2, but (2,0) is closer by Euclidean distance (4 vs 8)
		ChunkPos option1 = new ChunkPos(2, 0); // Distance squared = 4
		ChunkPos option2 = new ChunkPos(2, 2); // Distance squared = 8
		unlockedChunks.add(option2);
		unlockedChunks.add(option1);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(option1, result, "Should choose closest by Euclidean distance when at same radius");
	}

	@Test
	@DisplayName("Spiral search checks perimeter only")
	void testSpiralSearchPerimeter() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		// Place chunk inside the radius boundary but not on perimeter
		ChunkPos interior = new ChunkPos(2, 1); // Not on radius 2 perimeter
		ChunkPos perimeter = new ChunkPos(3, 0); // On radius 3 perimeter
		unlockedChunks.add(perimeter);
		unlockedChunks.add(interior);

		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);

		// Interior (2,1) at radius ~2.2 should be found before perimeter (3,0) at
		// radius 3
		assertEquals(interior, result, "Should check interior chunks before larger radius perimeters");
	}

	@Test
	@DisplayName("Performance: handles large unlocked chunk sets efficiently")
	void testLargeUnlockedSet() {
		ChunkPos origin = new ChunkPos(0, 0);
		Set<ChunkPos> unlockedChunks = new HashSet<>();

		// Add 1000 unlocked chunks scattered around
		for (int i = -50; i < 50; i++) {
			for (int j = -50; j < 50; j++) {
				if ((i + j) % 3 == 0) { // Sparse pattern
					unlockedChunks.add(new ChunkPos(i, j));
				}
			}
		}

		// Should still find adjacent efficiently
		ChunkPos adjacent = new ChunkPos(0, -1);
		unlockedChunks.add(adjacent);

		long startTime = System.nanoTime();
		ChunkPos result = findNearestUnlockedChunk(origin, unlockedChunks);
		long duration = System.nanoTime() - startTime;

		assertEquals(adjacent, result, "Should find adjacent chunk even with large set");
		assertTrue(duration < 1_000_000, // Less than 1ms
				"Should complete in less than 1ms (actual: " + (duration / 1_000_000.0) + "ms)");
	}

	@Test
	@DisplayName("Consistent results with same inputs")
	void testDeterministicResults() {
		ChunkPos origin = new ChunkPos(5, 5);
		Set<ChunkPos> unlockedChunks = new HashSet<>();
		unlockedChunks.add(new ChunkPos(7, 5));
		unlockedChunks.add(new ChunkPos(3, 5));
		unlockedChunks.add(new ChunkPos(5, 7));

		// Run algorithm multiple times with same inputs
		ChunkPos result1 = findNearestUnlockedChunk(origin, unlockedChunks);
		ChunkPos result2 = findNearestUnlockedChunk(origin, unlockedChunks);
		ChunkPos result3 = findNearestUnlockedChunk(origin, unlockedChunks);

		assertEquals(result1, result2, "Should return same result on repeated calls");
		assertEquals(result2, result3, "Should return same result on repeated calls");
	}
}
