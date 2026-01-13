package chunkloaded.advancement;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_8779;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import chunkloaded.network.CreditUpdatePacket;

/**
 * Manages player notifications for credit awards and progression updates.
 * <p>
 * Provides user-facing feedback when credits are earned, including:
 * - Chat messages with formatted text
 * - Sound effects for positive feedback
 * - Configurable notification toggle
 * <p>
 * All notifications respect the {@code enableNotifications} configuration flag.
 * When disabled, no chat messages or sounds are sent to players.
 * <p>
 * Thread Safety: All methods should be called on the server thread only.
 */
public class NotificationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * Whether notifications are enabled globally.
   * Loaded from configuration.
   */
  private boolean notificationsEnabled;

  /**
   * Creates a notification manager with default settings.
   * <p>
   * Notifications are enabled by default. Call
   * {@link #setNotificationsEnabled(boolean)}
   * to apply configuration settings.
   */
  public NotificationManager() {
    this.notificationsEnabled = true;
  }

  /**
   * Notifies a player that they've been awarded credits for completing an
   * advancement.
   * <p>
   * Sends:
   * - A formatted chat message with the advancement name and credit count
   * - An experience orb pickup sound effect
   * <p>
   * Does nothing if notifications are disabled in configuration.
   *
   * @param player      The player who earned credits
   * @param advancement The advancement that was completed
   * @param credits     The number of credits awarded (must be > 0)
   */
  public void notifyCreditAwarded(class_3222 player, class_8779 advancement, int credits) {
    if (!notificationsEnabled) {
      LOGGER.debug("Notifications disabled, skipping notification for {}", player.method_5477().getString());
      return;
    }

    if (credits <= 0) {
      LOGGER.warn("Attempted to notify player {} of non-positive credit award: {}",
          player.method_5477().getString(), credits);
      return;
    }

    // Get advancement display name
    String advancementName = getAdvancementDisplayName(advancement);

    // Create formatted message
    class_2561 message = createCreditAwardMessage(advancementName, credits);

    // Send chat message
    player.method_7353(message, false);

    // Play sound effect
    player.method_5783(
        class_3417.field_14627,
        0.5f, // volume
        1.0f // pitch
    );

    LOGGER.debug("Sent credit award notification to {}: {} credit(s) for {}",
        player.method_5477().getString(), credits, advancementName);
  }

  /**
   * Sends a notification about the player's current available credits.
   * <p>
   * Used by commands to show credit status.
   *
   * @param player           The player to notify
   * @param availableCredits Number of credits available to spend
   * @param totalCompleted   Total number of advancements completed
   */
  public void notifyCreditsAvailable(class_3222 player, int availableCredits, int totalCompleted) {
    class_2561 message = class_2561.method_43469(
        "chunklocked.credits.available",
        class_2561.method_43470(String.valueOf(availableCredits)).method_27694(style -> style.method_36139(0xFFD700)), // Gold
        class_2561.method_43470(String.valueOf(totalCompleted)).method_27694(style -> style.method_36139(0xAAAAAA)) // Gray
    );

    player.method_7353(message, false);
  }

  /**
   * Synchronizes credit data to the client via network packet.
   * <p>
   * Sends a {@link CreditUpdatePacket} to the player containing their current
   * available credits and total advancements. This updates the client-side
   * {@link chunklooked.client.ClientChunkDataHolder} for UI display.
   * <p>
   * Called when:
   * - Player earns credits (to immediately update UI)
   * - Player joins the server (initial sync)
   * - Player changes dimensions (to ensure consistency)
   *
   * @param player           The player to sync data to
   * @param availableCredits The player's current available credits
   * @param totalCompleted   The player's total advancements completed
   */
  public static void syncCreditsToClient(class_3222 player, int availableCredits, int totalCompleted) {
    try {
      CreditUpdatePacket packet = CreditUpdatePacket.create(availableCredits, totalCompleted);
      ServerPlayNetworking.send(player, packet);
      LOGGER.debug("Sent credit sync packet to {}: credits={}, total={}",
          player.method_5477().getString(), availableCredits, totalCompleted);
    } catch (Exception e) {
      LOGGER.error("Failed to send credit sync packet to player {}", player.method_5477().getString(), e);
    }
  }

  /**
   * Extracts a display name from an advancement.
   * <p>
   * Attempts to use the advancement's display name if available,
   * otherwise falls back to the advancement ID.
   * <p>
   * Package-private for testing.
   *
   * @param advancement The advancement
   * @return A human-readable name for the advancement
   */
  String getAdvancementDisplayName(class_8779 advancement) {
    // Try to get display name from advancement data
    var display = advancement.comp_1920().comp_1913();
    if (display.isPresent()) {
      return display.get().method_811().getString();
    }

    // Fallback to advancement ID
    String id = advancement.comp_1919().toString();
    // Make it more readable: "minecraft:story/mine_diamond" → "Mine Diamond"
    String[] parts = id.split("[:/]");
    String lastPart = parts[parts.length - 1];
    return formatIdentifier(lastPart);
  }

  /**
   * Formats an identifier string for display.
   * <p>
   * Converts underscores to spaces and capitalizes words.
   * Example: "mine_diamond" → "Mine Diamond"
   * <p>
   * Package-private for testing.
   *
   * @param identifier The identifier string
   * @return A formatted, human-readable string
   */
  static String formatIdentifier(String identifier) {
    String[] words = identifier.split("_");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      if (!word.isEmpty()) {
        // Capitalize first letter
        result.append(Character.toUpperCase(word.charAt(0)));
        if (word.length() > 1) {
          result.append(word.substring(1).toLowerCase());
        }

        if (i < words.length - 1) {
          result.append(" ");
        }
      }
    }

    return result.toString();
  }

  /**
   * Creates a formatted chat message for credit awards.
   * <p>
   * Format: "Advancement Complete! +X Chunk Credit(s) [Advancement Name]"
   * Color scheme:
   * - "Advancement Complete!" in green
   * - "+X Chunk Credit(s)" in gold
   * - "[Advancement Name]" in gray
   *
   * @param advancementName The name of the advancement
   * @param credits         The number of credits awarded
   * @return A formatted Component for chat display
   */
  private class_2561 createCreditAwardMessage(String advancementName, int credits) {
    String creditText = credits == 1 ? "Chunk Credit" : "Chunk Credits";

    return class_2561.method_43469(
        "chunklocked.advancement.credit_awarded",
        class_2561.method_43470(String.valueOf(credits)).method_27694(style -> style.method_36139(0xFFD700)), // Gold
        class_2561.method_43470(creditText).method_27694(style -> style.method_36139(0xFFD700)), // Gold
        class_2561.method_43470(advancementName).method_27694(style -> style.method_36139(0xAAAAAA)) // Gray
    );
  }

  /**
   * Sets whether notifications are enabled.
   * <p>
   * This should be called when loading configuration.
   *
   * @param enabled true to enable notifications, false to disable
   */
  public void setNotificationsEnabled(boolean enabled) {
    this.notificationsEnabled = enabled;
    LOGGER.debug("Notifications {}", enabled ? "enabled" : "disabled");
  }

  /**
   * Checks if notifications are currently enabled.
   *
   * @return true if notifications are enabled, false otherwise
   */
  public boolean isNotificationsEnabled() {
    return notificationsEnabled;
  }
}
