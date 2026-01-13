package chunkloaded.border;

import chunkloaded.Chunklocked;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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
  private static void onBlockBroken(Level world, Player player, BlockPos pos,
      BlockState state, @Nullable BlockEntity blockEntity) {
    LOGGER.info("Block broken event fired! pos={}, world={}", pos, world.dimension());
    // Only handle server-side and overworld
    if (!(world instanceof ServerLevel serverWorld)) {
      return;
    }

    if (!serverWorld.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    ChunkBarrierManager barrierManager = Chunklocked.getBarrierManager();
    if (barrierManager == null) {
      LOGGER.warn("Barrier manager is null during block break event");
      return;
    }

    // Check what block was broken
    boolean wasBarrier = state.is(Chunklocked.BARRIER_BLOCK_V2);
    LOGGER.info("Block broken: {}, wasBarrier={}", state.getBlock(), wasBarrier);

    LOGGER.info("Checking if position {} should have a barrier for player {}...", pos, player.getUUID());
    // Check if this position should have a barrier (is at a boundary between
    // locked/unlocked)
    boolean shouldHave = barrierManager.shouldHaveBarrier(serverWorld, pos, player.getUUID(), wasBarrier);
    LOGGER.info("shouldHaveBarrier returned: {}", shouldHave);

    if (shouldHave) {
      // Replace the broken block with a barrier immediately
      LOGGER.info("Block broken at barrier boundary {}, replacing with barrier", pos);
      serverWorld.setBlock(pos, Chunklocked.BARRIER_BLOCK_V2.defaultBlockState(), 3);
    } else {
      LOGGER.info("Position {} is not a barrier boundary", pos);
    }
  }
}
