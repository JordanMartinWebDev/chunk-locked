package chunklocked.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AdvancementRewardConfig}.
 * <p>
 * Tests initialization, validation, and getter/setter behavior.
 */
class AdvancementRewardConfigTest {

  private AdvancementRewardConfig config;

  @BeforeEach
  void setUp() {
    config = new AdvancementRewardConfig();
  }

  @Test
  void testDefaultValues_InitializedCorrectly() {
    assertEquals(1, config.getDefaultCredits(), "Default credits should be 1");
    assertFalse(config.isAllowRevoke(), "Allow revoke should default to false");
    assertTrue(config.isEnableNotifications(), "Notifications should be enabled by default");

    assertNotNull(config.getCustomRewards(), "Custom rewards map should not be null");
    assertFalse(config.getCustomRewards().isEmpty(), "Custom rewards should have default entries");

    assertNotNull(config.getBlacklist(), "Blacklist should not be null");
    assertFalse(config.getBlacklist().isEmpty(), "Blacklist should have default entries");
  }

  @Test
  void testDefaultCustomRewards_ContainExpectedValues() {
    Map<String, Integer> rewards = config.getCustomRewards();

    // Check for some expected high-value advancements
    assertTrue(rewards.containsKey("minecraft:adventure/adventuring_time"),
        "Should have adventuring_time advancement");
    assertEquals(20, rewards.get("minecraft:adventure/adventuring_time"),
        "Adventuring time should award 20 credits");

    assertTrue(rewards.containsKey("minecraft:end/dragon_egg"),
        "Should have dragon_egg advancement");
    assertEquals(10, rewards.get("minecraft:end/dragon_egg"),
        "Dragon egg should award 10 credits");
  }

  @Test
  void testDefaultBlacklist_ContainsTrivialAdvancements() {
    List<String> blacklist = config.getBlacklist();

    assertTrue(blacklist.contains("minecraft:story/root"),
        "Should blacklist tutorial root advancement");
    assertTrue(blacklist.contains("minecraft:story/mine_stone"),
        "Should blacklist trivial mine stone advancement");
  }

  @Test
  void testValidate_DefaultConfigIsValid() {
    assertTrue(config.validate(), "Default configuration should be valid");
  }

  @Test
  void testValidate_ValidCustomConfig() {
    config.setDefaultCredits(2);
    config.setCustomRewards(Map.of("test:advancement", 5));
    config.setBlacklist(List.of("test:blacklisted"));

    assertTrue(config.validate(), "Valid custom configuration should pass validation");
  }

