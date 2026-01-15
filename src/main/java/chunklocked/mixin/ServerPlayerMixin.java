package chunklocked.mixin;

import chunklocked.Chunklocked;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

/**
 * Mixin into ServerPlayer to detect when players respawn after death.
 * <p>
 * This mixin injects into the {@code findRespawnPositionAndUseSpawnBlock}
 * method to intercept
 * the respawn position calculation. If the calculated respawn location is in a
 * locked chunk,
 * we modify it to return a safe location in the nearest unlocked chunk.
 * <p>
 * This preserves normal bed/spawn point behavior when in unlocked chunks while
 * preventing
 * players from getting stuck with the wither penalty effect when respawning in
 * locked areas.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

  @Shadow
  public abstract ServerLevel serverLevel();

  /**
   * Intercept respawn position to check if it's in a locked chunk.
   * If the respawn position (from bed or world spawn) is in a locked chunk,
   * we teleport the player to the nearest unlocked chunk after respawn completes.
   * 
   * @param entity  The respawned player entity
   * @param wonGame Whether the player won the game (end credits)
   * @param cir     Callback info returnable
   */
  @Inject(method = "respawn", at = @At("RETURN"))
  private void onRespawnComplete(boolean wonGame, CallbackInfoReturnable<net.minecraft.world.entity.Entity> cir) {
    // Get the returned entity (the new respawned player)
    net.minecraft.world.entity.Entity entity = cir.getReturnValue();

    // Make sure it's a ServerPlayer
    if (!(entity instanceof ServerPlayer respawnedPlayer)) {
      return;
    }

    // Get the world the player respawned in
    ServerLevel world = (ServerLevel) respawnedPlayer.level();

    // Use a slight delay to ensure position is fully set
    world.getServer().execute(() -> {
      Chunklocked.handlePlayerRespawn(respawnedPlayer, world);
    });
  }
}
