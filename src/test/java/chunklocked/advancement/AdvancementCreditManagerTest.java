package chunklocked.advancement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AdvancementCreditManager}.
 * <p>
 * Tests business logic including recipe advancement filtering,
 * blacklisting, and custom reward configuration.
 */
@DisplayName("AdvancementCreditManager Tests")
class AdvancementCreditManagerTest {

    private AdvancementCreditManager manager;

    @BeforeEach
    void setUp() {
        manager = new AdvancementCreditManager();
    }

    // ========== Recipe Advancement Filtering Tests ==========

    @Test
    @DisplayName("Should identify vanilla recipe advancements")
    void testRecipeAdvancement_Vanilla() {
        // Recipe advancements from vanilla Minecraft
        assertTrue(isRecipeAdvancement("minecraft:recipes/building_blocks/stone_stairs"),
                "Vanilla recipe advancement should be detected");
        assertTrue(isRecipeAdvancement("minecraft:recipes/food/bread"),
                "Vanilla food recipe should be detected");
        assertTrue(isRecipeAdvancement("minecraft:recipes/tools/iron_pickaxe"),
                "Vanilla tool recipe should be detected");
    }

    @Test
    @DisplayName("Should identify modded recipe advancements")
    void testRecipeAdvancement_Modded() {
        assertTrue(isRecipeAdvancement("create:recipes/crafting/materials/copper_ingot"),
                "Modded recipe advancement should be detected");
        assertTrue(isRecipeAdvancement("ae2:recipes/tools/network_tool"),
                "Another modded recipe should be detected");
    }

    @Test
    @DisplayName("Should NOT identify regular advancements as recipes")
    void testNotRecipeAdvancement_Regular() {
        // Regular story/progression advancements
        assertFalse(isRecipeAdvancement("minecraft:story/mine_stone"),
                "Story advancement should not be recipe");
        assertFalse(isRecipeAdvancement("minecraft:nether/obtain_ancient_debris"),
                "Nether advancement should not be recipe");
        assertFalse(isRecipeAdvancement("minecraft:adventure/adventuring_time"),
                "Adventure advancement should not be recipe");
        assertFalse(isRecipeAdvancement("minecraft:husbandry/breed_all_animals"),
                "Husbandry advancement should not be recipe");
    }

    @Test
    @DisplayName("Should handle edge cases correctly")
    void testRecipeAdvancement_EdgeCases() {
        // Edge case: advancement with "recipe" in the name but not in recipes/ path
        assertFalse(isRecipeAdvancement("minecraft:story/follow_the_recipe"),
                "Non-recipe path containing 'recipe' should not match");

        // Edge case: empty or malformed IDs
        assertFalse(isRecipeAdvancement(""),
                "Empty string should not be recipe");
        assertFalse(isRecipeAdvancement("minecraft"),
                "ID without namespace separator should not be recipe");

        // Edge case: technically matches pattern but won't occur in real Minecraft
        assertTrue(isRecipeAdvancement(":recipes/"),
                "Malformed ID with recipes pattern still matches (acceptable edge case)");
    }

    // ========== Helper Method to Access Private Method ==========

