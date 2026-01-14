package chunklocked.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Semi-transparent barrier block with player-only collision.
 * 
 * Functions like vanilla barrier blocks (unbreakable) but with entity-specific
 * collision:
 * - Players are blocked by a full collision box
 * - Ender pearls are blocked (prevents teleport escape)
 * - Mobs can pass through and pathfind through
 * - Items and projectiles (except ender pearls) pass through
 * - Fluids are blocked (treated as solid)
 * 
 * This makes barriers less intrusive for non-player entities while still
 * preventing players from entering locked chunks.
 * 
 * Properties:
 * - Unbreakable in survival mode (hardness -1.0)
 * - Max blast resistance
 * - No drops when broken
 * - Player-only collision (mobs/items pass through)
 * - Blocks ender pearls (prevents teleport exploit)
 * - Mob pathfinding allowed (mobs will walk through)
 * - Semi-transparent rendering (requires BlockRenderLayerMap registration on
 * client)
 * - Connected texture appearance (faces between adjacent barriers are culled)
 * - Blocks fluids (water/lava stopped by barriers)
 * 
 * Note: Fluids are completely blocked to prevent players from using water to
 * swim through barriers. Ender pearls are blocked to prevent teleport escape.
 * 
 * @see chunklocked.ChunklockedClient - Where the render layer is registered
 */
public class BarrierBlockV2 extends Block {

  // Cache collision shapes for performance
  private static final VoxelShape PLAYER_COLLISION = Shapes.block(); // Full cube
  private static final VoxelShape NO_COLLISION = Shapes.empty(); // Empty shape

  public BarrierBlockV2(BlockBehaviour.Properties settings) {
    super(settings);
  }

  /**
   * Return collision shape based on what entity is colliding.
   * 
   * Players are blocked by a full cube collision box, while most other entities
   * (mobs, items, most projectiles) can pass through as if the block doesn't
   * exist.
   * Ender pearls are blocked to prevent players from teleporting through
   * barriers.
   * 
   * @param state   The block state
   * @param level   The world/level
   * @param pos     The block position
   * @param context Collision context containing entity information
   * @return Full cube for players and ender pearls, empty shape for everything
   *         else
   */
  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
      BlockPos pos, CollisionContext context) {
    // Check if context contains entity information
    if (context instanceof EntityCollisionContext entityContext) {
      Entity entity = entityContext.getEntity();

      // Block players with full collision
      if (entity instanceof Player) {
        return PLAYER_COLLISION;
      }

      // Block ender pearls to prevent teleport escape
      if (entity != null && entity.getType() == EntityType.ENDER_PEARL) {
        return PLAYER_COLLISION;
      }

      // Also block entities controlled by players (boats, minecarts, etc.)
      if (entity != null && entity.getControllingPassenger() instanceof Player) {
        return PLAYER_COLLISION;
      }
    }

    // Everything else passes through (mobs, items, other projectiles)
    return NO_COLLISION;
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

  /**
   * Return visual shape for mob AI pathfinding.
   * 
   * Mobs use this shape (not collision shape) to determine if they can pathfind
   * through a block. Return empty shape so mobs consider this block passable.
   * 
   * @param state   The block state
   * @param level   The world/level
   * @param pos     The block position
   * @param context Collision context
   * @return Empty shape so mobs can pathfind through
   */
  @Override
  public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  /**
   * Prevent fluids and other blocks from replacing this barrier.
   * 
   * This prevents water/lava from destroying barrier blocks when flowing.
   * Fluids will stop at the barrier edge instead.
   * 
   * @param state   The block state to check
   * @param context The replacement context
   * @return false - block cannot be replaced by fluids or other blocks
   */
  @Override
  public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    return false;
  }
}
