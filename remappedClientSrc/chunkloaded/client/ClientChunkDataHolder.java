package chunkloaded.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side holder for synchronized chunk progression data.
 * <p>
 * This class stores the client's view of the player's credit state, which is
 * synchronized from the server via
 * {@link chunkloaded.network.CreditUpdatePacket}.
 * <p>
 * Unlike the server-side
 * {@link chunkloaded.advancement.AdvancementCreditManager},
 * this is a simple data holder with no business logic. It's used by:
 * - HUD rendering (display available credits)
 * - Map UI (show progression info)
 * - Other client-side features that need progression data
 * <p>
 * Data is reset when:
 * - The player disconnects from the server
 * - The connection is lost
 * - A new world is joined
 * <p>
 * Thread Safety: This class is NOT thread-safe. All access should be from
 * the render thread (where UI code runs). Updates from network packets are
 * scheduled to run on the render thread before updating these values.
 */
public final class ClientChunkDataHolder {

  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * The number of unlock credits currently available to the player.
   * Initialized to 0; updated when server sends CreditUpdatePacket.
   */
  private static int clientAvailableCredits = 0;

  /**
   * The total number of advancements completed by the player.
   * Initialized to 0; updated when server sends CreditUpdatePacket.
   * Used to show progression and achievement count in UI.
   */
  private static int clientTotalAdvancements = 0;

  /**
   * Whether the client is currently connected to a server.
   * Used to avoid displaying stale data when disconnected.
   */
  private static boolean isConnected = false;

  /**
   * Private constructor to prevent instantiation.
   * All methods are static.
   */
  private ClientChunkDataHolder() {
    throw new UnsupportedOperationException("ClientChunkDataHolder is a utility class");
  }

  /**
   * Sets the available credits value from server sync.
   * <p>
   * Called when a CreditUpdatePacket is received from the server.
   * Should be called on the render thread.
   *
   * @param credits The number of available unlock credits (must be >= 0)
   * @throws IllegalArgumentException if credits is negative
   */
  public static void setCredits(int credits) {
    if (credits < 0) {
      LOGGER.warn("Attempted to set negative credits: {}", credits);
      return;
    }
    clientAvailableCredits = credits;
    LOGGER.debug("Client credits updated to: {}", credits);
  }

  /**
   * Sets the total advancements value from server sync.
   * <p>
   * Called when a CreditUpdatePacket is received from the server.
   * Should be called on the render thread.
   *
   * @param total The total number of advancements completed (must be >= 0)
   * @throws IllegalArgumentException if total is negative
   */
  public static void setTotalAdvancements(int total) {
    if (total < 0) {
      LOGGER.warn("Attempted to set negative total advancements: {}", total);
      return;
    }
    clientTotalAdvancements = total;
    LOGGER.debug("Client total advancements updated to: {}", total);
  }

  /**
   * Gets the current number of available credits.
   * <p>
   * Used by UI rendering to display credit count to player.
   *
   * @return The number of credits available to unlock chunks
   */
  public static int getCredits() {
    return clientAvailableCredits;
  }

  /**
   * Gets the current total number of advancements completed.
   * <p>
   * Used by UI to show progression statistics.
   *
   * @return The total advancements completed by the player
   */
  public static int getTotalAdvancements() {
    return clientTotalAdvancements;
  }

  /**
   * Marks the client as connected to the server.
   * Called when player joins a server or completes initial login.
   */
  public static void setConnected(boolean connected) {
    isConnected = connected;
    if (!connected) {
      reset();
    }
    LOGGER.debug("Client connection status: {}", connected);
  }

  /**
   * Checks if the client is currently connected to a server.
   *
   * @return true if connected, false otherwise
   */
  public static boolean isConnected() {
    return isConnected;
  }

  /**
   * Resets all client data to initial state.
   * <p>
   * Called when:
   * - Player disconnects from server
   * - Connection is lost
   * - Switching to a new world/server
   * <p>
   * Should be called on the render thread.
   */
  public static void reset() {
    clientAvailableCredits = 0;
    clientTotalAdvancements = 0;
    isConnected = false;
    LOGGER.debug("Client chunk data holder reset");
  }

  /**
   * Updates both credit values in a single atomic operation.
   * <p>
   * Useful for synchronizing data from network packets to avoid
   * temporary inconsistent states.
   *
   * @param credits The available credits (must be >= 0)
   * @param total   The total advancements (must be >= 0)
   * @throws IllegalArgumentException if either value is negative
   */
  public static void setAll(int credits, int total) {
    if (credits < 0 || total < 0) {
      LOGGER.warn("Attempted to set invalid values: credits={}, total={}", credits, total);
      return;
    }
    clientAvailableCredits = credits;
    clientTotalAdvancements = total;
    LOGGER.debug("Client data updated - Credits: {}, Total Advancements: {}", credits, total);
  }
}
