package chunklocked.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StarterItemsManager}.
 * <p>
 * Tests the logic for determining when starter items should be given
 * and validates the shouldGiveStarterItems method.
 * <p>
 * NOTE: Testing the actual item-giving logic requires a full Minecraft
 * server environment with ServerPlayer instances, so that is covered
 * by manual/integration tests instead.
 */
class StarterItemsManagerTest {

  // ========== SHOULD GIVE TESTS ==========

  @Test
  void shouldGiveStarterItems_EasyModeFirstJoin_ReturnsTrue() {
    assertTrue(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.EASY, false),
        "Should give starter items in EASY mode on first join");
  }

  @Test
  void shouldGiveStarterItems_ExtremeModeFirstJoin_ReturnsTrue() {
    assertTrue(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.EXTREME, false),
        "Should give starter items in EXTREME mode on first join");
  }

  @Test
  void shouldGiveStarterItems_DisabledMode_ReturnsFalse() {
    assertFalse(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.DISABLED, false),
        "Should NOT give starter items in DISABLED mode");
  }

  @Test
  void shouldGiveStarterItems_EasyModeAlreadyReceived_ReturnsFalse() {
    assertFalse(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.EASY, true),
        "Should NOT give starter items if already received");
  }

  @Test
  void shouldGiveStarterItems_ExtremeModeAlreadyReceived_ReturnsFalse() {
    assertFalse(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.EXTREME, true),
        "Should NOT give starter items if already received");
  }

  @Test
  void shouldGiveStarterItems_DisabledModeAlreadyReceived_ReturnsFalse() {
    assertFalse(StarterItemsManager.shouldGiveStarterItems(
        ChunklockedMode.DISABLED, true),
        "Should NOT give starter items in DISABLED mode even if not received");
  }

  // ========== NULL HANDLING TESTS ==========

  @Test
  void giveStarterItems_NullPlayer_ThrowsException() {
    assertThrows(IllegalArgumentException.class,
        () -> StarterItemsManager.giveStarterItems(null),
        "Passing null player should throw IllegalArgumentException");
  }

  // ========== MODE ACTIVE CHECK ==========

  @Test
  void modeActive_DisabledMode_ReturnsFalse() {
    assertFalse(ChunklockedMode.DISABLED.isActive(),
        "DISABLED mode should not be active");
  }

  @Test
  void modeActive_EasyMode_ReturnsTrue() {
    assertTrue(ChunklockedMode.EASY.isActive(),
        "EASY mode should be active");
  }

  @Test
  void modeActive_ExtremeMode_ReturnsTrue() {
    assertTrue(ChunklockedMode.EXTREME.isActive(),
        "EXTREME mode should be active");
  }
}
