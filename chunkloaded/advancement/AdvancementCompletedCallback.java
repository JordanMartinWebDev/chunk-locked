package chunkloaded.advancement;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.class_3222;
import net.minecraft.class_8779;

/**
 * Callback interface for handling advancement completion events.
 * <p>
 * This event is fired on the server thread when a player completes an
 * advancement
 * (not partial progress - only when all criteria are met).
 * <p>
 * This event is invoked after the advancement has been marked as completed in
 * the
 * player's advancement progress, ensuring that subsequent queries will reflect
 * the completed state.
 * <p>
 * <b>Thread Safety:</b> This event is only fired on the server thread.
 * Listeners
 * do not need to implement additional synchronization.
 * <p>
 * <b>Fake Players:</b> This event will not fire for fake players (automation
 * mods).
 * Only real player completions trigger this event.
 * <p>
 * Usage example:
 * 
 * <pre>{@code
 * AdvancementCompletedCallback.EVENT.register((player, advancement, criterionName) -> {
 *   System.out.println(player.getName().getString() + " completed " + advancement.id());
 * });
 * }</pre>
 *
 * @see net.minecraft.class_2985
 */
@FunctionalInterface
public interface AdvancementCompletedCallback {
  /**
   * The event instance that listeners should register with.
   * <p>
   * Multiple listeners can be registered and will be invoked in registration
   * order.
   */
  Event<AdvancementCompletedCallback> EVENT = EventFactory.createArrayBacked(
      AdvancementCompletedCallback.class,
      (listeners) -> (player, advancement, criterionName) -> {
        for (AdvancementCompletedCallback listener : listeners) {
          listener.onAdvancementCompleted(player, advancement, criterionName);
        }
      });

  /**
   * Called when a player completes an advancement.
   * <p>
   * This method is invoked after the advancement has been fully completed
   * (all criteria met), not on partial progress updates.
   *
   * @param player        The player who completed the advancement. Never null,
   *                      never a fake player.
   * @param advancement   The advancement that was completed. Never null.
   * @param criterionName The name of the criterion that triggered completion.
   *                      This is typically the last criterion to be completed,
   *                      but may be any criterion for advancements with only one
   *                      criterion.
   *                      Never null.
   */
  void onAdvancementCompleted(class_3222 player, class_8779 advancement, String criterionName);
}