  @Test
  void testValidate_NegativeDefaultCredits_ThrowsException() {
    config.setDefaultCredits(0); // Should be valid
    assertTrue(config.validate(), "Zero credits should be valid");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.setDefaultCredits(-1);
    });

    assertTrue(exception.getMessage().contains("cannot be negative"),
        "Error message should mention negative value");
  }

  @Test
  void testValidate_NegativeCustomReward_ThrowsException() {
    Map<String, Integer> invalidRewards = new HashMap<>();
    invalidRewards.put("test:advancement", -5);
    config.setCustomRewards(invalidRewards);

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      config.validate();
    });

    assertTrue(exception.getMessage().contains("cannot be negative"),
        "Error message should mention negative value");
  }

  @Test
  void testValidate_NullCustomRewards_ThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.setCustomRewards(null);
    });

    assertTrue(exception.getMessage().contains("cannot be null"),
        "Error message should mention null value");
  }

  @Test
  void testValidate_NullBlacklist_ThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.setBlacklist(null);
    });

    assertTrue(exception.getMessage().contains("cannot be null"),
        "Error message should mention null value");
  }

  @Test
  void testValidate_EmptyAdvancementIdInCustomRewards_ThrowsException() {
    Map<String, Integer> invalidRewards = new HashMap<>();
    invalidRewards.put("", 5);
    config.setCustomRewards(invalidRewards);

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      config.validate();
    });

    assertTrue(exception.getMessage().contains("empty"),
        "Error message should mention empty ID");
  }

  @Test
  void testValidate_EmptyAdvancementIdInBlacklist_ThrowsException() {
    config.setBlacklist(List.of("valid:id", ""));

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      config.validate();
    });

    assertTrue(exception.getMessage().contains("empty"),
        "Error message should mention empty ID");
  }

  @Test
  void testSetDefaultCredits_ValidValue_UpdatesField() {
    config.setDefaultCredits(5);
    assertEquals(5, config.getDefaultCredits(), "Default credits should be updated");
  }

  @Test
  void testSetDefaultCredits_NegativeValue_ThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      config.setDefaultCredits(-1);
    });

    assertTrue(exception.getMessage().contains("negative"),
        "Error message should mention negative value");
  }

  @Test
  void testSetCustomRewards_CreatesDefensiveCopy() {
    Map<String, Integer> originalMap = new HashMap<>();
    originalMap.put("test:advancement", 5);

    config.setCustomRewards(originalMap);

    // Modify original map
    originalMap.put("test:advancement", 10);

    // Config should still have original value
    assertEquals(5, config.getCustomRewards().get("test:advancement"),
        "Config should have defensive copy, not affected by external changes");
  }

  @Test
  void testGetCustomRewards_ReturnsUnmodifiableMap() {
    Map<String, Integer> rewards = config.getCustomRewards();

    assertThrows(UnsupportedOperationException.class, () -> {
      rewards.put("test:new", 5);
    }, "Returned map should be unmodifiable");
  }

  @Test
  void testSetBlacklist_CreatesDefensiveCopy() {
    List<String> originalList = new ArrayList<>();
    originalList.add("test:advancement");

    config.setBlacklist(originalList);

    // Modify original list
    originalList.add("test:another");

    // Config should still have original value
    assertEquals(1, config.getBlacklist().size(),
        "Config should have defensive copy, not affected by external changes");
  }

  @Test
  void testGetBlacklist_ReturnsUnmodifiableList() {
    List<String> blacklist = config.getBlacklist();

    assertThrows(UnsupportedOperationException.class, () -> {
      blacklist.add("test:new");
    }, "Returned list should be unmodifiable");
  }

  @Test
  void testSetAllowRevoke_UpdatesField() {
    config.setAllowRevoke(true);
    assertTrue(config.isAllowRevoke(), "Allow revoke should be updated");

    config.setAllowRevoke(false);
    assertFalse(config.isAllowRevoke(), "Allow revoke should be updated");
  }

  @Test
  void testSetEnableNotifications_UpdatesField() {
    config.setEnableNotifications(false);
    assertFalse(config.isEnableNotifications(), "Enable notifications should be updated");

    config.setEnableNotifications(true);
    assertTrue(config.isEnableNotifications(), "Enable notifications should be updated");
  }

  @Test
  void testToString_ContainsKeyInformation() {
    String str = config.toString();

    assertTrue(str.contains("defaultCredits"), "toString should include defaultCredits");
    assertTrue(str.contains("customRewards"), "toString should include customRewards");
    assertTrue(str.contains("blacklist"), "toString should include blacklist");
    assertTrue(str.contains("allowRevoke"), "toString should include allowRevoke");
    assertTrue(str.contains("enableNotifications"), "toString should include enableNotifications");
  }

  @Test
  void testCustomRewards_AllValuesNonNegative() {
    Map<String, Integer> rewards = config.getCustomRewards();

    for (Map.Entry<String, Integer> entry : rewards.entrySet()) {
      assertTrue(entry.getValue() >= 0,
          "Custom reward for " + entry.getKey() + " should be non-negative");
    }
  }

  @Test
  void testBlacklist_AllIdsNonEmpty() {
    List<String> blacklist = config.getBlacklist();

    for (String id : blacklist) {
      assertNotNull(id, "Blacklist entry should not be null");
      assertFalse(id.isEmpty(), "Blacklist entry should not be empty");
    }
  }
}
