package chunklocked.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigManager}.
 * <p>
 * Tests file I/O, error handling, and configuration lifecycle.
 */
class ConfigManagerTest {

  @TempDir
  Path tempDir;

  private ConfigManager configManager;
  private Path configFile;

  @BeforeEach
  void setUp() {
    configManager = new ConfigManager(tempDir);
    configFile = tempDir.resolve("chunk-locked-advancements.json");
  }

  @AfterEach
  void tearDown() throws IOException {
    // Clean up any created files
    if (Files.exists(configFile)) {
      Files.delete(configFile);
    }
  }

  @Test
  void testLoadConfig_FileDoesNotExist_CreatesDefaultConfig() {
    assertFalse(Files.exists(configFile), "Config file should not exist yet");

    AdvancementRewardConfig config = configManager.loadConfig();

    assertNotNull(config, "Config should not be null");
    assertEquals(1, config.getDefaultCredits(), "Should have default credits");
    assertTrue(Files.exists(configFile), "Config file should be created");
  }

  @Test
  void testLoadConfig_ValidFile_LoadsCorrectly() throws IOException {
    // Create a valid config file
    String validJson = """
        {
          "defaultCredits": 2,
          "customRewards": {
            "test:advancement": 10
          },
          "blacklist": ["test:blacklisted"],
          "allowRevoke": true,
          "enableNotifications": false
        }
        """;
    Files.writeString(configFile, validJson);

    AdvancementRewardConfig config = configManager.loadConfig();

    assertEquals(2, config.getDefaultCredits(), "Should load custom default credits");
    assertEquals(10, config.getCustomRewards().get("test:advancement"),
        "Should load custom rewards");
    assertTrue(config.getBlacklist().contains("test:blacklisted"),
        "Should load blacklist");
    assertTrue(config.isAllowRevoke(), "Should load allowRevoke");
    assertFalse(config.isEnableNotifications(), "Should load enableNotifications");
  }

  @Test
  void testLoadConfig_EmptyFile_UsesDefaults() throws IOException {
    // Create empty file
    Files.writeString(configFile, "");

    AdvancementRewardConfig config = configManager.loadConfig();

    assertNotNull(config, "Config should not be null");
    assertEquals(1, config.getDefaultCredits(), "Should use default credits");
  }

  @Test
  void testLoadConfig_InvalidJson_UsesDefaults() throws IOException {
    // Create file with invalid JSON
    Files.writeString(configFile, "{ this is not valid JSON }");

    AdvancementRewardConfig config = configManager.loadConfig();

    assertNotNull(config, "Config should not be null");
    assertEquals(1, config.getDefaultCredits(), "Should fall back to defaults");
  }

  @Test
  void testLoadConfig_InvalidValues_UsesDefaults() throws IOException {
    // Create file with invalid values (negative credits)
    String invalidJson = """
        {
          "defaultCredits": -5,
          "customRewards": {},
          "blacklist": [],
          "allowRevoke": false,
          "enableNotifications": true
        }
        """;
    Files.writeString(configFile, invalidJson);

    AdvancementRewardConfig config = configManager.loadConfig();

    assertNotNull(config, "Config should not be null");
    assertEquals(1, config.getDefaultCredits(), "Should fall back to defaults on validation failure");
  }

  @Test
  void testSaveConfig_CreatesValidJsonFile() throws IOException {
    AdvancementRewardConfig config = new AdvancementRewardConfig();
    config.setDefaultCredits(3);
    config.setCustomRewards(Map.of("test:advancement", 5));
    config.setBlacklist(List.of("test:blacklisted"));

    boolean success = configManager.saveConfig(config);

    assertTrue(success, "Save should succeed");
    assertTrue(Files.exists(configFile), "Config file should exist");

    // Verify file contents
    String json = Files.readString(configFile);
    assertTrue(json.contains("\"defaultCredits\": 3"), "Should contain defaultCredits");
    assertTrue(json.contains("test:advancement"), "Should contain custom rewards");
    assertTrue(json.contains("test:blacklisted"), "Should contain blacklist");
  }

  @Test
  void testSaveConfig_CreatesParentDirectory() throws IOException {
    // Use a nested directory that doesn't exist
    Path nestedDir = tempDir.resolve("nested/deep/path");
    ConfigManager nestedConfigManager = new ConfigManager(nestedDir);
    Path nestedConfigFile = nestedDir.resolve("chunk-locked-advancements.json");

    assertFalse(Files.exists(nestedDir), "Nested directory should not exist");

    AdvancementRewardConfig config = new AdvancementRewardConfig();
    boolean success = nestedConfigManager.saveConfig(config);

    assertTrue(success, "Save should succeed");
    assertTrue(Files.exists(nestedConfigFile), "Config file should exist in nested directory");
  }

  @Test
  void testSaveConfig_InvalidConfig_ReturnsFalse() {
    AdvancementRewardConfig config = new AdvancementRewardConfig();
    // Create config with invalid values that pass setter but fail validation
    Map<String, Integer> invalidRewards = new HashMap<>();
    invalidRewards.put("", 5); // Empty ID
    config.setCustomRewards(invalidRewards);

    boolean success = configManager.saveConfig(config);

    assertFalse(success, "Save should fail for invalid config");
  }

