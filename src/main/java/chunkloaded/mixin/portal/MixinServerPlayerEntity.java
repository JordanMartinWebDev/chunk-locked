package chunkloaded.mixin.portal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import chunkloaded.portal.NetherPortalDetection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Mixin to detect when a player teleports to a different dimension.
 * In Yarn 1.21.11, the method is teleportTo(TeleportTransition), not moveToWorld or
 * changeDimension.
 * TeleportTransition contains the destination ServerLevel, allowing us to detect
 * dimension changes.
 * 
 * LESSON LEARNED: Always use javap to inspect actual compiled method names
 * before writing mixins.
 * The previous attempts (moveToWorld, changeDimension) were guessed based on
 * Forge/MCP naming,
 * but Yarn mappings use different names. This is the ACTUAL method name in Yarn
 * 1.21.11+build.4.
 */
@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntity {

  /**
   * Inject after teleportTo completes to detect dimension changes.
   * The TeleportTransition parameter contains the destination world, allowing us to
   * check
   * if the player is entering the Overworld from another dimension.
   * 
   * Method signature verified using:
   * javap -p net/minecraft/server/network/ServerPlayer.class | grep
   * teleportTo
   * Result: public net.minecraft.server.network.ServerPlayer
   * teleportTo(net.minecraft.world.TeleportTransition);
   */
  @Inject(method = "teleportTo(Lnet/minecraft/world/TeleportTransition;)Lnet/minecraft/server/network/ServerPlayer;", at = @At("RETURN"))
  private void onTeleportTo(TeleportTransition target, CallbackInfoReturnable<ServerPlayer> cir) {
    ServerPlayer player = (ServerPlayer) (Object) this;

    // Check if the destination world is the Overworld
    String targetWorldKey = target.world().dimension().getValue().toString();
    if (targetWorldKey.equals("minecraft:overworld")) {
      // Player is entering the Overworld - notify the portal detection system
      NetherPortalDetection.onPlayerEnteredOverworld(player);
    }
  }
}
