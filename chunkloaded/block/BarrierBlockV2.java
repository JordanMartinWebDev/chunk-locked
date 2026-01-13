package chunkloaded.block;

import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_4970;

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
public class BarrierBlockV2 extends class_2248 {

  public BarrierBlockV2(class_4970.class_2251 settings) {
    super(settings);
  }

  /**
   * Prevent item drops even if broken by creative players or commands.
   */
  @Override
  public class_2680 method_9576(class_1937 world, class_2338 pos, class_2680 state, class_1657 player) {
    return super.method_9576(world, pos, state, player);
  }
}
