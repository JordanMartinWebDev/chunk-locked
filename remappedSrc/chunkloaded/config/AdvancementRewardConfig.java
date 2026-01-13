package chunkloaded.config;

import java.util.*;

/**
 * Configuration data class for advancement reward system.
 * <p>
 * Holds all configurable values related to advancement credit rewards,
 * including default credit amounts, custom rewards for specific advancements,
 * blacklisted advancements, and feature toggles.
 * <p>
 * This class is designed to be serialized to/from JSON configuration files.
 * All fields have sensible defaults that provide balanced gameplay.
 * <p>
 * Thread Safety: Instances should only be accessed from the server thread.
 * Configuration reloads should replace the entire instance rather than
 * mutating fields.
 *
 * @see ConfigManager
 */
public class AdvancementRewardConfig {

  /**
   * Default number of credits awarded for completing any advancement
   * that doesn't have a custom reward configured.
   * <p>
   * Must be >= 0. Setting to 0 disables default rewards.
   */
  private int defaultCredits;

  /**
   * Custom credit rewards for specific advancements.
   * <p>
   * Maps advancement identifiers (as strings, e.g.,
   * "minecraft:story/mine_diamond")
   * to the number of credits that should be awarded.
   * <p>
   * Advancements not in this map will use the defaultCredits value.
   * Set a value to 0 to give no reward without blacklisting.
   */
  private Map<String, Integer> customRewards;

  /**
   * List of advancement identifiers that should never grant credits.
   * <p>
   * Useful for:
   * - Tutorial/trivial advancements
   * - Advancements that auto-complete
   * - Advancements deemed too easy for the progression system
   * <p>
   * Note: Blacklisted advancements take precedence over customRewards.
   */
  private List<String> blacklist;

  /**
   * Whether credits can be revoked if advancements are revoked.
   * <p>
   * Default: false (once earned, credits stay)
   * <p>
   * If true, revoking an advancement via commands will subtract the
   * credits that were awarded (if player has enough credits).
   * <p>
   * Note: Not yet implemented in Phase 3.
   */
  private boolean allowRevoke;

  /**
   * Whether to send chat notifications when credits are awarded.
   * <p>
   * Default: true
   * <p>
   * When enabled, players receive a message like:
   * "Achievement Unlocked! You earned 1 chunk unlock credit."
   */
  private boolean enableNotifications;

  /**
   * Creates a configuration with balanced default values.
   * <p>
   * Default values:
   * - defaultCredits: 1 (every advancement = 1 credit)
   * - customRewards: Bonus credits for major achievements
   * - blacklist: Common early-game advancements
   * - allowRevoke: false
   * - enableNotifications: true
   */
  public AdvancementRewardConfig() {
    this.defaultCredits = 1;
    this.customRewards = createDefaultCustomRewards();
    this.blacklist = createDefaultBlacklist();
    this.allowRevoke = false;
    this.enableNotifications = true;
  }

  /**
   * Creates the default custom reward mappings.
   * <p>
   * These values reward major accomplishments with bonus credits:
   * - Exploration achievements: 10-20 credits
   * - Collection achievements: 5-10 credits
   * - Boss/milestone achievements: 5-10 credits
   *
   * @return Map of advancement IDs to credit amounts
   */
  private Map<String, Integer> createDefaultCustomRewards() {
    Map<String, Integer> rewards = new HashMap<>();

    // Major exploration achievements
    rewards.put("minecraft:adventure/adventuring_time", 20); // Visit all biomes
    rewards.put("minecraft:nether/explore_nether", 5); // Visit all Nether biomes

    // Collection achievements
    rewards.put("minecraft:nether/all_potions", 10); // All potion effects
    rewards.put("minecraft:nether/all_effects", 10); // All effects from any source
    rewards.put("minecraft:husbandry/complete_catalogue", 5); // Tame all cat variants

    // Boss/milestone achievements
    rewards.put("minecraft:end/dragon_egg", 10); // Obtain dragon egg
    rewards.put("minecraft:nether/summon_wither", 5); // Summon wither
    rewards.put("minecraft:nether/obtain_ancient_debris", 3); // First ancient debris

    return rewards;
  }

