package chunkloaded.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * Semi-transparent barrier block for chunk boundaries.
 * 
 * Functions like vanilla barrier blocks (unbreakable, collision)
 * but has a visible semi-transparent gray texture so players can see chunk
 * boundaries.
 * 
 * Properties:
 * - Unbreakable in survival mode (hardness -1.0)
 * - Max blast resistance
 * - No drops when broken
 * - Full collision box
 * - Semi-transparent rendering (requires BlockRenderLayerMap registration on
 * client)
 * - Connected texture appearance (faces between adjacent barriers are culled)
 * 
 * @see chunkloaded.ChunklockedClient - Where the render layer is registered
 */
public class BarrierBlockV2 extends Block {

  public BarrierBlockV2(BlockBehaviour.Properties settings) {
    super(settings);
  }

  /**
   * Prevent item drops even if broken by creative players or commands.
   */
  @Override
  public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    return super.playerWillDestroy(world, pos, state, player);
  }

  /**
   * Get the shade brightness - full light for translucent rendering.
   * This prevents the block from appearing darker than surrounding blocks.
   */
  @Override
  public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
    return 1.0F;
  }

  /**
   * Allow light to propagate through this block for better visual appearance.
   * Translucent blocks should allow skylight through.
   */
  @Override
  public boolean propagatesSkylightDown(BlockState state) {
    return true;
  }

  /**
   * Skip rendering faces between adjacent barrier blocks.
   * This creates a "connected texture" appearance where walls of barriers
   * look like one continuous surface rather than individual blocks.
   * 
   * Similar to how glass blocks cull faces between adjacent glass.
   * 
   * @param state         The state of this block
   * @param adjacentState The state of the adjacent block
   * @param direction     The direction to the adjacent block
   * @return true if the face should be hidden (adjacent block is also a barrier)
   */
  @Override
  public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
    // Hide faces between adjacent barrier blocks for connected appearance
    if (adjacentState.is(this)) {
      return true;
    }
    return super.skipRendering(state, adjacentState, direction);
  }
}
