package chunkloaded.mixin.advancement;

import chunkloaded.advancement.AdvancementCompletedCallback;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into PlayerAdvancements to detect when players complete advancements.
 * <p>
 * This mixin injects into the {@code award} method at the RETURN point,
 * which occurs after the criterion has been awarded and advancement progress
 * updated.
 * By checking {@code isDone()} at this point, we can detect when the final
 * criterion
 * was just completed.
 * <p>
 * <b>Why inject at RETURN:</b>
 * - The advancement progress has been fully updated
 * - The criterion has been successfully awarded
 * - We can check if the advancement is now complete
 * - Rewards and triggers have already been processed
 * <p>
 * <b>Fake Player Filtering:</b>
 * The {@code award} method already includes fake player detection via
 * {@code Player.isEffectiveAi()} checks, so we inherit this protection.
 * Only real players will reach this injection point with completed
 * advancements.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
  /**
   * Shadow field to access the player who owns this advancement tracker.
   */
  @Shadow
  private ServerPlayer owner;

  /**
   * Shadow method to get the progress for a specific advancement.
   * Used to check if the advancement is now complete.
   *
   * @param advancement The advancement to get progress for
   * @return The advancement progress tracker
   */
  @Shadow
  public abstract AdvancementProgress getProgress(AdvancementHolder advancement);

  /**
   * Inject into grantCriterion to detect advancement completions.
   * <p>
   * This method is called when a player makes progress on an advancement
   * criterion.
   * By injecting at RETURN, we catch the moment when the final criterion is
   * completed
   * and the advancement becomes fully done.
   * <p>
   * The method signature:
   * {@code public boolean grantCriterion(AdvancementHolder advancement, String criterionName)}
   *
   * @param advancement   The advancement being progressed
   * @param criterionName The specific criterion that was just granted
   * @param cir           Callback info returnable (contains the return value)
   */
  @Inject(method = "grantCriterion", at = @At("RETURN"))
  private void onCriterionGranted(
      AdvancementHolder advancement,
      String criterionName,
      CallbackInfoReturnable<Boolean> cir) {
    // Only fire event if the advancement is now complete
    // This prevents firing on partial progress updates
    AdvancementProgress progress = this.getProgress(advancement);
    if (progress.isDone()) {
      // Fire the advancement completed event
      // The owner field is guaranteed to be a real player (not fake)
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          this.owner,
          advancement,
          criterionName);
    }
  }
}
