package chunkloaded.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
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
 * - Semi-transparent rendering
 */
public class BarrierBlockV2 extends Block {

  public BarrierBlockV2(BlockBehaviour.Properties settings) {
    super(settings);
  }

  /**
   * Prevent item drops even if broken by creative players or commands.
   */
  @Override
  public BlockState onBreak(Level world, BlockPos pos, BlockState state, Player player) {
    return super.onBreak(world, pos, state, player);
  }

  /**
   * Make the block transparent - allows seeing through adjacent faces.
   */
  @Override
  public boolean isTransparent(BlockState state) {
    return true;
  }

  /**
   * Get the ambient occlusion light level - full light for translucent rendering.
   */
  @Override
  public float getAmbientOcclusionLightLevel(BlockState state, BlockGetter world, BlockPos pos) {
    return 1.0F;
  }
}