    /**
     * Helper method to test the private isRecipeAdvancement method.
     * Uses reflection to access the private method for testing.
     */
    private boolean isRecipeAdvancement(String advancementId) {
        try {
            var method = AdvancementCreditManager.class.getDeclaredMethod(
                    "isRecipeAdvancement", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(manager, advancementId);
        } catch (Exception e) {
            fail("Failed to invoke isRecipeAdvancement: " + e.getMessage());
            return false;
        }
    }

    // ========== Configuration Tests ==========

    @Test
    @DisplayName("Should add advancement to blacklist")
    void testBlacklist() {
        manager.addBlacklistedAdvancement("minecraft:story/iron_tools");
        assertTrue(manager.isBlacklisted("minecraft:story/iron_tools"),
                "Blacklisted advancement should be detected");
    }

    @Test
    @DisplayName("Should not blacklist non-blacklisted advancements")
    void testNotBlacklisted() {
        assertFalse(manager.isBlacklisted("minecraft:story/mine_diamond"),
                "Non-blacklisted advancement should return false");
    }

    @Test
    @DisplayName("Should set and retrieve custom reward amounts")
    void testCustomReward() {
        manager.setCustomReward("minecraft:end/kill_dragon", 10);
        assertEquals(10, manager.getCreditAmount("minecraft:end/kill_dragon"),
                "Custom reward should be retrievable");
    }

    @Test
    @DisplayName("Should return default credits for non-custom advancements")
    void testDefaultCredits() {
        assertEquals(1, manager.getCreditAmount("minecraft:story/mine_stone"),
                "Non-custom advancement should use default credits");
    }

    @Test
    @DisplayName("Should reject negative custom rewards")
    void testNegativeReward() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.setCustomReward("minecraft:test", -5),
                "Negative rewards should throw exception");
    }

    @Test
    @DisplayName("Should accept zero credits as valid")
    void testZeroCredits() {
        assertDoesNotThrow(
                () -> manager.setCustomReward("minecraft:test", 0),
                "Zero credits should be valid");
        assertEquals(0, manager.getCreditAmount("minecraft:test"),
                "Zero credits should be stored correctly");
    }

    // ========== Blacklist Management Tests ==========

    @Test
    @DisplayName("Should remove advancement from blacklist when cleared")
    void testClearBlacklist() {
        manager.addBlacklistedAdvancement("minecraft:story/iron_tools");
        manager.clearBlacklist();
        assertFalse(manager.isBlacklisted("minecraft:story/iron_tools"),
                "Cleared blacklist should not contain previously blacklisted advancement");
    }

    @Test
    @DisplayName("Should handle multiple blacklisted advancements")
    void testMultipleBlacklistedAdvancements() {
        manager.addBlacklistedAdvancement("minecraft:story/iron_tools");
        manager.addBlacklistedAdvancement("minecraft:nether/find_fortress");
        manager.addBlacklistedAdvancement("minecraft:end/kill_dragon");

        assertTrue(manager.isBlacklisted("minecraft:story/iron_tools"));
        assertTrue(manager.isBlacklisted("minecraft:nether/find_fortress"));
        assertTrue(manager.isBlacklisted("minecraft:end/kill_dragon"));
    }

    @Test
    @DisplayName("Should return zero credits for blacklisted advancements")
    void testBlacklistedAdvancement_ReturnsZeroCredits() {
        manager.setCustomReward("minecraft:story/mine_diamond", 5);
        manager.addBlacklistedAdvancement("minecraft:story/mine_diamond");

        assertEquals(0, manager.getCreditAmount("minecraft:story/mine_diamond"),
                "Blacklisted advancement should return zero credits even if custom reward exists");
    }

    @Test
    @DisplayName("Should handle duplicate blacklist entries")
    void testDuplicateBlacklistEntries() {
        manager.addBlacklistedAdvancement("minecraft:story/iron_tools");
        manager.addBlacklistedAdvancement("minecraft:story/iron_tools");

        assertTrue(manager.isBlacklisted("minecraft:story/iron_tools"),
                "Duplicate blacklist entries should not cause issues");
    }

    // ========== Custom Reward Management Tests ==========

    @Test
    @DisplayName("Should clear custom rewards")
    void testClearCustomRewards() {
        manager.setCustomReward("minecraft:end/kill_dragon", 10);
        manager.setCustomReward("minecraft:nether/obtain_ancient_debris", 5);

        manager.clearCustomRewards();

        assertEquals(1, manager.getCreditAmount("minecraft:end/kill_dragon"),
                "After clearing, should return default credits");
        assertEquals(1, manager.getCreditAmount("minecraft:nether/obtain_ancient_debris"),
                "After clearing, should return default credits");
    }

    @Test
    @DisplayName("Should handle very large custom reward values")
    void testLargeCustomReward() {
        manager.setCustomReward("minecraft:test", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, manager.getCreditAmount("minecraft:test"),
                "Should handle Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("Should override existing custom rewards")
    void testOverrideCustomReward() {
        manager.setCustomReward("minecraft:end/kill_dragon", 10);
        manager.setCustomReward("minecraft:end/kill_dragon", 20);

        assertEquals(20, manager.getCreditAmount("minecraft:end/kill_dragon"),
                "Second setCustomReward should override the first");
    }

    @Test
    @DisplayName("Should handle multiple custom rewards")
    void testMultipleCustomRewards() {
        manager.setCustomReward("minecraft:end/kill_dragon", 10);
        manager.setCustomReward("minecraft:nether/obtain_ancient_debris", 5);
        manager.setCustomReward("minecraft:story/mine_diamond", 2);

        assertEquals(10, manager.getCreditAmount("minecraft:end/kill_dragon"));
        assertEquals(5, manager.getCreditAmount("minecraft:nether/obtain_ancient_debris"));
        assertEquals(2, manager.getCreditAmount("minecraft:story/mine_diamond"));
    }

    // ========== Player Data Management Tests ==========

    @Test
    @DisplayName("Should create player data on first access")
    void testGetPlayerData_CreatesNewData() {
        var uuid = java.util.UUID.randomUUID();
        var data = manager.getPlayerData(uuid);

        assertNotNull(data, "Should create new player data");
        assertEquals(0, data.getAvailableCredits(), "New player should start with zero credits");
    }

    @Test
    @DisplayName("Should return same player data on multiple accesses")
    void testGetPlayerData_ReturnsSameInstance() {
        var uuid = java.util.UUID.randomUUID();
        var data1 = manager.getPlayerData(uuid);
        var data2 = manager.getPlayerData(uuid);

        assertSame(data1, data2, "Should return the same instance");
    }

    @Test
    @DisplayName("Should track multiple players")
    void testMultiplePlayers() {
        var uuid1 = java.util.UUID.randomUUID();
        var uuid2 = java.util.UUID.randomUUID();
        var uuid3 = java.util.UUID.randomUUID();

        var data1 = manager.getPlayerData(uuid1);
        var data2 = manager.getPlayerData(uuid2);
        var data3 = manager.getPlayerData(uuid3);

        assertNotSame(data1, data2, "Different players should have different data");
        assertNotSame(data2, data3, "Different players should have different data");
        assertEquals(3, manager.getTrackedPlayerCount(), "Should track three players");
    }

    @Test
    @DisplayName("Should load player data")
    void testLoadPlayerData() {
        var uuid = java.util.UUID.randomUUID();
        var data = new PlayerProgressionData();
        data.addCredits(10);

        manager.loadPlayerData(uuid, data);

        var retrieved = manager.getPlayerData(uuid);
        assertEquals(10, retrieved.getAvailableCredits(), "Loaded data should be retrievable");
    }

    @Test
    @DisplayName("Should return all player data as unmodifiable map")
    void testGetAllPlayerData() {
        var uuid1 = java.util.UUID.randomUUID();
        var uuid2 = java.util.UUID.randomUUID();

        manager.getPlayerData(uuid1).addCredits(5);
        manager.getPlayerData(uuid2).addCredits(10);

        var allData = manager.getAllPlayerData();

        assertEquals(2, allData.size(), "Should contain both players");
        assertTrue(allData.containsKey(uuid1), "Should contain first player");
        assertTrue(allData.containsKey(uuid2), "Should contain second player");

        // Verify it's unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> allData.put(java.util.UUID.randomUUID(), new PlayerProgressionData()),
                "Returned map should be unmodifiable");
    }

    @Test
    @DisplayName("Should report correct player count")
    void testGetPlayerCount() {
        assertEquals(0, manager.getPlayerCount(), "Should start with zero players");

        manager.getPlayerData(java.util.UUID.randomUUID());
        assertEquals(1, manager.getPlayerCount(), "Should increment to 1");

        manager.getPlayerData(java.util.UUID.randomUUID());
        manager.getPlayerData(java.util.UUID.randomUUID());
        assertEquals(3, manager.getPlayerCount(), "Should increment to 3");
    }

    @Test
    @DisplayName("getPlayerCount and getTrackedPlayerCount should return same value")
    void testPlayerCountAliases() {
        manager.getPlayerData(java.util.UUID.randomUUID());
        manager.getPlayerData(java.util.UUID.randomUUID());

        assertEquals(manager.getPlayerCount(), manager.getTrackedPlayerCount(),
                "Both methods should return the same value");
    }

    // ========== Recipe Advancement Edge Cases ==========

    @Test
    @DisplayName("Should handle null advancement ID")
    void testRecipeAdvancement_Null() {
        assertFalse(isRecipeAdvancement(null),
                "Null advancement ID should not crash and return false");
    }

    @Test
    @DisplayName("Should handle advancement ID without colon")
    void testRecipeAdvancement_NoColon() {
        assertFalse(isRecipeAdvancement("just_a_string"),
                "Advancement without namespace separator should return false");
    }

    @Test
    @DisplayName("Should handle advancement ID with multiple colons")
    void testRecipeAdvancement_MultipleColons() {
        assertTrue(isRecipeAdvancement("mod:name:recipes/item"),
                "Should still detect recipes pattern with multiple colons");
    }

    @Test
    @DisplayName("Should handle very long advancement IDs")
    void testRecipeAdvancement_LongId() {
        String longId = "minecraft:recipes/building_blocks/very/deep/nested/path/to/stone_stairs_variant_123";
        assertTrue(isRecipeAdvancement(longId),
                "Should handle very long recipe paths");
    }

    // ========== NotificationManager Integration Tests ==========

    @Test
    @DisplayName("Should have notification manager")
    void testHasNotificationManager() {
        assertNotNull(manager.getNotificationManager(),
                "Manager should have a notification manager");
    }

    @Test
    @DisplayName("Should enable/disable notifications through notification manager")
    void testNotificationManagerIntegration() {
        var notifManager = manager.getNotificationManager();
        assertTrue(notifManager.isNotificationsEnabled(),
                "Notifications should be enabled by default");

        notifManager.setNotificationsEnabled(false);
        assertFalse(notifManager.isNotificationsEnabled(),
                "Should be able to disable notifications");
    }

    // ========== Config Loading Tests ==========

    @Test
    @DisplayName("Should load config and apply settings")
    void testLoadConfig() {
        var config = new chunklocked.config.AdvancementRewardConfig();
        config.setDefaultCredits(5);
        config.setEnableNotifications(false);

        var customRewards = new java.util.HashMap<String, Integer>();
        customRewards.put("minecraft:end/kill_dragon", 20);
        customRewards.put("minecraft:nether/obtain_ancient_debris", 10);
        config.setCustomRewards(customRewards);

        var blacklist = java.util.Arrays.asList("minecraft:story/root", "minecraft:tutorial/craft_table");
        config.setBlacklist(blacklist);

        manager.loadConfig(config);

        // Verify default credits
        assertEquals(5, manager.getCreditAmount("minecraft:story/random_advancement"),
                "Default credits should be applied");

        // Verify custom rewards
        assertEquals(20, manager.getCreditAmount("minecraft:end/kill_dragon"),
                "Custom reward should be loaded");
        assertEquals(10, manager.getCreditAmount("minecraft:nether/obtain_ancient_debris"),
                "Custom reward should be loaded");

        // Verify blacklist
        assertTrue(manager.isBlacklisted("minecraft:story/root"),
                "Blacklist should be loaded");
        assertTrue(manager.isBlacklisted("minecraft:tutorial/craft_table"),
                "Blacklist should be loaded");
        assertEquals(0, manager.getCreditAmount("minecraft:story/root"),
                "Blacklisted advancement should return 0 credits");

        // Verify notifications
        assertFalse(manager.getNotificationManager().isNotificationsEnabled(),
                "Notifications setting should be applied");
    }

    @Test
    @DisplayName("Should clear previous config when loading new config")
    void testLoadConfig_ClearsPrevious() {
        // Set up initial config
        manager.setCustomReward("minecraft:old_advancement", 99);
        manager.addBlacklistedAdvancement("minecraft:old_blacklist");

        // Load new config (without those values)
        var config = new chunklocked.config.AdvancementRewardConfig();
        config.setDefaultCredits(3);
        config.setCustomRewards(new java.util.HashMap<>());
        config.setBlacklist(java.util.Collections.emptyList());

        manager.loadConfig(config);

        // Verify old values are cleared
        assertEquals(3, manager.getCreditAmount("minecraft:old_advancement"),
                "Old custom reward should be cleared");
        assertFalse(manager.isBlacklisted("minecraft:old_blacklist"),
                "Old blacklist entry should be cleared");
    }

    @Test
    @DisplayName("Should handle empty config")
    void testLoadConfig_Empty() {
        var config = new chunklocked.config.AdvancementRewardConfig();
        config.setDefaultCredits(1);
        config.setCustomRewards(new java.util.HashMap<>());
        config.setBlacklist(java.util.Collections.emptyList());

        assertDoesNotThrow(() -> manager.loadConfig(config),
                "Should handle empty config without errors");
    }

    @Test
    @DisplayName("Should handle config with large number of custom rewards")
    void testLoadConfig_LargeCustomRewards() {
        var config = new chunklocked.config.AdvancementRewardConfig();
        var customRewards = new java.util.HashMap<String, Integer>();

        // Add 100 custom rewards
        for (int i = 0; i < 100; i++) {
            customRewards.put("minecraft:advancement_" + i, i + 1);
        }

        config.setCustomRewards(customRewards);
        config.setBlacklist(java.util.Collections.emptyList());

        manager.loadConfig(config);

        // Verify a few entries
        assertEquals(1, manager.getCreditAmount("minecraft:advancement_0"));
        assertEquals(50, manager.getCreditAmount("minecraft:advancement_49"));
        assertEquals(100, manager.getCreditAmount("minecraft:advancement_99"));
    }

    @Test
    @DisplayName("Should handle config reload multiple times")
    void testLoadConfig_MultipleTimes() {
        var config1 = new chunklocked.config.AdvancementRewardConfig();
        config1.setDefaultCredits(5);
        manager.loadConfig(config1);
        assertEquals(5, manager.getCreditAmount("minecraft:test"));

        var config2 = new chunklocked.config.AdvancementRewardConfig();
        config2.setDefaultCredits(10);
        manager.loadConfig(config2);
        assertEquals(10, manager.getCreditAmount("minecraft:test"),
                "Second config load should override first");

        var config3 = new chunklocked.config.AdvancementRewardConfig();
        config3.setDefaultCredits(2);
        manager.loadConfig(config3);
        assertEquals(2, manager.getCreditAmount("minecraft:test"),
                "Third config load should override second");
    }
}
