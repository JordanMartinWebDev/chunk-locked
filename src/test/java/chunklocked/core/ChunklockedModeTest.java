package chunklocked.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import chunklocked.core.ChunklockedMode.AdvancementType;

/**
 * Unit tests for {@link ChunklockedMode}.
 * <p>
 * Verifies mode behavior, credit multipliers, and active state detection.
 */
class ChunklockedModeTest {

  @Test
  void isActive_DisabledMode_ReturnsFalse() {
    assertFalse(ChunklockedMode.DISABLED.isActive(),
        "DISABLED mode should not be active");
  }

  @Test
  void isActive_EasyMode_ReturnsTrue() {
    assertTrue(ChunklockedMode.EASY.isActive(),
        "EASY mode should be active");
  }

  @Test
  void isActive_ExtremeMode_ReturnsTrue() {
    assertTrue(ChunklockedMode.EXTREME.isActive(),
        "EXTREME mode should be active");
  }

  @Test
  void getCreditMultiplier_DisabledMode_ReturnsNegativeOne() {
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(AdvancementType.TASK),
        "DISABLED mode should return -1 for TASK (use config)");
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(AdvancementType.GOAL),
        "DISABLED mode should return -1 for GOAL (use config)");
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(AdvancementType.CHALLENGE),
        "DISABLED mode should return -1 for CHALLENGE (use config)");
  }

  @Test
  void getCreditMultiplier_EasyMode_ReturnsCorrectValues() {
    assertEquals(1, ChunklockedMode.EASY.getCreditMultiplier(AdvancementType.TASK),
        "EASY mode TASK should award 1 credit");
    assertEquals(5, ChunklockedMode.EASY.getCreditMultiplier(AdvancementType.GOAL),
        "EASY mode GOAL should award 5 credits");
    assertEquals(10, ChunklockedMode.EASY.getCreditMultiplier(AdvancementType.CHALLENGE),
        "EASY mode CHALLENGE should award 10 credits");
  }

  @Test
  void getCreditMultiplier_ExtremeMode_ReturnsCorrectValues() {
    assertEquals(1, ChunklockedMode.EXTREME.getCreditMultiplier(AdvancementType.TASK),
        "EXTREME mode TASK should award 1 credit");
    assertEquals(2, ChunklockedMode.EXTREME.getCreditMultiplier(AdvancementType.GOAL),
        "EXTREME mode GOAL should award 2 credits");
    assertEquals(5, ChunklockedMode.EXTREME.getCreditMultiplier(AdvancementType.CHALLENGE),
        "EXTREME mode CHALLENGE should award 5 credits");
  }

  @Test
  void getCreditMultiplier_NullType_ThrowsException() {
    assertThrows(IllegalArgumentException.class,
        () -> ChunklockedMode.EASY.getCreditMultiplier(null),
        "Passing null AdvancementType should throw IllegalArgumentException");
  }

  @Test
  void advancementType_AllValuesExist() {
    assertEquals(3, AdvancementType.values().length,
        "AdvancementType should have exactly 3 values");
    assertNotNull(AdvancementType.valueOf("TASK"));
    assertNotNull(AdvancementType.valueOf("GOAL"));
    assertNotNull(AdvancementType.valueOf("CHALLENGE"));
  }
}
