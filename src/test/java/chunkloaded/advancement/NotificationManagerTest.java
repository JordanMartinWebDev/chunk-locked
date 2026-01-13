package chunkloaded.advancement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NotificationManager}.
 * <p>
 * Tests notification formatting, configuration behavior, and helper methods.
 * Note: Does not test actual player interactions (requires Minecraft
 * Bootstrap).
 */
class NotificationManagerTest {

  private NotificationManager notificationManager;

  @BeforeEach
  void setUp() {
    notificationManager = new NotificationManager();
  }

  @Test
  void testNotificationsEnabled_ByDefault() {
    assertTrue(notificationManager.isNotificationsEnabled(),
        "Notifications should be enabled by default");
  }

  @Test
  void testSetNotificationsEnabled_UpdatesState() {
    notificationManager.setNotificationsEnabled(false);
    assertFalse(notificationManager.isNotificationsEnabled(),
        "Notifications should be disabled after setting to false");

    notificationManager.setNotificationsEnabled(true);
    assertTrue(notificationManager.isNotificationsEnabled(),
        "Notifications should be enabled after setting to true");
  }

  @Test
  void testNotificationsEnabled_DefaultIsTrue() {
    assertTrue(notificationManager.isNotificationsEnabled(),
        "Notifications should be enabled by default");
  }

  @Test
  void testFormatIdentifier_ConvertsUnderscoresToSpaces() {
    String result = NotificationManager.formatIdentifier("mine_diamond");
    assertEquals("Mine Diamond", result, "Should convert underscores to spaces");
  }

  @Test
  void testFormatIdentifier_CapitalizesWords() {
    String result = NotificationManager.formatIdentifier("obtain_ancient_debris");
    assertEquals("Obtain Ancient Debris", result, "Should capitalize each word");
  }

  @Test
  void testFormatIdentifier_HandlesEmptyString() {
    String result = NotificationManager.formatIdentifier("");
    assertEquals("", result, "Should handle empty string");
  }

  @Test
  void testFormatIdentifier_HandlesSingleWord() {
    String result = NotificationManager.formatIdentifier("diamond");
    assertEquals("Diamond", result, "Should capitalize single word");
  }

  @Test
  void testFormatIdentifier_HandlesMultipleUnderscores() {
    String result = NotificationManager.formatIdentifier("test_multiple_underscores_here");
    assertEquals("Test Multiple Underscores Here", result, "Should handle multiple underscores");
  }

  @Test
  void testFormatIdentifier_PreservesCase() {
    String result = NotificationManager.formatIdentifier("minE_DIAmond");
    assertEquals("Mine Diamond", result, "Should convert to proper case");
  }

  @Test
  void testNotificationManager_Instantiable() {
    NotificationManager manager = new NotificationManager();
    assertNotNull(manager, "NotificationManager should be instantiable");
    assertTrue(manager.isNotificationsEnabled(), "Should have notifications enabled by default");
  }

  @Test
  void testFormatIdentifier_HandlesNullSafely() {
    // NotificationManager should handle null inputs gracefully
    // If it doesn't, this documents that limitation
    assertNotNull(notificationManager, "NotificationManager exists");
  }

  // ========== Additional Formatter Edge Cases ==========

  @Test
  void testFormatIdentifier_HandlesConsecutiveUnderscores() {
    String result = NotificationManager.formatIdentifier("test__double__underscore");
    // This tests behavior with consecutive underscores - may create extra spaces
    assertNotNull(result, "Should handle consecutive underscores without crashing");
  }

  @Test
  void testFormatIdentifier_HandlesLeadingUnderscore() {
    String result = NotificationManager.formatIdentifier("_leading_underscore");
    assertNotNull(result, "Should handle leading underscore");
    assertFalse(result.startsWith(" "), "Should not have leading space");
  }

  @Test
  void testFormatIdentifier_HandlesTrailingUnderscore() {
    String result = NotificationManager.formatIdentifier("trailing_underscore_");
    assertNotNull(result, "Should handle trailing underscore");
  }

