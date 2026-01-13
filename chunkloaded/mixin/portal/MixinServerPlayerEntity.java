package chunkloaded.mixin.portal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chunkloaded.portal.NetherPortalDetection;
import net.minecraft.class_3222;
import net.minecraft.class_5454;

/**
 * Mixin to detect when a player teleports to a different dimension.
 * In Yarn 1.21.11, the method is teleportTo(TeleportTarget), not moveToWorld or
 * changeDimension.
 * TeleportTarget contains the destination ServerWorld, allowing us to detect
 * dimension changes.
 * 
 * LESSON LEARNED: Always use javap to inspect actual compiled method names
 * before writing mixins.
 * The previous attempts (moveToWorld, changeDimension) were guessed based on
 * Forge/MCP naming,
 * but Yarn mappings use different names. This is the ACTUAL method name in Yarn
 * 1.21.11+build.4.
 */
@Mixin(class_3222.class)
public class MixinServerPlayerEntity {

  /**
   * Inject after teleportTo completes to detect dimension changes.
   * The TeleportTarget parameter contains the destination world, allowing us to
   * check
   * if the player is entering the Overworld from another dimension.
   * 
   * Method signature verified using:
   * javap -p net/minecraft/server/network/ServerPlayerEntity.class | grep
   * teleportTo
   * Result: public net.minecraft.server.network.ServerPlayerEntity
   * teleportTo(net.minecraft.world.TeleportTarget);
   */
  @Inject(method = "teleportTo(Lnet/minecraft/world/TeleportTarget;)Lnet/minecraft/server/network/ServerPlayerEntity;", at = @At("RETURN"))
  private void onTeleportTo(class_5454 target, CallbackInfoReturnable<class_3222> cir) {
    class_3222 player = (class_3222) (Object) this;

    // Check if the destination world is the Overworld
    String targetWorldKey = target.comp_2820().method_27983().method_29177().toString();
    if (targetWorldKey.equals("minecraft:overworld")) {
      // Player is entering the Overworld - notify the portal detection system
      NetherPortalDetection.onPlayerEnteredOverworld(player);
    }
  }
}
