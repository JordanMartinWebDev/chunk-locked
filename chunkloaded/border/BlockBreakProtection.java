package chunkloaded.border;

import chunkloaded.Chunklocked;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for block break events and protects chunk barriers.
 * Automatically replaces broken boundary blocks with barriers
 * to prevent players from creating holes in the barrier walls.
 */
public class BlockBreakProtection {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlockBreakProtection.class);

  /**
   * Registers the block break protection event handler.
   * Call this during mod initialization.
   */
  public static void register() {
    LOGGER.info("Attempting to register block break protection...");
    try {
      PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
        LOGGER.info("*** BLOCK BROKEN EVENT TRIGGERED *** at {}", pos);
        onBlockBroken(world, player, pos, state, blockEntity);
      });
      LOGGER.info("✓ Block break protection registered successfully");
    } catch (Exception e) {
      LOGGER.error("✗ Failed to register block break protection", e);
    }
  }

  /**
   * Called after a player breaks a block.
   * Checks if the broken block was at a chunk boundary and replaces it with a
   * barrier if needed.
   * 
   * @param world       The world where the block was broken
   * @param player      The player who broke the block
   * @param pos         The position of the broken block
   * @param state       The state of the broken block
   * @param blockEntity The block entity if it existed
   */
  private static void onBlockBroken(class_1937 world, class_1657 player, class_2338 pos,
      class_2680 state, @Nullable class_2586 blockEntity) {
    LOGGER.info("Block broken event fired! pos={}, world={}", pos, world.method_27983());
    // Only handle server-side and overworld
    if (!(world instanceof class_3218 serverWorld)) {
      return;
    }

    if (!serverWorld.method_27983().equals(class_1937.field_25179)) {
      return;
    }

    ChunkBarrierManager barrierManager = Chunklocked.getBarrierManager();
    if (barrierManager == null) {
      LOGGER.warn("Barrier manager is null during block break event");
      return;
    }

    // Check what block was broken
    boolean wasBarrier = state.method_27852(class_2246.field_10499);
    LOGGER.info("Block broken: {}, wasBarrier={}", state.method_26204(), wasBarrier);

    LOGGER.info("Checking if position {} should have a barrier for player {}...", pos, player.method_5667());
    // Check if this position should have a barrier (is at a boundary between
    // locked/unlocked)
    boolean shouldHave = barrierManager.shouldHaveBarrier(serverWorld, pos, player.method_5667(), wasBarrier);
    LOGGER.info("shouldHaveBarrier returned: {}", shouldHave);

    if (shouldHave) {
      // Replace the broken block with a barrier immediately
      LOGGER.info("Block broken at barrier boundary {}, replacing with barrier", pos);
      serverWorld.method_8501(pos, class_2246.field_10499.method_9564());
    } else {
      LOGGER.info("Position {} is not a barrier boundary", pos);
    }
  }
}
