package chunkloaded.advancement;

import chunkloaded.config.AdvancementRewardConfig;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages credit awards for advancement completions.
 * <p>
 * Responsibilities:
 * - Track player progression data (credits, completed advancements)
 * - Award credits when advancements are completed
 * - Prevent duplicate credit awards
 * - Apply custom reward multipliers from configuration
 * - Respect advancement blacklist from configuration
 * <p>
 * This class is not thread-safe. All methods should be called on the server
 * thread only.
 */
public class AdvancementCreditManager {
  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * Player progression data indexed by player UUID.
   * Persists across sessions via world data.
   */
  private final Map<UUID, PlayerProgressionData> playerData;

  /**
   * Custom credit rewards for specific advancements.
   * Maps advancement ID to credit amount.
   * Loaded from configuration.
   */
  private final Map<String, Integer> customRewards;

  /**
   * Advancements that should not grant any credits.
   * Loaded from configuration.
   */
  private final Set<String> blacklistedAdvancements;

  /**
   * Default number of credits awarded per advancement (if no custom reward
   * specified).
   * Loaded from configuration.
   */
  private int defaultCredits;

  /**
   * Whether to send notifications when credits are awarded.
   * Loaded from configuration.
   */
  private boolean enableNotifications;

  /**
   * Notification manager for sending player feedback.
   */
  private final NotificationManager notificationManager;

  /**
   * Creates a new advancement credit manager with default configuration.
   * <p>
   * Custom rewards and blacklist will be empty until loaded from configuration.
   */
  public AdvancementCreditManager() {
    this.playerData = new HashMap<>();
    this.customRewards = new HashMap<>();
    this.blacklistedAdvancements = new HashSet<>();
    this.defaultCredits = 1;
    this.enableNotifications = true;
    this.notificationManager = new NotificationManager();
  }

  /**
   * Handles an advancement completion event.
   * <p>
   * This method:
   * 1. Checks if the advancement is blacklisted
   * 2. Checks if the player has already received a reward for this advancement
   * 3. Looks up custom reward amount (or uses default)
   * 4. Awards credits to the player
   * 5. Marks the advancement as rewarded
   *
   * @param player      The player who completed the advancement
   * @param advancement The advancement that was completed
   */
  public void onAdvancementCompleted(ServerPlayer player, AdvancementHolder advancement, String criterionName) {
    String advancementId = advancement.id().toString();
    UUID playerId = player.getUUID();

    LOGGER.info("=== Processing advancement completion ===");
    LOGGER.info("  Advancement ID: {}", advancementId);
    LOGGER.info("  Player: {}", player.getName().getString());
    LOGGER.info("  Is Recipe: {}", isRecipeAdvancement(advancementId));
    LOGGER.info("  Is Blacklisted: {}", blacklistedAdvancements.contains(advancementId));

    // Skip recipe advancements (they're not "real" progression advancements)
    if (isRecipeAdvancement(advancementId)) {
      LOGGER.info("  RESULT: Skipping - This is a recipe advancement");
      return;
    }

    // Check blacklist first
    if (blacklistedAdvancements.contains(advancementId)) {
      LOGGER.info("  RESULT: Skipping - This advancement is blacklisted");
      return;
    }

    // Get or create player progression data
    PlayerProgressionData progression = getPlayerData(playerId);

    // Check if already rewarded
    if (progression.hasReceivedReward(advancementId)) {
      LOGGER.info("  RESULT: Skipping - Already rewarded to this player");
      return;
    }

    // Determine credit amount
    int creditsToAward = customRewards.getOrDefault(advancementId, defaultCredits);

    // Award credits
    progression.addCredits(creditsToAward);
    progression.markAdvancementRewarded(advancementId);

    LOGGER.info("  RESULT: ✓ AWARDED {} credit(s)!", creditsToAward);
    LOGGER.info("  Total credits for player: {}", progression.getAvailableCredits());
    LOGGER.info("==========================================");

    // Send notification to player
    notificationManager.notifyCreditAwarded(player, advancement, creditsToAward);
  }

  /**
   * Gets the progression data for a player by UUID.
   * <p>
   * Creates new progression data if none exists for this player.
   *
   * @param playerId The player's UUID
   * @return The player's progression data (never null)
   */
  public PlayerProgressionData getPlayerData(UUID playerId) {
    return playerData.computeIfAbsent(playerId, id -> new PlayerProgressionData());
  }

  /**
   * Gets the progression data for a player.
   * <p>
   * Convenience method that extracts the UUID from the player entity.
   *
   * @param player The player
   * @return The player's progression data (never null)
   */
  public PlayerProgressionData getPlayerData(ServerPlayer player) {
    return getPlayerData(player.getUUID());
  }

