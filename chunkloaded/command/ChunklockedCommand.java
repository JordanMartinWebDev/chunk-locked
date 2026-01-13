package chunkloaded.command;

import chunkloaded.Chunklocked;
import chunkloaded.advancement.AdvancementCreditManager;
import chunkloaded.core.ChunkAccessManager;
import chunkloaded.core.ChunkUnlockData;
import chunkloaded.core.ChunkManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import net.minecraft.class_1923;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2186;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_7157;

/**
 * Main command handler for /chunklocked.
 * Provides administrative and player commands for chunk management.
 */
public class ChunklockedCommand {

  /**
   * Registers all /chunklocked subcommands.
   *
   * @param dispatcher     The command dispatcher
   * @param registryAccess Command registry access
   * @param environment    Command environment
   */
  public static void register(CommandDispatcher<class_2168> dispatcher,
      class_7157 registryAccess,
      class_2170.class_5364 environment) {
    dispatcher.register(class_2170.method_9247("chunklocked")
        // /chunklocked credits [player]
        .then(class_2170.method_9247("credits")
            .executes(ChunklockedCommand::showOwnCredits)
            .then(class_2170.method_9244("player", class_2186.method_9305())
                .executes(ChunklockedCommand::showPlayerCredits))
            .then(class_2170.method_9247("add")
                .then(class_2170.method_9244("player", class_2186.method_9305())
                    .then(class_2170.method_9244("amount", IntegerArgumentType.integer(1))
                        .executes(ChunklockedCommand::addCredits))))
            .then(class_2170.method_9247("set")
                .then(class_2170.method_9244("player", class_2186.method_9305())
                    .then(class_2170.method_9244("amount", IntegerArgumentType.integer(0))
                        .executes(ChunklockedCommand::setCredits)))))
        .then(class_2170.method_9247("reset")
            .executes(ChunklockedCommand::resetCreditsSelf)
            .then(class_2170.method_9244("player", class_2186.method_9305())
                .executes(ChunklockedCommand::resetCreditsForPlayer)))

        // /chunklocked listchunks [player]
        .then(class_2170.method_9247("listchunks")
            .executes(ChunklockedCommand::listOwnChunks)
            .then(class_2170.method_9244("player", class_2186.method_9305())
                .executes(ChunklockedCommand::listPlayerChunks)))

        // /chunklocked forceunlock <player> <chunkX> <chunkZ>
        .then(class_2170.method_9247("forceunlock")
            .then(class_2170.method_9244("player", class_2186.method_9305())
                .then(class_2170.method_9244("chunkX", IntegerArgumentType.integer())
                    .then(class_2170.method_9244("chunkZ", IntegerArgumentType.integer())
                        .executes(ChunklockedCommand::forceUnlock)))))

        // /chunklocked unlock <chunkX> <chunkZ>
        .then(class_2170.method_9247("unlock")
            .then(class_2170.method_9244("chunkX", IntegerArgumentType.integer())
                .then(class_2170.method_9244("chunkZ", IntegerArgumentType.integer())
                    .executes(ChunklockedCommand::unlockChunk))))

        // /chunklocked unlockfacing - Unlock the chunk in the direction you're facing
        .then(class_2170.method_9247("unlockfacing")
            .executes(ChunklockedCommand::unlockFacing))

        // /chunklocked debugbarriers - Show all barrier positions
        .then(class_2170.method_9247("debugbarriers")
            .executes(ChunklockedCommand::debugBarriers))

        // /chunklocked givecredits <amount> - Give yourself credits (admin)
        // /chunklocked givecredits <player> <amount> - Give player credits (admin)
        .then(class_2170.method_9247("givecredits")
            .then(class_2170.method_9244("amount", IntegerArgumentType.integer(1))
                .executes(ChunklockedCommand::giveCreditsToSelf))
            .then(class_2170.method_9244("player", class_2186.method_9305())
                .then(class_2170.method_9244("amount", IntegerArgumentType.integer(1))
                    .executes(ChunklockedCommand::giveCreditsToPlayer))))

        // /chunklocked help
        .then(class_2170.method_9247("help")
            .executes(ChunklockedCommand::showHelp)));
  }

