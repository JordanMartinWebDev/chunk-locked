package chunkloaded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Manages loading, saving, and reloading of advancement reward configuration.
 * <p>
 * Handles JSON serialization/deserialization and provides graceful error
 * handling
 * for missing, corrupt, or invalid configuration files. Falls back to default
 * configuration when errors occur.
 * <p>
 * Configuration file location: {@code config/chunk-locked-advancements.json}
 * <p>
 * Thread Safety: All methods should be called from the server thread only.
 *
 * @see AdvancementRewardConfig
 */
public class ConfigManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

  /**
   * Configuration file path relative to the game directory.
   */
  private static final String CONFIG_FILE = "config/chunk-locked-advancements.json";

  /**
   * GSON instance for JSON serialization with pretty printing.
   */
  private final Gson gson;

  /**
   * Currently loaded configuration.
   * <p>
   * Should be replaced entirely on reload rather than mutated.
   */
  private AdvancementRewardConfig currentConfig;

  /**
   * Path to the configuration file.
   */
  private final Path configPath;

  /**
   * Creates a new configuration manager.
   * <p>
   * Does not automatically load configuration - call {@link #loadConfig()}
   * explicitly.
   *
   * @param configDirectory The base configuration directory (typically "config/")
   */
  public ConfigManager(Path configDirectory) {
    this.gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    this.configPath = configDirectory.resolve("chunk-locked-advancements.json");
  }

  /**
   * Loads the configuration from disk.
   * <p>
   * Behavior:
   * - If file doesn't exist: creates default config and saves it
   * - If file exists but is corrupt: logs error, uses default config
   * - If file exists and is valid: loads and validates it
   * <p>
   * This method should be called during mod initialization.
   *
   * @return The loaded (or default) configuration
   */
  public AdvancementRewardConfig loadConfig() {
    LOGGER.info("Loading advancement reward configuration from: {}", configPath);

    if (!Files.exists(configPath)) {
      LOGGER.info("Configuration file not found. Creating default configuration.");
      return createDefaultConfig();
    }

    try {
      String json = Files.readString(configPath);
      AdvancementRewardConfig config = gson.fromJson(json, AdvancementRewardConfig.class);

      if (config == null) {
        LOGGER.warn("Configuration file is empty or invalid. Using default configuration.");
        return createDefaultConfig();
      }

      // Validate loaded configuration
      try {
        config.validate();
        LOGGER.info("Configuration loaded successfully: {}", config);
        this.currentConfig = config;
        return config;
      } catch (IllegalStateException e) {
        LOGGER.error("Configuration validation failed: {}. Using default configuration.", e.getMessage());
        return createDefaultConfig();
      }

    } catch (IOException e) {
      LOGGER.error("Failed to read configuration file: {}. Using default configuration.", e.getMessage());
      return createDefaultConfig();
    } catch (JsonSyntaxException e) {
      LOGGER.error("Configuration file has invalid JSON syntax: {}. Using default configuration.", e.getMessage());
      return createDefaultConfig();
    }
  }

  /**
   * Saves the current configuration to disk.
   * <p>
   * Creates parent directories if they don't exist.
   * Uses atomic write via temp file to prevent corruption.
   *
   * @param config The configuration to save
   * @return true if save succeeded, false otherwise
   */
  public boolean saveConfig(AdvancementRewardConfig config) {
    LOGGER.info("Saving configuration to: {}", configPath);

    try {
      // Validate before saving
      config.validate();

      // Ensure parent directory exists
      Files.createDirectories(configPath.getParent());

      // Serialize to JSON
      String json = gson.toJson(config);

      // Write atomically (write to temp, then move)
      Path tempPath = configPath.getParent().resolve(configPath.getFileName() + ".tmp");
      Files.writeString(tempPath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      Files.move(tempPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      LOGGER.info("Configuration saved successfully.");
      this.currentConfig = config;
      return true;

    } catch (IllegalStateException e) {
      LOGGER.error("Cannot save invalid configuration: {}", e.getMessage());
      return false;
    } catch (IOException e) {
      LOGGER.error("Failed to write configuration file: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Creates and saves a default configuration file.
   * <p>
   * Used when:
   * - Configuration file doesn't exist (first run)
   * - Configuration file is corrupt beyond repair
   *
   * @return A new default configuration instance
   */
  private AdvancementRewardConfig createDefaultConfig() {
    LOGGER.info("Creating default configuration");
    AdvancementRewardConfig config = new AdvancementRewardConfig();

    // Try to save the default config, but don't fail if it errors
    if (saveConfig(config)) {
      LOGGER.info("Default configuration file created at: {}", configPath);
    } else {
      LOGGER.warn("Could not write default configuration file. Using in-memory defaults.");
    }

    this.currentConfig = config;
    return config;
  }

  /**
   * Reloads the configuration from disk.
   * <p>
   * This can be called at runtime (e.g., via a command) to apply
   * configuration changes without restarting the server.
   * <p>
   * Callers should notify dependent systems (like AdvancementCreditManager)
   * to refresh their cached configuration values.
   *
   * @return The newly loaded configuration
   */
  public AdvancementRewardConfig reloadConfig() {
    LOGGER.info("Reloading configuration");
    AdvancementRewardConfig config = loadConfig();
    this.currentConfig = config;
    return config;
  }

  /**
   * Gets the currently loaded configuration.
   * <p>
   * If no configuration has been loaded yet, this will return null.
   * Callers should call {@link #loadConfig()} during initialization.
   *
   * @return The current configuration, or null if not loaded
   */
  public AdvancementRewardConfig getCurrentConfig() {
    return currentConfig;
  }

  /**
   * Gets or creates the configuration.
   * <p>
   * Convenience method that ensures a configuration always exists.
   * If no configuration has been loaded, loads it automatically.
   *
   * @return The current configuration (never null)
   */
  public AdvancementRewardConfig getOrLoadConfig() {
    if (currentConfig == null) {
      return loadConfig();
    }
    return currentConfig;
  }

  /**
   * Gets the path to the configuration file.
   *
   * @return The configuration file path
   */
  public Path getConfigPath() {
    return configPath;
  }
}