  /**
   * Creates the default blacklist of advancements.
   * <p>
   * Blacklisted advancements are typically:
   * - Tutorial/first-time actions (getting wood, crafting table)
   * - Very easy achievements that all players will get
   * - Achievements that provide no meaningful progression
   *
   * @return List of advancement IDs that won't grant credits
   */
  private List<String> createDefaultBlacklist() {
    List<String> blacklist = new ArrayList<>();

    // Early game tutorial achievements - too trivial
    blacklist.add("minecraft:story/root"); // Minecraft intro popup
    blacklist.add("minecraft:story/mine_stone"); // Mine stone with pickaxe
    blacklist.add("minecraft:story/upgrade_tools"); // Craft better pickaxe

    return blacklist;
  }

  /**
   * Validates all configuration values.
   * <p>
   * Checks:
   * - defaultCredits >= 0
   * - All custom reward values >= 0
   * - No null collections
   * - Advancement IDs are non-empty strings
   *
   * @return true if configuration is valid
   * @throws IllegalStateException if validation fails
   */
  public boolean validate() {
    if (defaultCredits < 0) {
      throw new IllegalStateException("defaultCredits cannot be negative: " + defaultCredits);
    }

    if (customRewards == null) {
      throw new IllegalStateException("customRewards cannot be null");
    }

    for (Map.Entry<String, Integer> entry : customRewards.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isEmpty()) {
        throw new IllegalStateException("Custom reward has empty advancement ID");
      }
      if (entry.getValue() < 0) {
        throw new IllegalStateException(
            "Custom reward for " + entry.getKey() + " cannot be negative: " + entry.getValue());
      }
    }

    if (blacklist == null) {
      throw new IllegalStateException("blacklist cannot be null");
    }

    for (String id : blacklist) {
      if (id == null || id.isEmpty()) {
        throw new IllegalStateException("Blacklist contains empty advancement ID");
      }
    }

    return true;
  }

  // Getters

  public int getDefaultCredits() {
    return defaultCredits;
  }

  public Map<String, Integer> getCustomRewards() {
    return Collections.unmodifiableMap(customRewards);
  }

  public List<String> getBlacklist() {
    return Collections.unmodifiableList(blacklist);
  }

  public boolean isAllowRevoke() {
    return allowRevoke;
  }

  public boolean isEnableNotifications() {
    return enableNotifications;
  }

  // Setters (for deserialization and testing)

  public void setDefaultCredits(int defaultCredits) {
    if (defaultCredits < 0) {
      throw new IllegalArgumentException("defaultCredits cannot be negative: " + defaultCredits);
    }
    this.defaultCredits = defaultCredits;
  }

  public void setCustomRewards(Map<String, Integer> customRewards) {
    if (customRewards == null) {
      throw new IllegalArgumentException("customRewards cannot be null");
    }
    this.customRewards = new HashMap<>(customRewards);
  }

  public void setBlacklist(List<String> blacklist) {
    if (blacklist == null) {
      throw new IllegalArgumentException("blacklist cannot be null");
    }
    this.blacklist = new ArrayList<>(blacklist);
  }

  public void setAllowRevoke(boolean allowRevoke) {
    this.allowRevoke = allowRevoke;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  @Override
  public String toString() {
    return "AdvancementRewardConfig{" +
        "defaultCredits=" + defaultCredits +
        ", customRewards=" + customRewards.size() + " entries" +
        ", blacklist=" + blacklist.size() + " entries" +
        ", allowRevoke=" + allowRevoke +
        ", enableNotifications=" + enableNotifications +
        '}';
  }
}
