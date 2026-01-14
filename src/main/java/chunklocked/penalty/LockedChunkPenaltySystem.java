package chunklocked.penalty;

import chunklocked.Chunklocked;
import chunklocked.core.ChunkManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies Wither effect to players in locked chunks with no credits.
 * 
 * The penalty system:
 * 1. Checks player chunk positions periodically (not every tick)
 * 2. If in a locked chunk with zero credits, applies Wither effect
 * 3. Reapplies the effect every N ticks to prevent milk bucket escape
 * 4. Shows periodic warning messages
 * 
 * Performance: Chunk checks run every PENALTY_CHECK_INTERVAL ticks to minimize
 * overhead. Individual players are only checked when necessary.
 */
public class LockedChunkPenaltySystem {
  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  // Interval (in ticks) between penalty checks per player
  // 20 ticks = 1 second, so 20 = check once per second
  // This is VERY cheap: just ChunkPos lookup and set lookup
  private static final int PENALTY_CHECK_INTERVAL = 20;

  // Wither effect reapplication interval in ticks
  // 20 ticks = 1 second, so 60 = every 3 seconds
  private static final int WITHER_REAPPLY_INTERVAL = 60;

  // Warning message interval in ticks
  // Warn every 5 seconds (100 ticks)
  private static final int WARNING_MESSAGE_INTERVAL = 100;

  // Wither effect level (Wither I = 0, Wither II = 1)
  private static final int WITHER_LEVEL = 1; // Wither II for more danger

  // Wither effect duration in ticks
  // 20 ticks = 1 second, so we use 1 second duration
  // Since it reapplies, duration doesn't need to be long
  private static final int WITHER_DURATION = 20;

  /**
   * Tracks last time we checked penalty for each player.
   * Map: Player UUID → Last check tick count
   * This prevents checking the same player multiple times per tick.
   */
  private static final Map<UUID, Long> lastPenaltyCheckTick = new HashMap<>();

  /**
   * Tracks last time we warned each player (to avoid spam).
   * Map: Player UUID → Last warning tick count
   */
  private static final Map<UUID, Long> lastWarningTick = new HashMap<>();

  /**
   * Registers the penalty system to run on server ticks.
   */
  public static void register() {
    LOGGER.info("Registering locked chunk penalty system...");
    ServerTickEvents.END_SERVER_TICK.register(LockedChunkPenaltySystem::onServerTickEnd);
    LOGGER.info("✓ Locked chunk penalty system registered");
  }

  /**
   * Called at the end of each server tick.
   * Checks all players and applies penalties as needed.
   * 
   * PERFORMANCE: Only checks each player every PENALTY_CHECK_INTERVAL ticks
   * to minimize overhead. The actual checks (chunk lookup, set lookup) are
   * very cheap operations.
   */
  private static void onServerTickEnd(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      if (player == null || player.isRemoved() || player.isSpectator()) {
        continue;
      }

      // Only check this player if enough time has passed since last check
      UUID playerUuid = player.getUUID();
      Long lastCheck = lastPenaltyCheckTick.get(playerUuid);
      long currentTick = player.tickCount;

      if (lastCheck != null && (currentTick - lastCheck) < PENALTY_CHECK_INTERVAL) {
        // Not time to check this player yet
        continue;
      }

      // Time to check - record this check time
      lastPenaltyCheckTick.put(playerUuid, currentTick);

      // Perform the actual penalty check
      checkAndApplyPenalty(player);
    }
  }

  /**
   * Checks if a player is in a locked chunk with no credits and applies penalty.
   * Also removes the penalty if conditions are no longer met.
   * 
   * @param player The player to check
   */
  private static void checkAndApplyPenalty(ServerPlayer player) {
    ChunkManager chunkManager = Chunklocked.getChunkManager();
    if (chunkManager == null) {
      return;
    }

    // Penalty only applies in the Overworld
    // If player is in Nether/End, clear any pending Wither effect
    if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
      // Player is not in Overworld - remove Wither effect and tracking
      removeWitherPenalty(player);
      lastWarningTick.remove(player.getUUID());
      return;
    }

    // Get player's current chunk
    ChunkPos currentChunk = new ChunkPos(player.blockPosition());

    // Check if chunk is locked (globally)
    if (chunkManager.isChunkUnlockedGlobally(currentChunk)) {
      // Player is in an unlocked chunk - remove Wither effect and tracking
      removeWitherPenalty(player);
      lastWarningTick.remove(player.getUUID());
      return;
    }

    // Player is in a locked chunk - check if they have credits
    int credits = chunkManager.getAvailableCredits(player.getUUID());
    if (credits > 0) {
      // Player has credits, no penalty needed - remove any existing Wither effect
      removeWitherPenalty(player);
      lastWarningTick.remove(player.getUUID());
      return;
    }

    // PENALTY: Player is in locked chunk (in Overworld) with NO credits
    applyWitherPenalty(player, currentChunk);
    sendWarningMessage(player, currentChunk);
  }

  /**
   * Applies the Wither effect to the player.
   * The effect is reapplied every WITHER_REAPPLY_INTERVAL ticks to prevent
   * the player from using milk buckets to escape the penalty.
   * 
   * @param player The player to apply penalty to
   * @param chunk  The locked chunk they're in (for logging)
   */
  private static void applyWitherPenalty(ServerPlayer player, ChunkPos chunk) {
    // Apply Wither effect
    MobEffectInstance witherEffect = new MobEffectInstance(
        MobEffects.WITHER,
        WITHER_DURATION,
        WITHER_LEVEL,
        false, // ambient (no particles)
        false // show particles anyway
    );

    player.addEffect(witherEffect);

    // Log only periodically to avoid spam
    if (player.tickCount % WITHER_REAPPLY_INTERVAL == 0) {
      LOGGER.debug("Applied Wither penalty to {} in locked chunk [{}, {}]",
          player.getName().getString(), chunk.x, chunk.z);
    }
  }

  /**
   * Removes the Wither effect from a player.
   * Called when player leaves locked chunk, returns to unlocked area, or changes
   * dimensions.
   * 
   * @param player The player to remove penalty from
   */
  private static void removeWitherPenalty(ServerPlayer player) {
    if (player.hasEffect(MobEffects.WITHER)) {
      player.removeEffect(MobEffects.WITHER);
      LOGGER.debug("Removed Wither penalty from {}", player.getName().getString());
    }
  }

  /**
   * Sends a warning message to the player (if enough time has passed since last
   * warning).
   * 
   * @param player The player to warn
   * @param chunk  The locked chunk they're in
   */
  private static void sendWarningMessage(ServerPlayer player, ChunkPos chunk) {
    long currentTick = player.tickCount;
    Long lastWarning = lastWarningTick.get(player.getUUID());

    // Only send warning every WARNING_MESSAGE_INTERVAL ticks
    if (lastWarning == null || (currentTick - lastWarning) >= WARNING_MESSAGE_INTERVAL) {
      player.sendSystemMessage(
          Component.literal("§c⚠ WARNING: You are in a locked chunk with no credits!")
              .append("\n")
              .append(Component.literal("§c⚠ Return to an unlocked chunk to escape the Wither effect")));

      lastWarningTick.put(player.getUUID(), currentTick);

      LOGGER.debug("Warned player {} about locked chunk penalty", player.getName().getString());
    }
  }

  /**
   * Clears penalty tracking when a player leaves (disconnects).
   * 
   * @param playerUuid The UUID of the departing player
   */
  public static void onPlayerDisconnect(UUID playerUuid) {
    lastPenaltyCheckTick.remove(playerUuid);
    lastWarningTick.remove(playerUuid);
  }
}
