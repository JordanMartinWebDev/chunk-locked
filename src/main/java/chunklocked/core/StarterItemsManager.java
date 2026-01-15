package chunklocked.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the starter items given to players in Chunklocked worlds.
 * <p>
 * In EASY and EXTREME modes, players receive:
 * <ul>
 * <li>3 Oak Saplings</li>
 * <li>10 Bone Meal</li>
 * </ul>
 * <p>
 * These items help players establish a tree farm early, which is critical
 * for progression in a limited-chunk environment.
 * <p>
 * Starter items are given only once per player, tracked in ChunkUnlockData.
 *
 * @since 1.0.0
 */
public class StarterItemsManager {
  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * Gives starter items to a player.
   * <p>
   * Items given:
   * <ul>
   * <li>3 Oak Saplings (for sustainable wood)</li>
   * <li>10 Bone Meal (to accelerate tree growth)</li>
   * </ul>
   * <p>
   * Items are added directly to the player's inventory. If the inventory is full,
   * excess items will be dropped at the player's feet.
   *
   * @param player The player to give items to
   * @throws IllegalArgumentException if player is null
   */
  public static void giveStarterItems(ServerPlayer player) {
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }

    LOGGER.info("Giving starter items to player: {}", player.getName().getString());

    // Give 3 Oak Saplings
    ItemStack saplings = new ItemStack(Items.OAK_SAPLING, 3);
    boolean saplingsAdded = player.getInventory().add(saplings);
    if (!saplingsAdded) {
      // Inventory full, drop at player's feet
      player.drop(saplings, false);
      LOGGER.debug("Player inventory full, dropped {} saplings at their feet", saplings.getCount());
    }

    // Give 10 Bone Meal
    ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 10);
    boolean boneMealAdded = player.getInventory().add(boneMeal);
    if (!boneMealAdded) {
      // Inventory full, drop at player's feet
      player.drop(boneMeal, false);
      LOGGER.debug("Player inventory full, dropped {} bone meal at their feet", boneMeal.getCount());
    }

    LOGGER.info("Successfully gave starter items to {}: 3x Oak Sapling, 10x Bone Meal",
        player.getName().getString());
  }

  /**
   * Checks if a player should receive starter items based on the world mode.
   * <p>
   * Players should receive starter items if:
   * <ul>
   * <li>The world mode is EASY or EXTREME (not DISABLED)</li>
   * <li>They have not already received starter items</li>
   * </ul>
   *
   * @param mode            The world's difficulty mode
   * @param alreadyReceived Whether the player has already received starter items
   * @return true if starter items should be given, false otherwise
   */
  public static boolean shouldGiveStarterItems(ChunklockedMode mode, boolean alreadyReceived) {
    return mode.isActive() && !alreadyReceived;
  }
}
