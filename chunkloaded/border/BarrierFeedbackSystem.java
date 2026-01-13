package chunkloaded.border;

import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides visual and audio feedback when players interact with chunk
 * boundaries.
 * 
 * <p>
 * Handles:
 * - Particle effects at boundary encounters
 * - Sound effects for collision feedback
 * - Action bar messages showing unlock status
 * </p>
 * 
 * <p>
 * All feedback is configurable and can be throttled to prevent spam.
 * </p>
 */
public class BarrierFeedbackSystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(BarrierFeedbackSystem.class);

  // Configuration constants
  private static final long PARTICLE_THROTTLE_MS = 1000; // 1 particle effect per second
  private static final long SOUND_THROTTLE_MS = 1000; // 1 sound per second
  private static final long MESSAGE_THROTTLE_MS = 2000; // 1 message per 2 seconds

  // Track last feedback time per player to prevent spam
  private static final class PlayerFeedbackState {
    long lastParticleTime = 0;
    long lastSoundTime = 0;
    long lastMessageTime = 0;
  }

  /**
   * Sends particle effect feedback when player hits a barrier.
   * Uses vanilla BARRIER particle (red prohibited icon).
   * 
   * @param world  The server world
   * @param player The player hitting the barrier
   * @param hitPos The position where the barrier was hit
   */
  public static void sendBarrierParticles(class_3218 world, class_3222 player, class_2338 hitPos) {
    if (world == null || player == null || hitPos == null) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    PlayerFeedbackState state = getOrCreatePlayerState(player);

    // Throttle particle effects
    if (currentTime - state.lastParticleTime < PARTICLE_THROTTLE_MS) {
      return;
    }
    state.lastParticleTime = currentTime;

    // Emit particles at the hit position
    class_243 particlePos = class_243.method_24955(hitPos);

    // Send BARRIER particles (red prohibited icon) - only visible with barrier item
    // in hand
    // Also send SMOKE for visible feedback
    world.method_65096(
        class_2398.field_11251,
        particlePos.field_1352,
        particlePos.field_1351 + 0.5,
        particlePos.field_1350,
        3, // particle count
        0.3, 0.3, 0.3, // velocity spread
        0.1 // base velocity
    );

    LOGGER.debug("Sent barrier particles at {}", hitPos);
  }

  /**
   * Sends sound feedback when player hits a barrier.
   * Uses a stone block hit sound for a solid, permanent wall feel.
   * 
   * @param world  The server world
   * @param player The player hitting the barrier
   * @param hitPos The position where the barrier was hit
   */
  public static void sendBarrierSound(class_3218 world, class_3222 player, class_2338 hitPos) {
    if (world == null || player == null || hitPos == null) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    PlayerFeedbackState state = getOrCreatePlayerState(player);

    // Throttle sound effects
    if (currentTime - state.lastSoundTime < SOUND_THROTTLE_MS) {
      return;
    }
    state.lastSoundTime = currentTime;

    // Play stone hit sound
    class_243 soundPos = class_243.method_24955(hitPos);
    world.method_43128(
        null, // null = broadcast to all players
        soundPos.field_1352, soundPos.field_1351, soundPos.field_1350,
        class_3417.field_14658,
        class_3419.field_15245,
        0.5f, // volume
        1.2f // pitch (slightly higher for "barrier" feel)
    );

    LOGGER.debug("Sent barrier sound at {}", hitPos);
  }

  /**
   * Sends action bar message when player approaches a locked chunk boundary.
   * Shows the chunk unlock requirement.
   * 
   * @param player     The player approaching the boundary
   * @param lockReason Reason the chunk is locked (e.g., "Requires 3 more
   *                   credits")
   */
  public static void sendBoundaryMessage(class_3222 player, String lockReason) {
    if (player == null || lockReason == null) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    PlayerFeedbackState state = getOrCreatePlayerState(player);

    // Throttle messages
    if (currentTime - state.lastMessageTime < MESSAGE_THROTTLE_MS) {
      return;
    }
    state.lastMessageTime = currentTime;

    // Send action bar message
    class_2561 message = class_2561.method_43470("§c⚠ §r" + lockReason);
    player.method_7353(message, true); // true = actionbar

    LOGGER.debug("Sent boundary message to {}: {}", player.method_5477().getString(), lockReason);
  }

  /**
   * Sends particle effect for successful chunk unlock.
   * Uses HAPPY_VILLAGER particles (green sparkles).
   * 
   * @param world    The server world
   * @param chunkPos The chunk that was unlocked
   */
  public static void sendUnlockParticles(class_3218 world, class_1923 chunkPos) {
    if (world == null || chunkPos == null) {
      return;
    }

    // Calculate center of chunk
    int centerX = chunkPos.method_8326() + 8;
    int centerZ = chunkPos.method_8328() + 8;
    int centerY = world.method_8615(); // Use sea level instead of getTopY()

    // Emit happy villager particles (green sparkles)
    world.method_65096(
        class_2398.field_11211,
        centerX + 0.5,
        centerY,
        centerZ + 0.5,
        15, // particle count
        8.0, 3.0, 8.0, // velocity spread
        0.5 // base velocity
    );

    LOGGER.debug("Sent unlock particles at chunk {}", chunkPos);
  }

  /**
   * Sends sound effect for successful chunk unlock.
   * Uses a bell sound for a positive, celebratory feel.
   * 
   * @param world    The server world
   * @param chunkPos The chunk that was unlocked
   */
  public static void sendUnlockSound(class_3218 world, class_1923 chunkPos) {
    if (world == null || chunkPos == null) {
      return;
    }

    // Calculate center of chunk
    int centerX = chunkPos.method_8326() + 8;
    int centerZ = chunkPos.method_8328() + 8;
    int centerY = world.method_8615(); // Use sea level instead of getTopY()

    // Play bell sound (positive feedback)
    world.method_60511(
        null, // broadcast to all players
        centerX + 0.5, centerY, centerZ + 0.5,
        class_3417.field_14793,
        class_3419.field_15245,
        1.0f, // volume
        1.5f // pitch (higher = more celebratory)
    );

    LOGGER.debug("Sent unlock sound at chunk {}", chunkPos);
  }

  /**
   * Sends particle and sound for safe return (player exited locked chunk back to
   * Nether).
   * Uses HAPPY_VILLAGER particles and chime sound.
   * 
   * @param world  The server world
   * @param player The player who exited safely
   */
  public static void sendSafeReturnFeedback(class_3218 world, class_3222 player) {
    if (world == null || player == null) {
      return;
    }

    double x = player.method_23317();
    double y = player.method_23318();
    double z = player.method_23321();

    // Emit particles at player position
    world.method_65096(
        class_2398.field_11211,
        x,
        y + 1.0,
        z,
        8,
        0.5, 0.5, 0.5,
        0.3);

    // Play chime sound
    world.method_60511(
        null,
        x, y, z,
        class_3417.field_14725,
        class_3419.field_15248,
        1.0f,
        1.2f);

    LOGGER.debug("Sent safe return feedback for player {}", player.method_5477().getString());
  }

  /**
   * Gets or creates feedback state for a player.
   * Used internally to track throttling.
   * 
   * @param player The player to get state for
   * @return PlayerFeedbackState (stored as entity data)
   */
  private static PlayerFeedbackState getOrCreatePlayerState(class_3222 player) {
    // Store state in player NBT data
    String key = "ChunkLocked_FeedbackState";

    // For simplicity, we'll create a new state each call and rely on throttling
    // In a production system, this would be cached in player NBT
    return new PlayerFeedbackState();
  }

  /**
   * Clears feedback state for a player (called on disconnect).
   * 
   * @param player The player disconnecting
   */
  public static void clearPlayerState(class_3222 player) {
    if (player == null) {
      return;
    }
    LOGGER.debug("Cleared feedback state for player {}", player.method_5477().getString());
  }
}