  @Test
  void testSaveAndLoad_RoundTrip() throws IOException {
    // Create custom config
    AdvancementRewardConfig originalConfig = new AdvancementRewardConfig();
    originalConfig.setDefaultCredits(5);
    originalConfig.setCustomRewards(Map.of(
        "test:advancement1", 10,
        "test:advancement2", 20));
    originalConfig.setBlacklist(List.of("test:blacklisted1", "test:blacklisted2"));
    originalConfig.setAllowRevoke(true);
    originalConfig.setEnableNotifications(false);

    // Save
    configManager.saveConfig(originalConfig);

    // Load
    AdvancementRewardConfig loadedConfig = configManager.loadConfig();

    // Verify all fields match
    assertEquals(originalConfig.getDefaultCredits(), loadedConfig.getDefaultCredits(),
        "Default credits should match");
    assertEquals(originalConfig.getCustomRewards().size(), loadedConfig.getCustomRewards().size(),
        "Custom rewards size should match");
    assertEquals(originalConfig.getBlacklist().size(), loadedConfig.getBlacklist().size(),
        "Blacklist size should match");
    assertEquals(originalConfig.isAllowRevoke(), loadedConfig.isAllowRevoke(),
        "Allow revoke should match");
    assertEquals(originalConfig.isEnableNotifications(), loadedConfig.isEnableNotifications(),
        "Enable notifications should match");
  }

  @Test
  void testReloadConfig_LoadsFreshData() throws IOException {
    // Initial load
    AdvancementRewardConfig initialConfig = configManager.loadConfig();
    assertEquals(1, initialConfig.getDefaultCredits());

    // Manually modify the config file
    String modifiedJson = """
        {
          "defaultCredits": 10,
          "customRewards": {},
          "blacklist": [],
          "allowRevoke": false,
          "enableNotifications": true
        }
        """;
    Files.writeString(configFile, modifiedJson);

    // Reload
    AdvancementRewardConfig reloadedConfig = configManager.reloadConfig();

    assertEquals(10, reloadedConfig.getDefaultCredits(),
        "Reload should load new values from file");
  }

  @Test
  void testGetCurrentConfig_AfterLoad_ReturnsLoadedConfig() {
    assertNull(configManager.getCurrentConfig(), "Should be null before loading");

    AdvancementRewardConfig config = configManager.loadConfig();

    assertSame(config, configManager.getCurrentConfig(),
        "getCurrentConfig should return the loaded config");
  }

  @Test
  void testGetOrLoadConfig_NoConfigLoaded_LoadsAutomatically() {
    assertNull(configManager.getCurrentConfig(), "Should be null initially");

    AdvancementRewardConfig config = configManager.getOrLoadConfig();

    assertNotNull(config, "getOrLoadConfig should auto-load");
    assertSame(config, configManager.getCurrentConfig(),
        "Should cache the loaded config");
  }

  @Test
  void testGetOrLoadConfig_ConfigAlreadyLoaded_ReturnsCached() {
    AdvancementRewardConfig firstLoad = configManager.loadConfig();
    AdvancementRewardConfig secondLoad = configManager.getOrLoadConfig();

    assertSame(firstLoad, secondLoad, "Should return cached config without reloading");
  }

  @Test
  void testGetConfigPath_ReturnsCorrectPath() {
    Path expectedPath = tempDir.resolve("chunk-locked-advancements.json");
    assertEquals(expectedPath, configManager.getConfigPath(),
        "Config path should match expected location");
  }

  @Test
  void testJsonFormatting_IsPrettyPrinted() throws IOException {
    AdvancementRewardConfig config = new AdvancementRewardConfig();
    configManager.saveConfig(config);

    String json = Files.readString(configFile);

    // Pretty-printed JSON should have indentation and newlines
    assertTrue(json.contains("\n"), "JSON should have newlines");
    assertTrue(json.contains("  "), "JSON should have indentation");
  }

  @Test
  void testPartialJson_MergesWithDefaults() throws IOException {
    // Create JSON with only some fields
    String partialJson = """
        {
          "defaultCredits": 5
        }
        """;
    Files.writeString(configFile, partialJson);

    AdvancementRewardConfig config = configManager.loadConfig();

    // Should have custom defaultCredits
    assertEquals(5, config.getDefaultCredits(), "Should use custom value from file");

    // Should have default values for missing fields
    assertNotNull(config.getCustomRewards(), "Should initialize customRewards");
    assertNotNull(config.getBlacklist(), "Should initialize blacklist");
    assertFalse(config.isAllowRevoke(), "Should use default allowRevoke");
    assertTrue(config.isEnableNotifications(), "Should use default enableNotifications");
  }

  @Test
  void testSaveConfig_UpdatesCurrentConfig() {
    AdvancementRewardConfig config = new AdvancementRewardConfig();
    config.setDefaultCredits(7);

    configManager.saveConfig(config);

    assertSame(config, configManager.getCurrentConfig(),
        "Save should update current config reference");
  }

  @Test
  void testAtomicWrite_UsesTemporaryFile() throws IOException {
    // This test verifies the atomic write behavior indirectly
    // by ensuring the final file exists and is valid after save
    AdvancementRewardConfig config = new AdvancementRewardConfig();

    boolean success = configManager.saveConfig(config);

    assertTrue(success, "Save should succeed");
    assertTrue(Files.exists(configFile), "Final config file should exist");

    // Ensure no .tmp file is left behind
    Path tempFile = configFile.getParent().resolve(configFile.getFileName() + ".tmp");
    assertFalse(Files.exists(tempFile), "Temporary file should be cleaned up");
  }
}