  /**
   * Checks if an advancement ID represents a recipe advancement.
   * <p>
   * Recipe advancements are automatically granted when players discover
   * items/blocks
   * and teach them crafting recipes. These should not award credits as they are
   * numerous and not true progression achievements.
   * <p>
   * Recipe advancements have IDs matching the pattern:
   * {@code namespace:recipes/...}
   * For example: {@code minecraft:recipes/building_blocks/stone_stairs}
   * <p>
   * Story advancements (like {@code minecraft:story/mine_stone}) will return
   * false.
   *
   * @param advancementId The advancement ID to check
   * @return true if this is a recipe advancement, false otherwise
   */
  private boolean isRecipeAdvancement(String advancementId) {
    // Recipe advancements have the exact pattern ":recipes/" after the namespace
    // Examples that SHOULD match:
    // - "minecraft:recipes/food/bread" ✓
    // - "create:recipes/tools/wrench" ✓
    // Examples that should NOT match:
    // - "minecraft:story/mine_stone" ✗
    // - "minecraft:nether/find_fortress" ✗

    if (advancementId == null || advancementId.isEmpty()) {
      return false;
    }

    return advancementId.contains(":recipes/");
  }

  /**
   * Sets a custom credit reward for a specific advancement.
   * <p>
   * Used when loading configuration.
   *
   * @param advancementId The advancement identifier
   * @param credits       The number of credits to award (must be >= 0)
   * @throws IllegalArgumentException if credits is negative
   */
  public void setCustomReward(String advancementId, int credits) {
    if (credits < 0) {
      throw new IllegalArgumentException("Custom reward cannot be negative: " + credits);
    }
    customRewards.put(advancementId, credits);
  }

  /**
   * Adds an advancement to the blacklist.
   * <p>
   * Blacklisted advancements will not grant any credits when completed.
   * Used when loading configuration.
   *
   * @param advancementId The advancement identifier to blacklist
   */
  public void addBlacklistedAdvancement(String advancementId) {
    blacklistedAdvancements.add(advancementId);
  }

  /**
   * Clears all custom rewards.
   * <p>
   * Used when reloading configuration.
   */
  public void clearCustomRewards() {
    customRewards.clear();
  }

  /**
   * Clears the advancement blacklist.
   * <p>
   * Used when reloading configuration.
   */
  public void clearBlacklist() {
    blacklistedAdvancements.clear();
  }

  /**
   * Loads configuration values and updates internal state.
   * <p>
   * This should be called:
   * - During mod initialization after config is loaded
   * - When config is reloaded at runtime
   * <p>
   * This method clears existing custom rewards and blacklist,
   * then loads the new values from the provided configuration.
   *
   * @param config The configuration to load
   */
  public void loadConfig(AdvancementRewardConfig config) {
    LOGGER.info("Loading advancement reward configuration");

    // Clear existing config
    clearCustomRewards();
    clearBlacklist();

    // Load default credits
    this.defaultCredits = config.getDefaultCredits();
    this.enableNotifications = config.isEnableNotifications();

    // Update notification manager
    notificationManager.setNotificationsEnabled(config.isEnableNotifications());

    // Load custom rewards
    for (Map.Entry<String, Integer> entry : config.getCustomRewards().entrySet()) {
      setCustomReward(entry.getKey(), entry.getValue());
    }

    // Load blacklist
    for (String advancementId : config.getBlacklist()) {
      addBlacklistedAdvancement(advancementId);
    }

    LOGGER.info("Loaded {} custom rewards and {} blacklisted advancements",
        customRewards.size(), blacklistedAdvancements.size());
    LOGGER.debug("Default credits: {}, Notifications enabled: {}",
        defaultCredits, enableNotifications);
  }

  /**
   * Gets all player data for serialization.
   * <p>
   * Used for saving to world data.
   *
   * @return Unmodifiable view of all player progression data
   */
  public Map<UUID, PlayerProgressionData> getAllPlayerData() {
    return Collections.unmodifiableMap(playerData);
  }

  /**
   * Loads player data from deserialization.
   * <p>
   * Used when loading from world data.
   *
   * @param playerId The player's UUID
   * @param data     The progression data to load
   */
  public void loadPlayerData(UUID playerId, PlayerProgressionData data) {
    playerData.put(playerId, data);
  }

  /**
   * Gets the number of players being tracked.
   *
   * @return The count of players with progression data
   */
  public int getTrackedPlayerCount() {
    return playerData.size();
  }

  /**
   * Gets the number of players being tracked.
   * Alias for getTrackedPlayerCount() for PersistentState compatibility.
   *
   * @return The count of players with progression data
   */
  public int getPlayerCount() {
    return playerData.size();
  }

  /**
   * Checks if an advancement is blacklisted.
   *
   * @param advancementId The advancement identifier
   * @return true if blacklisted, false otherwise
   */
  public boolean isBlacklisted(String advancementId) {
    return blacklistedAdvancements.contains(advancementId);
  }

  /**
   * Gets the credit amount that would be awarded for an advancement.
   * <p>
   * Returns 0 if the advancement is blacklisted.
   *
   * @param advancementId The advancement identifier
   * @return The number of credits that would be awarded
   */
  public int getCreditAmount(String advancementId) {
    if (blacklistedAdvancements.contains(advancementId)) {
      return 0;
    }
    return customRewards.getOrDefault(advancementId, defaultCredits);
  }

  /**
   * Gets the notification manager.
   *
   * @return The notification manager
   */
  public NotificationManager getNotificationManager() {
    return notificationManager;
  }
}
