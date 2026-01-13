package chunkloaded.portal;

import chunkloaded.Chunklocked;
import chunkloaded.core.ChunkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Detects when players exit Nether portals into the Overworld.
 * Auto-unlocks the chunk they enter if:
 * 1. The chunk is locked (not already unlocked)
 * 2. They have available credits
 * 
 * If they don't have credits, nothing happens (Wither damage will be added
 * later).
 * 
 * IMPLEMENTATION: Uses a Mixin on ServerPlayer.teleportTo(TeleportTransition)
 * to detect
 * dimension changes. This is the proper method in Yarn 1.21.11+build.4 for
 * teleportation
 * and dimension changes. The TeleportTransition contains the destination
 * ServerLevel.
 * 
 * See: MixinServerPlayerEntity.java for the actual injection point.
 * 
 * LESSON LEARNED: Always verify method names using javap before writing mixins.
 * The methods moveToWorld() and changeDimension() don't exist in Yarn 1.21.11 -
 * they
 * were guessed based on Forge/MCP naming conventions. The actual method is
 * teleportTo().
 */
public class NetherPortalDetection {
  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * Registers the portal detection system.
   * The actual detection happens via mixin injection in MixinServerPlayerEntity.
   */
  public static void register() {
    LOGGER.info("Registering Nether portal detection (via mixin)...");
    // Mixin is registered in chunk-locked.mixins.json
    // No event registration needed - the mixin handles it
    LOGGER.info("✓ Nether portal detection registered (mixin active)");
  }

  /**
   * Called by MixinServerPlayerEntity when a player enters the Overworld.
   * This is invoked from the mixin's @Inject callback after teleportTo completes.
   * 
   * @param player The player who just entered the Overworld
   */
  public static void onPlayerEnteredOverworld(ServerPlayer player) {
    LOGGER.info("Player {} entered Overworld (from another dimension)", player.getUUID());

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    if (chunkManager == null) {
      LOGGER.warn("Chunk manager not initialized during portal exit");
      return;
    }

    // Verify player is actually in Overworld
    if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
      LOGGER.warn("onPlayerEnteredOverworld called but player is not in Overworld?");
      return;
    }

    // Get the chunk the player is in
    ChunkPos currentChunk = new ChunkPos(player.blockPosition());

    LOGGER.info("Player {} exited Nether portal into chunk [{}, {}]",
        player.getUUID(), currentChunk.x, currentChunk.z);

    // Check if chunk is already unlocked
    if (chunkManager.isChunkUnlockedGlobally(currentChunk)) {
      LOGGER.info("Chunk [{}, {}] already unlocked", currentChunk.x, currentChunk.z);
      return;
    }

    // Check if player has credits
    int credits = chunkManager.getAvailableCredits(player.getUUID());
    if (credits <= 0) {
      LOGGER.info("Player {} has no credits ({}) - cannot auto-unlock chunk",
          player.getUUID(), credits);
      // Eventually add Wither effect here
      return;
    }

    // Try to unlock the chunk (will spend 1 credit if successful)
    boolean unlocked = chunkManager.tryUnlockChunk(player.getUUID(), currentChunk);

    if (unlocked) {
      LOGGER.info("✓ Auto-unlocked chunk [{}, {}] for player {} via Nether portal exit",
          currentChunk.x, currentChunk.z, player.getUUID());

      // Update barriers around the newly unlocked chunk and adjacent locked chunks
      try {
        ServerLevel world = (ServerLevel) player.level();
        chunkManager.updateBarriersAfterUnlock(world, player.getUUID(), currentChunk);
        LOGGER.info("✓ Updated barriers after unlocking chunk [{}, {}]", currentChunk.x, currentChunk.z);
      } catch (Exception e) {
        LOGGER.error("Failed to update barriers after unlocking chunk", e);
      }

      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              "§a✓ Chunk auto-unlocked via Nether portal! (Credits remaining: " +
                  chunkManager.getAvailableCredits(player.getUUID()) + ")"),
          false);
    } else {
      LOGGER.warn("Failed to unlock chunk [{}, {}] for player {} (unknown reason)",
          currentChunk.x, currentChunk.z, player.getUUID());
    }
  }
}
