package chunklocked.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Represents the difficulty mode for Chunk Locked progression.
 * <p>
 * Modes control how credits are awarded and whether the mod is active:
 * <ul>
 * <li><b>DISABLED</b>: Mod is inactive (standard world types). No barriers or
 * progression.</li>
 * <li><b>EASY</b>: Generous credit rewards (TASK=1, GOAL=5, CHALLENGE=10). Best
 * for casual play.</li>
 * <li><b>EXTREME</b>: Strict credit rewards (TASK=1, GOAL=2, CHALLENGE=5). For
 * hardcore players.</li>
 * </ul>
 * <p>
 * Mode is set at world creation and cannot be changed afterward.
 *
 * @since 1.0.0
 */
public enum ChunklockedMode {
  /**
   * Mod is disabled. No chunk locking, no barriers, no progression.
   * Used for standard world types (Default, Flat, Large Biomes, etc.).
   */
  DISABLED,

  /**
   * Easy difficulty mode with generous credit rewards.
   * <ul>
   * <li>TASK advancements: 1 credit</li>
   * <li>GOAL advancements: 5 credits</li>
   * <li>CHALLENGE advancements: 10 credits</li>
   * <li>Max possible credits: ~390</li>
   * <li>Starter items: 3 Oak Saplings + 10 Bone Meal</li>
   * </ul>
   */
  EASY,

  /**
   * Extreme difficulty mode with strict credit rewards.
   * <ul>
   * <li>TASK advancements: 1 credit</li>
   * <li>GOAL advancements: 2 credits</li>
   * <li>CHALLENGE advancements: 5 credits</li>
   * <li>Max possible credits: ~235</li>
   * <li>Starter items: 3 Oak Saplings + 10 Bone Meal</li>
   * </ul>
   */
  EXTREME;

  /**
   * Represents the frame type of an advancement (cosmetic border).
   */
  public enum AdvancementType {
    /** Regular advancements with square border. Most common type. */
    TASK,
    /** Important milestones with rounded border (e.g., "Acquire Hardware"). */
    GOAL,
    /** Difficult achievements with ornate border (e.g., "How Did We Get Here?"). */
    CHALLENGE
  }

  /**
   * Checks if the mod is active in this mode.
   * <p>
   * The mod is only active in EASY and EXTREME modes. DISABLED mode means
   * the player is using a standard world type and the mod should not interfere.
   *
   * @return true if the mod should enforce chunk locking (EASY or EXTREME), false
   *         otherwise
   */
  public boolean isActive() {
    return this != DISABLED;
  }

  /**
   * Calculates the credit multiplier for a given advancement type.
   * <p>
   * Credit multipliers determine how many unlock credits are awarded when
   * a player completes an advancement. Recipe advancements and blacklisted
   * advancements should be filtered out before calling this method.
   * <p>
   * <b>DISABLED mode</b>: Returns -1 to indicate config-based calculation should
   * be used.
   * <br>
   * <b>EASY mode</b>: TASK=1, GOAL=5, CHALLENGE=10
   * <br>
   * <b>EXTREME mode</b>: TASK=1, GOAL=2, CHALLENGE=5
   *
   * @param type the advancement frame type (TASK, GOAL, or CHALLENGE)
   * @return the credit multiplier, or -1 if config-based calculation should be
   *         used
   * @throws IllegalArgumentException if type is null
   */
  public int getCreditMultiplier(AdvancementType type) {
    if (type == null) {
      throw new IllegalArgumentException("AdvancementType cannot be null");
    }

    return switch (this) {
      case DISABLED -> -1; // Use config multipliers
      case EASY -> switch (type) {
        case TASK -> 1;
        case GOAL -> 5;
        case CHALLENGE -> 10;
      };
      case EXTREME -> switch (type) {
        case TASK -> 1;
        case GOAL -> 2;
        case CHALLENGE -> 5;
      };
    };
  }
}
