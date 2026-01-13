package chunkloaded.advancement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerProgressionData.
 * <p>
 * Tests all credit operations, advancement tracking, and edge cases.
 */
@DisplayName("PlayerProgressionData Tests")
class PlayerProgressionDataTest {

  private PlayerProgressionData data;

  @BeforeEach
  void setUp() {
    data = new PlayerProgressionData();
  }

  // ========== INITIALIZATION TESTS ==========

  @Test
  @DisplayName("New instance should have zero credits")
  void testNewInstance_ZeroCredits() {
    assertEquals(0, data.getAvailableCredits());
  }

  @Test
  @DisplayName("New instance should have zero advancements completed")
  void testNewInstance_ZeroAdvancements() {
    assertEquals(0, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("New instance should have empty rewarded advancements set")
  void testNewInstance_EmptyRewardedSet() {
    assertTrue(data.getRewardedAdvancements().isEmpty());
  }

  // ========== ADD CREDITS TESTS ==========

  @Test
  @DisplayName("Adding positive credits should increase balance")
  void testAddCredits_PositiveAmount_IncreasesBalance() {
    data.addCredits(5);
    assertEquals(5, data.getAvailableCredits());

    data.addCredits(3);
    assertEquals(8, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Adding zero credits should not change balance")
  void testAddCredits_Zero_NoChange() {
    data.addCredits(0);
    assertEquals(0, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Adding negative credits should throw exception")
  void testAddCredits_NegativeAmount_ThrowsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.addCredits(-1));
    assertTrue(exception.getMessage().contains("negative"));
  }

  @Test
  @DisplayName("Adding large amount of credits should work")
  void testAddCredits_LargeAmount_Success() {
    data.addCredits(Integer.MAX_VALUE - 1);
    assertEquals(Integer.MAX_VALUE - 1, data.getAvailableCredits());
  }

  // ========== SPEND CREDITS TESTS ==========

  @Test
  @DisplayName("Spending credits should decrease balance")
  void testSpendCredits_ValidAmount_DecreasesBalance() {
    data.addCredits(10);
    data.spendCredits(3);
    assertEquals(7, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Spending all credits should result in zero balance")
  void testSpendCredits_AllCredits_ZeroBalance() {
    data.addCredits(5);
    data.spendCredits(5);
    assertEquals(0, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Spending more than available should throw exception")
  void testSpendCredits_ExceedsAvailable_ThrowsException() {
    data.addCredits(5);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.spendCredits(6));
    assertTrue(exception.getMessage().contains("Insufficient"));
  }

  @Test
  @DisplayName("Spending zero credits should throw exception")
  void testSpendCredits_Zero_ThrowsException() {
    data.addCredits(5);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.spendCredits(0));
    assertTrue(exception.getMessage().contains("non-positive"));
  }

  @Test
  @DisplayName("Spending negative credits should throw exception")
  void testSpendCredits_Negative_ThrowsException() {
    data.addCredits(5);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.spendCredits(-1));
    assertTrue(exception.getMessage().contains("non-positive"));
  }

  @Test
  @DisplayName("Spending from zero balance should throw exception")
  void testSpendCredits_ZeroBalance_ThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> data.spendCredits(1));
  }

  // ========== ADVANCEMENT TRACKING TESTS ==========

  @Test
  @DisplayName("Marking advancement as rewarded should add to set")
  void testMarkAdvancementRewarded_AddsToSet() {
    data.markAdvancementRewarded("minecraft:story/mine_stone");
    assertTrue(data.hasReceivedReward("minecraft:story/mine_stone"));
  }

  @Test
  @DisplayName("Marking advancement should increment total count")
  void testMarkAdvancementRewarded_IncrementsTotal() {
    data.markAdvancementRewarded("advancement1");
    assertEquals(1, data.getTotalAdvancementsCompleted());

    data.markAdvancementRewarded("advancement2");
    assertEquals(2, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Marking same advancement twice should increment count twice")
  void testMarkAdvancementRewarded_Duplicate_IncrementsCount() {
    data.markAdvancementRewarded("advancement1");
    data.markAdvancementRewarded("advancement1");
    assertEquals(2, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Checking non-existent advancement should return false")
  void testHasReceivedReward_NonExistent_ReturnsFalse() {
    assertFalse(data.hasReceivedReward("nonexistent"));
  }

  @Test
  @DisplayName("Multiple advancements should all be tracked")
  void testMarkAdvancementRewarded_MultipleAdvancements_AllTracked() {
    data.markAdvancementRewarded("adv1");
    data.markAdvancementRewarded("adv2");
    data.markAdvancementRewarded("adv3");

    assertTrue(data.hasReceivedReward("adv1"));
    assertTrue(data.hasReceivedReward("adv2"));
    assertTrue(data.hasReceivedReward("adv3"));
    assertEquals(3, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Rewarded advancements set should be unmodifiable")
  void testGetRewardedAdvancements_ReturnsUnmodifiableSet() {
    data.markAdvancementRewarded("adv1");
    Set<String> rewarded = data.getRewardedAdvancements();

    assertThrows(UnsupportedOperationException.class, () -> rewarded.add("adv2"));
  }

  @Test
  @DisplayName("Rewarded advancements set should reflect current state")
  void testGetRewardedAdvancements_ReflectsCurrentState() {
    data.markAdvancementRewarded("adv1");
    data.markAdvancementRewarded("adv2");

    Set<String> rewarded = data.getRewardedAdvancements();
    assertEquals(2, rewarded.size());
    assertTrue(rewarded.contains("adv1"));
    assertTrue(rewarded.contains("adv2"));
  }

  // ========== SET CREDITS TESTS (Deserialization) ==========

  @Test
  @DisplayName("Setting credits directly should update balance")
  void testSetAvailableCredits_UpdatesBalance() {
    data.setAvailableCredits(100);
    assertEquals(100, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Setting credits to zero should work")
  void testSetAvailableCredits_Zero_Success() {
    data.addCredits(50);
    data.setAvailableCredits(0);
    assertEquals(0, data.getAvailableCredits());
  }

  @Test
  @DisplayName("Setting negative credits should throw exception")
  void testSetAvailableCredits_Negative_ThrowsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.setAvailableCredits(-1));
    assertTrue(exception.getMessage().contains("negative"));
  }

  // ========== SET TOTAL ADVANCEMENTS TESTS (Deserialization) ==========

  @Test
  @DisplayName("Setting total advancements directly should update count")
  void testSetTotalAdvancementsCompleted_UpdatesCount() {
    data.setTotalAdvancementsCompleted(25);
    assertEquals(25, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Setting total advancements to zero should work")
  void testSetTotalAdvancementsCompleted_Zero_Success() {
    data.markAdvancementRewarded("adv1");
    data.setTotalAdvancementsCompleted(0);
    assertEquals(0, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Setting negative total advancements should throw exception")
  void testSetTotalAdvancementsCompleted_Negative_ThrowsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> data.setTotalAdvancementsCompleted(-1));
    assertTrue(exception.getMessage().contains("negative"));
  }

  // ========== COMBINED OPERATIONS TESTS ==========

  @Test
  @DisplayName("Credits and advancements should be independent")
  void testCreditsAndAdvancements_Independent() {
    data.addCredits(10);
    data.markAdvancementRewarded("adv1");

    assertEquals(10, data.getAvailableCredits());
    assertEquals(1, data.getTotalAdvancementsCompleted());

    data.spendCredits(5);
    assertEquals(5, data.getAvailableCredits());
    assertEquals(1, data.getTotalAdvancementsCompleted());
  }

  @Test
  @DisplayName("Complex workflow should maintain consistency")
  void testComplexWorkflow_MaintainsConsistency() {
    // Simulate gameplay progression
    data.addCredits(5);
    data.markAdvancementRewarded("minecraft:story/root");
    data.markAdvancementRewarded("minecraft:story/mine_stone");

    assertEquals(5, data.getAvailableCredits());
    assertEquals(2, data.getTotalAdvancementsCompleted());

    data.spendCredits(2);
    assertEquals(3, data.getAvailableCredits());

    data.addCredits(10);
    data.markAdvancementRewarded("minecraft:story/upgrade_tools");

    assertEquals(13, data.getAvailableCredits());
    assertEquals(3, data.getTotalAdvancementsCompleted());
  }

  // ========== EDGE CASES ==========

  @Test
  @DisplayName("Advancement IDs with special characters should work")
  void testAdvancementIds_SpecialCharacters_Success() {
    data.markAdvancementRewarded("mod:category/sub-category/achievement_name");
    data.markAdvancementRewarded("namespace:path/to/advancement");

    assertTrue(data.hasReceivedReward("mod:category/sub-category/achievement_name"));
    assertTrue(data.hasReceivedReward("namespace:path/to/advancement"));
  }

  @Test
  @DisplayName("Empty advancement ID should work")
  void testAdvancementId_Empty_Success() {
    data.markAdvancementRewarded("");
    assertTrue(data.hasReceivedReward(""));
  }

  @Test
  @DisplayName("toString should include relevant information")
  void testToString_ContainsRelevantInfo() {
    data.addCredits(5);
    data.markAdvancementRewarded("adv1");
    data.markAdvancementRewarded("adv2");

    String result = data.toString();
    assertTrue(result.contains("availableCredits=5"));
    assertTrue(result.contains("totalAdvancementsCompleted=2"));
    assertTrue(result.contains("rewardedAdvancementsCount=2"));
  }

  @Test
  @DisplayName("Large number of advancements should work")
  void testLargeNumberOfAdvancements_Success() {
    for (int i = 0; i < 1000; i++) {
      data.markAdvancementRewarded("advancement_" + i);
    }

    assertEquals(1000, data.getTotalAdvancementsCompleted());
    assertEquals(1000, data.getRewardedAdvancements().size());
    assertTrue(data.hasReceivedReward("advancement_500"));
  }
}
