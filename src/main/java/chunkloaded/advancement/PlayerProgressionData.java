package chunkloaded.advancement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks progression data for a single player.
 * <p>
 * Stores information about:
 * - Available chunk unlock credits
 * - Total advancements completed
 * - Which advancements have already been rewarded
 * <p>
 * This class is not thread-safe. All access should be synchronized by the
 * caller
 * or confined to the server thread.
 */
public class PlayerProgressionData {
  /**
   * Set of advancement IDs that have already granted credits to this player.
   * Prevents duplicate credit awards.
   */
  private final Set<String> rewardedAdvancements;

  /**
   * Number of chunk unlock credits available to spend.
   * Can be 0 or positive.
   */
  private int availableCredits;

  /**
   * Total number of advancements completed by this player.
   * Used for statistics and progression tracking.
   */
  private int totalAdvancementsCompleted;

  /**
   * Creates a new player progression data with zero credits and no completed
   * advancements.
   */
  public PlayerProgressionData() {
    this.rewardedAdvancements = new HashSet<>();
    this.availableCredits = 0;
    this.totalAdvancementsCompleted = 0;
  }

  /**
   * Checks if the player has already received a credit reward for the specified
   * advancement.
   *
   * @param advancementId The advancement identifier to check
   * @return true if this advancement has already been rewarded, false otherwise
   */
  public boolean hasReceivedReward(String advancementId) {
    return rewardedAdvancements.contains(advancementId);
  }

  /**
   * Marks an advancement as having been rewarded.
   * <p>
   * This should be called after awarding credits to prevent duplicate rewards.
   * Marking an already-marked advancement has no effect.
   *
   * @param advancementId The advancement identifier to mark as rewarded
   */
  public void markAdvancementRewarded(String advancementId) {
    rewardedAdvancements.add(advancementId);
    totalAdvancementsCompleted++;
  }

  /**
   * Adds credits to the player's available balance.
   * <p>
   * The amount must be non-negative.
   *
   * @param amount The number of credits to add (must be >= 0)
   * @throws IllegalArgumentException if amount is negative
   */
  public void addCredits(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Cannot add negative credits: " + amount);
    }
    this.availableCredits += amount;
  }

  /**
   * Spends credits from the player's available balance.
   * <p>
   * The amount must not exceed the available balance.
   *
   * @param amount The number of credits to spend (must be > 0 and <=
   *               availableCredits)
   * @throws IllegalArgumentException if amount is invalid or exceeds available
   *                                  credits
   */
  public void spendCredits(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Cannot spend non-positive credits: " + amount);
    }
    if (amount > availableCredits) {
      throw new IllegalArgumentException(
          "Insufficient credits: tried to spend " + amount + " but only have " + availableCredits);
    }
    this.availableCredits -= amount;
  }

  /**
   * Gets the number of credits currently available to spend.
   *
   * @return The available credit balance (>= 0)
   */
  public int getAvailableCredits() {
    return availableCredits;
  }

  /**
   * Gets the total number of advancements completed by this player.
   * <p>
   * This includes all advancements that have been marked as rewarded,
   * regardless of whether they granted credits (e.g., blacklisted advancements).
   *
   * @return The total advancement count (>= 0)
   */
  public int getTotalAdvancementsCompleted() {
    return totalAdvancementsCompleted;
  }

  /**
   * Gets an unmodifiable view of the advancements that have been rewarded.
   * <p>
   * This is primarily for serialization and debugging purposes.
   *
   * @return An unmodifiable set of rewarded advancement identifiers
   */
  public Set<String> getRewardedAdvancements() {
    return Collections.unmodifiableSet(rewardedAdvancements);
  }

  /**
   * Sets the available credits directly.
   * <p>
   * This is intended for deserialization and should not be used during normal
   * gameplay.
   * Use {@link #addCredits(int)} or {@link #spendCredits(int)} instead.
   *
   * @param credits The credit amount to set (must be >= 0)
   * @throws IllegalArgumentException if credits is negative
   */
  public void setAvailableCredits(int credits) {
    if (credits < 0) {
      throw new IllegalArgumentException("Credits cannot be negative: " + credits);
    }
    this.availableCredits = credits;
  }

  /**
   * Sets the total advancements completed directly.
   * <p>
   * This is intended for deserialization and should not be used during normal
   * gameplay.
   *
   * @param total The total to set (must be >= 0)
   * @throws IllegalArgumentException if total is negative
   */
  public void setTotalAdvancementsCompleted(int total) {
    if (total < 0) {
      throw new IllegalArgumentException("Total advancements cannot be negative: " + total);
    }
    this.totalAdvancementsCompleted = total;
  }

  @Override
  public String toString() {
    return "PlayerProgressionData{" +
        "availableCredits=" + availableCredits +
        ", totalAdvancementsCompleted=" + totalAdvancementsCompleted +
        ", rewardedAdvancementsCount=" + rewardedAdvancements.size() +
        '}';
  }
}