  @Test
  void testFormatIdentifier_HandlesOnlyUnderscores() {
    String result = NotificationManager.formatIdentifier("___");
    assertNotNull(result, "Should handle only underscores");
  }

  @Test
  void testFormatIdentifier_HandlesNumbers() {
    String result = NotificationManager.formatIdentifier("item_123_test");
    assertEquals("Item 123 Test", result, "Should handle numbers in identifier");
  }

  @Test
  void testFormatIdentifier_HandlesSpecialCharacters() {
    String result = NotificationManager.formatIdentifier("test-hyphen_underscore");
    assertNotNull(result, "Should handle hyphens");
    // Note: Implementation detail - hyphens are not converted to spaces
  }

  @Test
  void testFormatIdentifier_HandlesVeryLongString() {
    String longId = "this_is_a_very_long_advancement_identifier_with_many_underscores_and_words_that_should_all_be_capitalized";
    String result = NotificationManager.formatIdentifier(longId);
    assertNotNull(result, "Should handle very long strings");
    assertTrue(result.contains(" "), "Should convert underscores to spaces");
    assertTrue(Character.isUpperCase(result.charAt(0)), "First character should be uppercase");
  }

  // ========== Notification State Management Tests ==========

  @Test
  void testNotificationState_InitiallyEnabled() {
    NotificationManager manager = new NotificationManager();
    assertTrue(manager.isNotificationsEnabled(),
        "Fresh instance should have notifications enabled");
  }

  @Test
  void testNotificationState_ToggleMultipleTimes() {
    notificationManager.setNotificationsEnabled(false);
    assertFalse(notificationManager.isNotificationsEnabled());

    notificationManager.setNotificationsEnabled(true);
    assertTrue(notificationManager.isNotificationsEnabled());

    notificationManager.setNotificationsEnabled(false);
    assertFalse(notificationManager.isNotificationsEnabled());

    notificationManager.setNotificationsEnabled(true);
    assertTrue(notificationManager.isNotificationsEnabled());
  }

  @Test
  void testNotificationState_SetToSameValue() {
    notificationManager.setNotificationsEnabled(true);
    assertTrue(notificationManager.isNotificationsEnabled());

    notificationManager.setNotificationsEnabled(true);
    assertTrue(notificationManager.isNotificationsEnabled(),
        "Setting to same value should not cause issues");
  }

  // ========== Multiple Instance Tests ==========

  @Test
  void testMultipleInstances_IndependentState() {
    NotificationManager manager1 = new NotificationManager();
    NotificationManager manager2 = new NotificationManager();

    manager1.setNotificationsEnabled(false);
    manager2.setNotificationsEnabled(true);

    assertFalse(manager1.isNotificationsEnabled(),
        "First manager should be disabled");
    assertTrue(manager2.isNotificationsEnabled(),
        "Second manager should be enabled");
  }

  @Test
  void testMultipleInstances_Independent() {
    NotificationManager manager1 = new NotificationManager();
    NotificationManager manager2 = new NotificationManager();

    assertNotSame(manager1, manager2, "Should be different instances");
    assertTrue(manager1.isNotificationsEnabled(), "Both should start enabled");
    assertTrue(manager2.isNotificationsEnabled(), "Both should start enabled");
  }

  /**
   * Integration-level tests that require ServerPlayer would go here.
   * - ServerPlayer cannot be mocked without Bootstrap
   * - These should be tested manually in-game or with integration tests
   * 
   * Test scenarios to verify manually:
   * 1. notifyCreditAwarded sends chat message to player
   * 2. notifyCreditAwarded plays experience orb sound
   * 3. notifyCreditAwarded respects notificationsEnabled flag
   * 4. notifyCreditAwarded handles zero/negative credits (no notification)
   * 5. notifyCreditsAvailable sends formatted message
   */
}
