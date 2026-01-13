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
 * TeleportTransition contains the destination ServerLevel, allowing us to
 * detect
 * dimension changes.
 */
@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntity {

  /**
   * Inject after teleport completes to detect dimension changes.
   * The TeleportTransition parameter contains the destination world, allowing us
   * to
   * check if the player is entering the Overworld from another dimension.
   */
  @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
  private void onTeleport(TeleportTransition target, CallbackInfoReturnable<ServerPlayer> cir) {
    ServerPlayer player = (ServerPlayer) (Object) this;

    // Check if the destination world is the Overworld
    String targetWorldKey = target.newLevel().dimension().identifier().toString();
    if (targetWorldKey.equals("minecraft:overworld")) {
      // Player is entering the Overworld - notify the portal detection system
      NetherPortalDetection.onPlayerEnteredOverworld(player);
    }
  }
}