  /**
   * Shows the command sender's credit count.
   */
  private static int showOwnCredits(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();
    return showCreditsForPlayer(context.getSource(), player);
  }

  /**
   * Shows a specified player's credit count (admin only).
   */
  private static int showPlayerCredits(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = class_2186.method_9315(context, "player");
    return showCreditsForPlayer(context.getSource(), player);
  }

  /**
   * Helper method to show credit count for a player.
   */
  private static int showCreditsForPlayer(class_2168 source, class_3222 player) {
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.method_5667());

    if (playerData == null) {
      source.method_9226(() -> class_2561.method_43470("§e" + player.method_5477().getString() + " has no credit data yet."), false);
      return 0;
    }

    int credits = playerData.getAvailableCredits();
    int totalAdvancements = playerData.getTotalAdvancementsCompleted();

    source.method_9226(() -> class_2561.method_43470(
        "§a" + player.method_5477().getString() + " has §e" + credits + " §acredits available " +
            "§7(completed " + totalAdvancements + " advancements)"),
        false);

    return credits;
  }

  /**
   * Adds credits to a player (admin only).
   */
  private static int addCredits(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = class_2186.method_9315(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.method_5667());
    if (playerData == null) {
      context.getSource().method_9213(class_2561.method_43470("§cPlayer has no progression data yet!"));
      return 0;
    }
    playerData.addCredits(amount);

    context.getSource().method_9226(() -> class_2561.method_43470(
        "§aAdded §e" + amount + " §acredits to " + player.method_5477().getString()), true);

    // Sync to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());

    // Save the changes to persistent storage
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      // Log a warning if persistent data is not available
      Chunklocked.LOGGER.warn(
          "persistentData is null when trying to save credits for player {}! Credits may not persist!",
          player.method_5477().getString());
    }

    return amount;
  }

  /**
   * Sets a player's credits to a specific amount (admin only).
   */
  private static int setCredits(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = class_2186.method_9315(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.method_5667());

    if (playerData == null) {
      context.getSource().method_9213(class_2561.method_43470("§cPlayer has no progression data yet!"));
      return 0;
    }

    // Set to exact amount
    playerData.setAvailableCredits(amount);

    context.getSource().method_9226(() -> class_2561.method_43470(
        "§aSet " + player.method_5477().getString() + "'s credits to §e" + amount), true);

    // Sync to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());
    // Save the changes to persistent storage
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    }
    return amount;
  }

  /**
   * Resets the command sender's credits to 0.
   */
  private static int resetCreditsSelf(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.method_5667());

    if (playerData == null) {
      context.getSource().method_9213(class_2561.method_43470("§cYou have no progression data yet!"));
      return 0;
    }

    playerData.setAvailableCredits(0);

    context.getSource().method_9226(
        () -> class_2561.method_43470("§eReset your credits to §a0"),
        false);

    // Sync to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());

    // Save the changes
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when resetting credits for {}", player.method_5477().getString());
    }

    return 1;
  }

  /**
   * Resets a specified player's credits to 0 (admin).
   */
  private static int resetCreditsForPlayer(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 target = class_2186.method_9315(context, "player");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(target.method_5667());

    if (playerData == null) {
      context.getSource().method_9213(class_2561.method_43470("§cPlayer has no progression data yet!"));
      return 0;
    }

    playerData.setAvailableCredits(0);

    context.getSource().method_9226(
        () -> class_2561.method_43470("§eReset §b" + target.method_5477().getString() + "§e's credits to §a0"),
        true);
    target.method_64398(class_2561.method_43470("§eYour credits were reset to §a0"));

    // Sync to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        target,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());

    // Save the changes
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when resetting credits for {}", target.method_5477().getString());
    }

    return 1;
  }

  /**
   * Lists the command sender's unlocked chunks.
   */
  private static int listOwnChunks(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();
    return listChunksForPlayer(context.getSource(), player);
  }

  /**
   * Lists a specified player's unlocked chunks (admin only).
   */
  private static int listPlayerChunks(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = class_2186.method_9315(context, "player");
    return listChunksForPlayer(context.getSource(), player);
  }

  /**
   * Helper method to list chunks for a player.
   */
  private static int listChunksForPlayer(class_2168 source, class_3222 player) {
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    Set<class_1923> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.method_5667());

    if (unlockedChunks == null || unlockedChunks.isEmpty()) {
      source.method_9226(() -> class_2561.method_43470(
          "§e" + player.method_5477().getString() + " has no unlocked chunks."), false);
      return 0;
    }

    source.method_9226(() -> class_2561.method_43470(
        "§a" + player.method_5477().getString() + " has §e" + unlockedChunks.size() + " §aunlocked chunks:"), false);

    // Show first 10 chunks (to avoid spam)
    int count = 0;
    for (class_1923 chunk : unlockedChunks) {
      if (count >= 10) {
        int remaining = unlockedChunks.size() - 10;
        source.method_9226(() -> class_2561.method_43470("§7... and " + remaining + " more"), false);
        break;
      }

      source.method_9226(() -> class_2561.method_43470("  §7- Chunk [" + chunk.field_9181 + ", " + chunk.field_9180 + "]"), false);
      count++;
    }

    return unlockedChunks.size();
  }

  /**
   * Force unlocks a chunk for a player without spending credits (admin only).
   */
  private static int forceUnlock(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = class_2186.method_9315(context, "player");
    int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
    int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
    class_1923 chunk = new class_1923(chunkX, chunkZ);

    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    ChunkManager chunkManager = Chunklocked.getChunkManager();

    // Check if already unlocked
    if (chunkAccessManager.canAccessChunk(player.method_5667(), chunk)) {
      context.getSource().method_9226(() -> class_2561.method_43470(
          "§eChunk [" + chunkX + ", " + chunkZ + "] is already unlocked for " +
              player.method_5477().getString()),
          false);
      return 0;
    }

    // Force unlock (bypasses adjacency check and credit cost)
    net.minecraft.class_3218 world = context.getSource().method_9225();
    chunkManager.forceUnlockChunk(player.method_5667(), chunk, world);

    context.getSource().method_9226(() -> class_2561.method_43470(
        "§aForce unlocked chunk [" + chunkX + ", " + chunkZ + "] for " +
            player.method_5477().getString()),
        true);

    player.method_64398(class_2561.method_43470("§aChunk [" + chunkX + ", " + chunkZ + "] has been unlocked!"));

    // Mark data as dirty
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after force unlock for {}",
          player.method_5477().getString());
    }

    return 1;
  }

  /**
   * Unlocks a chunk by spending a credit (player command).
   */
  private static int unlockChunk(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();
    int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
    int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
    class_1923 chunk = new class_1923(chunkX, chunkZ);

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();

    // Check if already unlocked
    if (chunkAccessManager.canAccessChunk(player.method_5667(), chunk)) {
      player.method_64398(class_2561.method_43470("§eChunk [" + chunkX + ", " + chunkZ + "] is already unlocked!"));
      return 0;
    }

    // Check adjacency requirement (unless player has no chunks unlocked)
    Set<class_1923> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.method_5667());
    if (!unlockedChunks.isEmpty() && !chunkAccessManager.isAdjacentToUnlockedChunk(player.method_5667(), chunk)) {
      player.method_64398(class_2561.method_43470("§cChunk must be adjacent to your unlocked area!"));
      return 0;
    }

    // Check if player has credits
    var playerData = creditManager.getPlayerData(player.method_5667());
    if (playerData == null || playerData.getAvailableCredits() < 1) {
      player.method_64398(class_2561.method_43470("§cNot enough credits! Complete more advancements."));
      return 0;
    }

    // Attempt to unlock chunk (validates and spends credit atomically)
    boolean success = chunkManager.tryUnlockChunk(player.method_5667(), chunk);

    if (!success) {
      player.method_64398(class_2561.method_43470("§cFailed to unlock chunk!"));
      return 0;
    }

    // Success feedback
    int remainingCredits = creditManager.getPlayerData(player.method_5667()).getAvailableCredits();
    player.method_64398(class_2561.method_43470(
        "§aChunk [" + chunkX + ", " + chunkZ + "] unlocked! " +
            "§7(" + remainingCredits + " credits remaining)"));

    // Sync updated credits to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        remainingCredits,
        creditManager.getPlayerData(player.method_5667()).getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData2 = Chunklocked.getPersistentData();
    if (persistentData2 != null) {
      persistentData2.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after credit spend for {}",
          player.method_5477().getString());
    }

    return 1;
  }

  /**
   * Shows help information for the command.
   */
  private static int showHelp(CommandContext<class_2168> context) {
    class_2168 source = context.getSource();
    boolean isAdmin = true; // TODO: Add proper permission check

    source.method_9226(() -> class_2561.method_43470("§6§l=== Chunk Locked Commands ==="), false);
    source.method_9226(() -> class_2561.method_43470("§e/chunklocked credits §7- Show your credits"), false);
    source.method_9226(() -> class_2561.method_43470("§e/chunklocked listchunks §7- List your unlocked chunks"), false);
    source.method_9226(() -> class_2561.method_43470("§e/chunklocked unlock <chunkX> <chunkZ> §7- Unlock a chunk"), false);
    source.method_9226(() -> class_2561.method_43470("§e/chunklocked unlockfacing §7- Unlock chunk you're facing"), false);
    source.method_9226(() -> class_2561.method_43470("§e/chunklocked help §7- Show this help"), false);

    if (isAdmin) {
      source.method_9226(() -> class_2561.method_43470("§c§lAdmin Commands:"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked givecredits <amount> §7- Give yourself credits"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked credits <player> §7- Show player's credits"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked credits add <player> <amount> §7- Add credits"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked credits set <player> <amount> §7- Set credits"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked listchunks <player> §7- List player's chunks"), false);
      source.method_9226(() -> class_2561.method_43470("§e/chunklocked forceunlock <player> <x> <z> §7- Force unlock chunk"),
          false);
    }

    return 1;
  }

  /**
   * Unlocks the chunk in the direction the player is facing using raycasting
   * (player command).
   * Casts a ray from the player's eyes and finds the nearest chunk boundary
   * they're looking at.
   */
  private static int unlockFacing(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();

    // Get player's eye position
    double eyeX = player.method_23317();
    double eyeY = player.method_23320();
    double eyeZ = player.method_23321();

    // Get look direction from yaw and pitch
    float yaw = player.method_36454();
    float pitch = player.method_36455();

    // Convert yaw and pitch to direction vector
    // Minecraft yaw: 0° = South (+Z), 90° = West (-X), 180° = North (-Z), 270° =
    // East (+X)
    double yawRad = Math.toRadians(yaw);
    double pitchRad = Math.toRadians(-pitch);

    double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
    double dirY = Math.sin(pitchRad);
    double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

    // Normalize direction
    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
    dirX /= length;
    dirY /= length;
    dirZ /= length;

    // Raycast to find chunk boundary
    // Check up to 256 blocks away
    double maxDistance = 256;
    double step = 0.5; // Step size for raycasting

    class_1923 targetChunk = null;
    String direction = null;

    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();

    // Raycast along the player's look direction
    for (double t = 0; t <= maxDistance; t += step) {
      double rayX = eyeX + dirX * t;
      double rayZ = eyeZ + dirZ * t;

      // Current chunk
      int currentChunkX = ((int) Math.floor(rayX)) >> 4;
      int currentChunkZ = ((int) Math.floor(rayZ)) >> 4;

      // Check next position
      double nextX = eyeX + dirX * (t + step);
      double nextZ = eyeZ + dirZ * (t + step);

      int nextChunkX = ((int) Math.floor(nextX)) >> 4;
      int nextChunkZ = ((int) Math.floor(nextZ)) >> 4;

      // Did we cross a chunk boundary?
      if (currentChunkX != nextChunkX || currentChunkZ != nextChunkZ) {
        // We crossed a boundary - determine which chunk we're moving to
        class_1923 candidateChunk;
        if (currentChunkX != nextChunkX) {
          // Crossed X boundary (East/West)
          candidateChunk = new class_1923(nextChunkX, currentChunkZ);
          direction = (nextChunkX > currentChunkX) ? "East" : "West";
        } else {
          // Crossed Z boundary (North/South)
          candidateChunk = new class_1923(currentChunkX, nextChunkZ);
          direction = (nextChunkZ > currentChunkZ) ? "South" : "North";
        }

        // Only stop if the candidate chunk is LOCKED (not unlocked)
        // Skip boundaries between unlocked chunks
        if (!chunkAccessManager.canAccessChunk(player.method_5667(), candidateChunk)) {
          targetChunk = candidateChunk;
          break;
        }
        // Otherwise, continue raycasting through unlocked chunks
      }
    }

    // If no boundary found, use simple direction fallback
    if (targetChunk == null) {
      player.method_64398(class_2561.method_43470("§cNo locked chunk boundary found in that direction!"));
      return 0;
    }

    int targetChunkX = targetChunk.field_9181;
    int targetChunkZ = targetChunk.field_9180;

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();

    // Check if already unlocked (this shouldn't happen now, but kept as safety
    // check)
    if (chunkAccessManager.canAccessChunk(player.method_5667(), targetChunk)) {
      player.method_64398(class_2561.method_43470(
          "§eThe chunk " + direction + " of you [" + targetChunkX + ", " + targetChunkZ + "] is already unlocked!"));
      return 0;
    }

    // Check adjacency requirement (should always pass since we're getting adjacent
    // chunk)
    Set<class_1923> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.method_5667());
    if (!unlockedChunks.isEmpty() && !chunkAccessManager.isAdjacentToUnlockedChunk(player.method_5667(), targetChunk)) {
      player.method_64398(class_2561.method_43470("§cChunk must be adjacent to your unlocked area!"));
      return 0;
    }

    // Check if player has credits
    var playerData = creditManager.getPlayerData(player.method_5667());
    if (playerData == null || playerData.getAvailableCredits() < 1) {
      player.method_64398(class_2561.method_43470("§cNot enough credits! Complete more advancements."));
      return 0;
    }

    // Attempt to unlock chunk
    boolean success = chunkManager.tryUnlockChunk(player.method_5667(), targetChunk);

    if (!success) {
      player.method_64398(class_2561.method_43470("§cFailed to unlock chunk!"));
      return 0;
    }

    // Update barriers now that chunk is unlocked
    net.minecraft.class_3218 world = context.getSource().method_9211().method_30002();
    chunkManager.updateBarriersAfterUnlock(world, player.method_5667(), targetChunk);

    // Success feedback
    int remainingCredits = creditManager.getPlayerData(player.method_5667()).getAvailableCredits();
    player.method_64398(class_2561.method_43470(
        "§aUnlocked chunk " + direction + " [" + targetChunkX + ", " + targetChunkZ + "]! " +
            "§7(" + remainingCredits + " credits remaining)"));

    // Sync updated credits to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        remainingCredits,
        creditManager.getPlayerData(player.method_5667()).getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData3 = Chunklocked.getPersistentData();
    if (persistentData3 != null) {
      persistentData3.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after unlockfacing for {}",
          player.method_5477().getString());
    }

    return 1;
  }

  /**
   * Debug command: Shows all barriers placed for the player and their locations.
   */
  private static int debugBarriers(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();

    Set<class_1923> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.method_5667());

    player.method_64398(class_2561.method_43470("§6=== Chunk Barrier Debug ==="));
    player.method_64398(class_2561.method_43470("§eUnlocked chunks: §f" + unlockedChunks.size()));

    for (class_1923 chunk : unlockedChunks) {
      int worldX = chunk.field_9181 * 16;
      int worldZ = chunk.field_9180 * 16;
      player.method_64398(class_2561.method_43470("  §7[" + chunk.field_9181 + ", " + chunk.field_9180 + "] §f(world: " + worldX + " to "
          + (worldX + 15) + ", " + worldZ + " to " + (worldZ + 15) + ")"));
    }

    // Get frontier chunks (what we calculate as needing barriers)
    ChunkManager chunkManager = Chunklocked.getChunkManager();
    Set<class_1923> frontierChunks = chunkManager.calculateFrontierChunks(player.method_5667());

    player.method_64398(class_2561.method_43470("§eFrontier (locked adjacent) chunks: §f" + frontierChunks.size()));
    for (class_1923 chunk : frontierChunks) {
      int worldX = chunk.field_9181 * 16;
      int worldZ = chunk.field_9180 * 16;

      // Check which unlocked neighbors this chunk has
      class_1923 north = new class_1923(chunk.field_9181, chunk.field_9180 - 1);
      class_1923 south = new class_1923(chunk.field_9181, chunk.field_9180 + 1);
      class_1923 east = new class_1923(chunk.field_9181 + 1, chunk.field_9180);
      class_1923 west = new class_1923(chunk.field_9181 - 1, chunk.field_9180);

      StringBuilder neighbors = new StringBuilder();
      if (unlockedChunks.contains(north))
        neighbors.append("N ");
      if (unlockedChunks.contains(south))
        neighbors.append("S ");
      if (unlockedChunks.contains(east))
        neighbors.append("E ");
      if (unlockedChunks.contains(west))
        neighbors.append("W ");

      String neighborStr = neighbors.toString().isEmpty() ? "NONE" : neighbors.toString();
      player.method_64398(class_2561.method_43470("  §7[" + chunk.field_9181 + ", " + chunk.field_9180 + "] §fbarriers: §e" + neighborStr));
    }

    return 1;
  }

  /**
   * Gives credits to yourself (admin command for testing).
   */
  private static int giveCreditsToSelf(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_3222 player = context.getSource().method_9207();
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.method_5667());

    if (playerData == null) {
      // Create new player data if it doesn't exist
      playerData = new chunkloaded.advancement.PlayerProgressionData();
      creditManager.loadPlayerData(player.method_5667(), playerData);
    }

    // Add credits
    int oldCredits = playerData.getAvailableCredits();
    playerData.addCredits(amount);
    int newCredits = playerData.getAvailableCredits();

    player.method_64398(class_2561.method_43470("§aGiven §e" + amount + " §acredits! §7(" + oldCredits + " → " + newCredits + ")"));

    // Sync updated credits to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        player,
        newCredits,
        playerData.getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData4 = Chunklocked.getPersistentData();
    if (persistentData4 != null) {
      persistentData4.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after givecredits to self for {}",
          player.method_5477().getString());
    }

    return amount;
  }

  /**
   * Gives credits to another player (admin command for testing).
   * Usage: /chunklocked givecredits <player> <amount>
   */
  private static int giveCreditsToPlayer(CommandContext<class_2168> context) throws CommandSyntaxException {
    class_2168 source = context.getSource();
    class_3222 targetPlayer = class_2186.method_9315(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(targetPlayer.method_5667());

    if (playerData == null) {
      // Create new player data if it doesn't exist
      playerData = new chunkloaded.advancement.PlayerProgressionData();
      creditManager.loadPlayerData(targetPlayer.method_5667(), playerData);
    }

    // Add credits
    int oldCredits = playerData.getAvailableCredits();
    playerData.addCredits(amount);
    int newCredits = playerData.getAvailableCredits();

    targetPlayer
        .method_64398(class_2561.method_43470("§aGiven §e" + amount + " §acredits! §7(" + oldCredits + " → " + newCredits + ")"));
    source.method_9226(() -> class_2561.method_43470("§aGave §e" + amount + " §acredits to §b"
        + targetPlayer.method_5477().getString() + "§a! §7(" + oldCredits + " → " + newCredits + ")"), false);

    // Sync updated credits to client
    chunkloaded.advancement.NotificationManager.syncCreditsToClient(
        targetPlayer,
        newCredits,
        playerData.getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData5 = Chunklocked.getPersistentData();
    if (persistentData5 != null) {
      persistentData5.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after givecredits to player {}",
          targetPlayer.method_5477().getString());
    }

    return amount;
  }
}
