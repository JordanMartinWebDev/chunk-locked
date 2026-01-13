package chunkloaded.portal;

import chunkloaded.Chunklocked;
import chunkloaded.core.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import net.minecraft.class_1923;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

/**
 * Detects when players exit Nether portals into the Overworld.
 * Auto-unlocks the chunk they enter if:
 * 1. The chunk is locked (not already unlocked)
 * 2. They have available credits
 * 
 * If they don't have credits, nothing happens (Wither damage will be added
 * later).
 * 
 * IMPLEMENTATION: Uses a Mixin on ServerPlayerEntity.teleportTo(TeleportTarget)
 * to detect
 * dimension changes. This is the proper method in Yarn 1.21.11+build.4 for
 * teleportation
 * and dimension changes. The TeleportTarget contains the destination
 * ServerWorld.
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
  public static void onPlayerEnteredOverworld(class_3222 player) {
    LOGGER.info("Player {} entered Overworld (from another dimension)", player.method_5667());

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    if (chunkManager == null) {
      LOGGER.warn("Chunk manager not initialized during portal exit");
      return;
    }

    // Verify player is actually in Overworld
    if (!player.method_51469().method_27983().equals(net.minecraft.class_1937.field_25179)) {
      LOGGER.warn("onPlayerEnteredOverworld called but player is not in Overworld?");
      return;
    }

    // Get the chunk the player is in
    class_1923 currentChunk = new class_1923(player.method_24515());

    LOGGER.info("Player {} exited Nether portal into chunk [{}, {}]",
        player.method_5667(), currentChunk.field_9181, currentChunk.field_9180);

    // Check if chunk is already unlocked
    if (chunkManager.isChunkUnlockedGlobally(currentChunk)) {
      LOGGER.info("Chunk [{}, {}] already unlocked", currentChunk.field_9181, currentChunk.field_9180);
      return;
    }

    // Check if player has credits
    int credits = chunkManager.getAvailableCredits(player.method_5667());
    if (credits <= 0) {
      LOGGER.info("Player {} has no credits ({}) - cannot auto-unlock chunk",
          player.method_5667(), credits);
      // Eventually add Wither effect here
      return;
    }

    // Try to unlock the chunk (will spend 1 credit if successful)
    boolean unlocked = chunkManager.tryUnlockChunk(player.method_5667(), currentChunk);

    if (unlocked) {
      LOGGER.info("✓ Auto-unlocked chunk [{}, {}] for player {} via Nether portal exit",
          currentChunk.field_9181, currentChunk.field_9180, player.method_5667());

      // Update barriers around the newly unlocked chunk and adjacent locked chunks
      try {
        class_3218 world = (class_3218) player.method_51469();
        chunkManager.updateBarriersAfterUnlock(world, player.method_5667(), currentChunk);
        LOGGER.info("✓ Updated barriers after unlocking chunk [{}, {}]", currentChunk.field_9181, currentChunk.field_9180);
      } catch (Exception e) {
        LOGGER.error("Failed to update barriers after unlocking chunk", e);
      }

      player.method_7353(
          net.minecraft.class_2561.method_43470(
              "§a✓ Chunk auto-unlocked via Nether portal! (Credits remaining: " +
                  chunkManager.getAvailableCredits(player.method_5667()) + ")"),
          false);
    } else {
      LOGGER.warn("Failed to unlock chunk [{}, {}] for player {} (unknown reason)",
          currentChunk.field_9181, currentChunk.field_9180, player.method_5667());
    }
  }
}
