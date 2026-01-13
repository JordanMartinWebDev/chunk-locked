package chunkloaded.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
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
  public void notifyCreditAwarded(ServerPlayer player, AdvancementHolder advancement, int credits) {
    if (!notificationsEnabled) {
      LOGGER.debug("Notifications disabled, skipping notification for {}", player.getName().getString());
      return;
    }

    if (credits <= 0) {
      LOGGER.warn("Attempted to notify player {} of non-positive credit award: {}",
          player.getName().getString(), credits);
      return;
    }

    // Get advancement display name
    String advancementName = getAdvancementDisplayName(advancement);

    // Create formatted message
    Component message = createCreditAwardMessage(advancementName, credits);

    // Send chat message
    player.sendSystemMessage(message);

    // Play sound effect
    player.playSound(
        SoundEvents.EXPERIENCE_ORB_PICKUP,
        0.5f, // volume
        1.0f // pitch
    );

    LOGGER.debug("Sent credit award notification to {}: {} credit(s) for {}",
        player.getName().getString(), credits, advancementName);
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
  public void notifyCreditsAvailable(ServerPlayer player, int availableCredits, int totalCompleted) {
    Component message = Component.translatable(
        "chunklocked.credits.available",
        Component.literal(String.valueOf(availableCredits)).withStyle(style -> style.withColor(0xFFD700)), // Gold
        Component.literal(String.valueOf(totalCompleted)).withStyle(style -> style.withColor(0xAAAAAA)) // Gray
    );

    player.sendSystemMessage(message);
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
  public static void syncCreditsToClient(ServerPlayer player, int availableCredits, int totalCompleted) {
    try {
      CreditUpdatePacket packet = CreditUpdatePacket.create(availableCredits, totalCompleted);
      ServerPlayNetworking.send(player, packet);
      LOGGER.debug("Sent credit sync packet to {}: credits={}, total={}",
          player.getName().getString(), availableCredits, totalCompleted);
    } catch (Exception e) {
      LOGGER.error("Failed to send credit sync packet to player {}", player.getName().getString(), e);
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
  String getAdvancementDisplayName(AdvancementHolder advancement) {
    // Try to get display name from advancement data
    var display = advancement.value().display();
    if (display.isPresent()) {
      return display.get().getTitle().getString();
    }

    // Fallback to advancement ID
    String id = advancement.id().toString();
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
  private Component createCreditAwardMessage(String advancementName, int credits) {
    String creditText = credits == 1 ? "Chunk Credit" : "Chunk Credits";

    return Component.translatable(
        "chunklocked.advancement.credit_awarded",
        Component.literal(String.valueOf(credits)).withStyle(style -> style.withColor(0xFFD700)), // Gold
        Component.literal(creditText).withStyle(style -> style.withColor(0xFFD700)), // Gold
        Component.literal(advancementName).withStyle(style -> style.withColor(0xAAAAAA)) // Gray
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
