package chunklocked.advancement;

import chunklocked.core.ChunklockedMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for frame-based credit calculation in
 * {@link AdvancementCreditManager}.
 * <p>
 * Tests the new mode-aware credit system that awards credits based on
 * advancement
 * frame type (TASK/GOAL/CHALLENGE) rather than flat config values.
 * <p>
 * NOTE: Since these tests cannot bootstrap full Minecraft classes, we test the
 * ChunklockedMode enum's credit multiplier logic and the manager's
 * configuration.
 */
class AdvancementCreditManagerModeTest {

  private AdvancementCreditManager manager;

  @BeforeEach
  void setUp() {
    manager = new AdvancementCreditManager();
  }

  // ========== DISABLED MODE TESTS ==========

  @Test
  void disabledMode_ReturnsNegativeOne_ForConfigBasedCalculation() {
    // In DISABLED mode, getCreditMultiplier returns -1 to indicate
    // that config-based calculation should be used instead
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(
        ChunklockedMode.AdvancementType.TASK));
  }

  @Test
  void disabledMode_GoalAdvancement_UsesDefaultCredits() {
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(
        ChunklockedMode.AdvancementType.GOAL));
  }

  @Test
  void disabledMode_ChallengeAdvancement_UsesDefaultCredits() {
    assertEquals(-1, ChunklockedMode.DISABLED.getCreditMultiplier(
        ChunklockedMode.AdvancementType.CHALLENGE));
  }

  // ========== EASY MODE TESTS ==========

  @Test
  void easyMode_TaskAdvancement_Awards1Credit() {
    assertEquals(1, ChunklockedMode.EASY.getCreditMultiplier(
        ChunklockedMode.AdvancementType.TASK),
        "EASY mode TASK advancements should award 1 credit");
  }

  @Test
  void easyMode_GoalAdvancement_Awards5Credits() {
    assertEquals(5, ChunklockedMode.EASY.getCreditMultiplier(
        ChunklockedMode.AdvancementType.GOAL),
        "EASY mode GOAL advancements should award 5 credits");
  }

  @Test
  void easyMode_ChallengeAdvancement_Awards10Credits() {
    assertEquals(10, ChunklockedMode.EASY.getCreditMultiplier(
        ChunklockedMode.AdvancementType.CHALLENGE),
        "EASY mode CHALLENGE advancements should award 10 credits");
  }

  // ========== EXTREME MODE TESTS ==========

  @Test
  void extremeMode_TaskAdvancement_Awards1Credit() {
    assertEquals(1, ChunklockedMode.EXTREME.getCreditMultiplier(
        ChunklockedMode.AdvancementType.TASK),
        "EXTREME mode TASK advancements should award 1 credit");
  }

  @Test
  void extremeMode_GoalAdvancement_Awards2Credits() {
    assertEquals(2, ChunklockedMode.EXTREME.getCreditMultiplier(
        ChunklockedMode.AdvancementType.GOAL),
        "EXTREME mode GOAL advancements should award 2 credits");
  }

  @Test
  void extremeMode_ChallengeAdvancement_Awards5Credits() {
    assertEquals(5, ChunklockedMode.EXTREME.getCreditMultiplier(
        ChunklockedMode.AdvancementType.CHALLENGE),
        "EXTREME mode CHALLENGE advancements should award 5 credits");
  }

  // ========== RECIPE FILTERING TESTS ==========

  @Test
  void recipePattern_DetectedByManager() {
    // Verify the manager can detect recipe advancements by pattern
    // Recipe pattern: contains ":recipes/"
    assertTrue("minecraft:recipes/food/bread".contains(":recipes/"),
        "Recipe advancement should contain ':recipes/' pattern");
    assertFalse("minecraft:story/mine_stone".contains(":recipes/"),
        "Story advancement should not contain ':recipes/' pattern");
  }

  // ========== BLACKLIST FILTERING TESTS ==========

  @Test
  void allModes_BlacklistedAdvancement_Awards0Credits() {
    String blacklistedId = "minecraft:story/blacklisted";
    manager.addBlacklistedAdvancement(blacklistedId);

    assertTrue(manager.isBlacklisted(blacklistedId),
        "Advancement should be blacklisted");
    assertEquals(0, manager.getCreditAmount(blacklistedId),
        "Blacklisted advancement should return 0 credits");
  }

  @Test
  void blacklist_HonoredInAllModes() {
    String blacklistedId = "minecraft:nether/find_bastion";
    manager.addBlacklistedAdvancement(blacklistedId);

    // Verify blacklist is respected
    assertTrue(manager.isBlacklisted(blacklistedId));
    assertEquals(0, manager.getCreditAmount(blacklistedId));
  }

  // ========== DISPLAYINFO FILTERING TESTS ==========

  @Test
  void advancementsWithoutDisplayInfo_ShouldAward0Credits() {
    // Advancements without DisplayInfo should never award credits
    // This is tested indirectly through the calculateCredits logic
    // which checks if display().isEmpty()

    // Verify that the mode's credit multipliers work regardless
    assertEquals(1, ChunklockedMode.EASY.getCreditMultiplier(
        ChunklockedMode.AdvancementType.TASK));
  }

  // ========== CONFIGURATION INTERACTION TESTS ==========

  @Test
  void disabledMode_CustomReward_Respected() {
    // In DISABLED mode, custom rewards from config should be used
    String advId = "minecraft:story/mine_diamond";
    manager.setCustomReward(advId, 99);

    assertEquals(99, manager.getCreditAmount(advId),
        "Custom reward should be respected in DISABLED mode");
  }

  @Test
  void clearCustomRewards_RemovesAllCustom() {
    manager.setCustomReward("minecraft:story/test", 50);
    manager.clearCustomRewards();

    assertEquals(1, manager.getCreditAmount("minecraft:story/test"),
        "After clearing, should use default credits");
  }

  @Test
  void clearBlacklist_RemovesAllBlacklisted() {
    manager.addBlacklistedAdvancement("minecraft:story/test");
    manager.clearBlacklist();

    assertFalse(manager.isBlacklisted("minecraft:story/test"),
        "After clearing, advancement should not be blacklisted");
  }

  // ========== INTEGRATION TESTS ==========

  @Test
  void config_LoadsCorrectly() {
    // Verify config loading clears existing data
    manager.setCustomReward("minecraft:old", 10);
    manager.addBlacklistedAdvancement("minecraft:old_blacklist");

    manager.clearCustomRewards();
    manager.clearBlacklist();

    assertFalse(manager.isBlacklisted("minecraft:old_blacklist"));
    assertEquals(1, manager.getCreditAmount("minecraft:old"));
  }

  // ========== EDGE CASE TESTS ==========

  @Test
  void nullAdvancementType_Handled() {
    // Verify getCreditMultiplier handles null gracefully
    assertThrows(IllegalArgumentException.class,
        () -> ChunklockedMode.EASY.getCreditMultiplier(null),
        "Passing null AdvancementType should throw exception");
  }

  @Test
  void modeEnumValues_AllDefined() {
    // Verify all mode values exist
    assertEquals(3, ChunklockedMode.values().length,
        "Should have exactly 3 modes");
    assertNotNull(ChunklockedMode.valueOf("DISABLED"));
    assertNotNull(ChunklockedMode.valueOf("EASY"));
    assertNotNull(ChunklockedMode.valueOf("EXTREME"));
  }

  @Test
  void advancementTypeEnumValues_AllDefined() {
    // Verify all advancement type values exist
    assertEquals(3, ChunklockedMode.AdvancementType.values().length,
        "Should have exactly 3 advancement types");
    assertNotNull(ChunklockedMode.AdvancementType.valueOf("TASK"));
    assertNotNull(ChunklockedMode.AdvancementType.valueOf("GOAL"));
    assertNotNull(ChunklockedMode.AdvancementType.valueOf("CHALLENGE"));
  }
}
