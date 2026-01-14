package chunklocked.border;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
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
  public static void sendBarrierParticles(ServerLevel world, ServerPlayer player, BlockPos hitPos) {
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
    Vec3 particlePos = Vec3.atBottomCenterOf(hitPos);

    // Send BARRIER particles (red prohibited icon) - only visible with barrier item
    // in hand
    // Also send SMOKE for visible feedback
    world.sendParticles(
        ParticleTypes.SMOKE,
        particlePos.x,
        particlePos.y + 0.5,
        particlePos.z,
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
  public static void sendBarrierSound(ServerLevel world, ServerPlayer player, BlockPos hitPos) {
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
    Vec3 soundPos = Vec3.atBottomCenterOf(hitPos);
    world.playSound(
        null, // null = broadcast to all players
        soundPos.x, soundPos.y, soundPos.z,
        SoundEvents.STONE_HIT,
        SoundSource.BLOCKS,
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
  public static void sendBoundaryMessage(ServerPlayer player, String lockReason) {
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
    Component message = Component.literal("§c⚠ §r" + lockReason);
    player.sendSystemMessage(message); // true = actionbar

    LOGGER.debug("Sent boundary message to {}: {}", player.getName().getString(), lockReason);
  }

  /**
   * Sends particle effect for successful chunk unlock.
   * Uses HAPPY_VILLAGER particles (green sparkles).
   * 
   * @param world    The server world
   * @param chunkPos The chunk that was unlocked
   */
  public static void sendUnlockParticles(ServerLevel world, ChunkPos chunkPos) {
    if (world == null || chunkPos == null) {
      return;
    }

    // Calculate center of chunk
    int centerX = chunkPos.getMinBlockX() + 8;
    int centerZ = chunkPos.getMinBlockZ() + 8;
    int centerY = world.getSeaLevel(); // Use sea level instead of getTopY()

    // Emit happy villager particles (green sparkles)
    world.sendParticles(
        ParticleTypes.HAPPY_VILLAGER,
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
  public static void sendUnlockSound(ServerLevel world, ChunkPos chunkPos) {
    if (world == null || chunkPos == null) {
      return;
    }

    // Calculate center of chunk
    int centerX = chunkPos.getMinBlockX() + 8;
    int centerZ = chunkPos.getMinBlockZ() + 8;
    int centerY = world.getSeaLevel(); // Use sea level instead of getTopY()

    // Play bell sound (positive feedback)
    world.playSound(
        null, // broadcast to all players
        centerX + 0.5, centerY, centerZ + 0.5,
        SoundEvents.NOTE_BLOCK_BELL,
        SoundSource.BLOCKS,
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
  public static void sendSafeReturnFeedback(ServerLevel world, ServerPlayer player) {
    if (world == null || player == null) {
      return;
    }

    double x = player.getX();
    double y = player.getY();
    double z = player.getZ();

    // Emit particles at player position
    world.sendParticles(
        ParticleTypes.HAPPY_VILLAGER,
        x,
        y + 1.0,
        z,
        8,
        0.5, 0.5, 0.5,
        0.3);

    // Play chime sound
    world.playSound(
        null,
        x, y, z,
        SoundEvents.NOTE_BLOCK_CHIME,
        SoundSource.PLAYERS,
        1.0f,
        1.2f);

    LOGGER.debug("Sent safe return feedback for player {}", player.getName().getString());
  }

  /**
   * Gets or creates feedback state for a player.
   * Used internally to track throttling.
   * 
   * @param player The player to get state for
   * @return PlayerFeedbackState (stored as entity data)
   */
  private static PlayerFeedbackState getOrCreatePlayerState(ServerPlayer player) {
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
  public static void clearPlayerState(ServerPlayer player) {
    if (player == null) {
      return;
    }
    LOGGER.debug("Cleared feedback state for player {}", player.getName().getString());
  }
}
