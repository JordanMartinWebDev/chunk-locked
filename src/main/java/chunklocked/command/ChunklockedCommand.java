package chunklocked.command;

import chunklocked.Chunklocked;
import chunklocked.advancement.AdvancementCreditManager;
import chunklocked.core.ChunkAccessManager;
import chunklocked.core.ChunkUnlockData;
import chunklocked.core.ChunkManager;
import chunklocked.core.ChunklockedMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;

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
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext registryAccess,
      Commands.CommandSelection environment) {
    dispatcher.register(Commands.literal("chunklocked")
        // /chunklocked credits [player]
        .then(Commands.literal("credits")
            .executes(ChunklockedCommand::showOwnCredits)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ChunklockedCommand::showPlayerCredits))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ChunklockedCommand::addCredits))))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ChunklockedCommand::setCredits)))))
        .then(Commands.literal("reset")
            .executes(ChunklockedCommand::resetCreditsSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ChunklockedCommand::resetCreditsForPlayer)))

        // /chunklocked listchunks [player]
        .then(Commands.literal("listchunks")
            .executes(ChunklockedCommand::listOwnChunks)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ChunklockedCommand::listPlayerChunks)))

        // /chunklocked forceunlock <player> <chunkX> <chunkZ>
        .then(Commands.literal("forceunlock")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                    .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                        .executes(ChunklockedCommand::forceUnlock)))))

        // /chunklocked unlock <chunkX> <chunkZ>
        .then(Commands.literal("unlock")
            .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                    .executes(ChunklockedCommand::unlockChunk))))

        // /chunklocked unlockfacing - Unlock the chunk in the direction you're facing
        .then(Commands.literal("unlockfacing")
            .executes(ChunklockedCommand::unlockFacing))

        // /chunklocked debugbarriers - Show all barrier positions
        .then(Commands.literal("debugbarriers")
            .executes(ChunklockedCommand::debugBarriers))

        // /chunklocked set_mode <easy|extreme|disabled> - Set the Chunklocked mode
        // (admin only)
        .then(Commands.literal("set_mode")
            .then(Commands.argument("mode", StringArgumentType.word())
                .executes(ChunklockedCommand::setMode)))

        // /chunklocked toggle_barriers - Toggle barriers on/off (emergency use)
        .then(Commands.literal("toggle_barriers")
            .executes(ChunklockedCommand::toggleBarriers))

        // /chunklocked givecredits <amount> - Give yourself credits (admin)
        // /chunklocked givecredits <player> <amount> - Give player credits (admin)
        .then(Commands.literal("givecredits")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(ChunklockedCommand::giveCreditsToSelf))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ChunklockedCommand::giveCreditsToPlayer))))

        // /chunklocked help
        .then(Commands.literal("help")
            .executes(ChunklockedCommand::showHelp)));
  }

  /**
   * Shows the command sender's credit count.
   */
  private static int showOwnCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    return showCreditsForPlayer(context.getSource(), player);
  }

  /**
   * Shows a specified player's credit count (admin only).
   */
  private static int showPlayerCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = EntityArgument.getPlayer(context, "player");
    return showCreditsForPlayer(context.getSource(), player);
  }

  /**
   * Helper method to show credit count for a player.
   */
  private static int showCreditsForPlayer(CommandSourceStack source, ServerPlayer player) {
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.getUUID());

    if (playerData == null) {
      source.sendSuccess(() -> Component.literal("§e" + player.getName().getString() + " has no credit data yet."),
          false);
      return 0;
    }

    int credits = playerData.getAvailableCredits();
    int totalAdvancements = playerData.getTotalAdvancementsCompleted();

    source.sendSuccess(() -> Component.literal(
        "§a" + player.getName().getString() + " has §e" + credits + " §acredits available " +
            "§7(completed " + totalAdvancements + " advancements)"),
        false);

    return credits;
  }

  /**
   * Adds credits to a player (admin only).
   */
  private static int addCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = EntityArgument.getPlayer(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.getUUID());
    if (playerData == null) {
      context.getSource().sendFailure(Component.literal("§cPlayer has no progression data yet!"));
      return 0;
    }
    playerData.addCredits(amount);

    context.getSource().sendSuccess(() -> Component.literal(
        "§aAdded §e" + amount + " §acredits to " + player.getName().getString()), true);

    // Sync to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
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
          player.getName().getString());
    }

    return amount;
  }

  /**
   * Sets a player's credits to a specific amount (admin only).
   */
  private static int setCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = EntityArgument.getPlayer(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.getUUID());

    if (playerData == null) {
      context.getSource().sendFailure(Component.literal("§cPlayer has no progression data yet!"));
      return 0;
    }

    // Set to exact amount
    playerData.setAvailableCredits(amount);

    context.getSource().sendSuccess(() -> Component.literal(
        "§aSet " + player.getName().getString() + "'s credits to §e" + amount), true);

    // Sync to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
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
  private static int resetCreditsSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.getUUID());

    if (playerData == null) {
      context.getSource().sendFailure(Component.literal("§cYou have no progression data yet!"));
      return 0;
    }

    playerData.setAvailableCredits(0);

    context.getSource().sendSuccess(
        () -> Component.literal("§eReset your credits to §a0"),
        false);

    // Sync to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        player,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());

    // Save the changes
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when resetting credits for {}", player.getName().getString());
    }

    return 1;
  }

  /**
   * Resets a specified player's credits to 0 (admin).
   */
  private static int resetCreditsForPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer target = EntityArgument.getPlayer(context, "player");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(target.getUUID());

    if (playerData == null) {
      context.getSource().sendFailure(Component.literal("§cPlayer has no progression data yet!"));
      return 0;
    }

    playerData.setAvailableCredits(0);

    context.getSource().sendSuccess(
        () -> Component.literal("§eReset §b" + target.getName().getString() + "§e's credits to §a0"),
        true);
    target.sendSystemMessage(Component.literal("§eYour credits were reset to §a0"));

    // Sync to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        target,
        playerData.getAvailableCredits(),
        playerData.getTotalAdvancementsCompleted());

    // Save the changes
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when resetting credits for {}", target.getName().getString());
    }

    return 1;
  }

  /**
   * Lists the command sender's unlocked chunks.
   */
  private static int listOwnChunks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    return listChunksForPlayer(context.getSource(), player);
  }

  /**
   * Lists a specified player's unlocked chunks (admin only).
   */
  private static int listPlayerChunks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = EntityArgument.getPlayer(context, "player");
    return listChunksForPlayer(context.getSource(), player);
  }

  /**
   * Helper method to list chunks for a player.
   */
  private static int listChunksForPlayer(CommandSourceStack source, ServerPlayer player) {
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    Set<ChunkPos> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.getUUID());

    if (unlockedChunks == null || unlockedChunks.isEmpty()) {
      source.sendSuccess(() -> Component.literal(
          "§e" + player.getName().getString() + " has no unlocked chunks."), false);
      return 0;
    }

    source.sendSuccess(() -> Component.literal(
        "§a" + player.getName().getString() + " has §e" + unlockedChunks.size() + " §aunlocked chunks:"), false);

    // Show first 10 chunks (to avoid spam)
    int count = 0;
    for (ChunkPos chunk : unlockedChunks) {
      if (count >= 10) {
        int remaining = unlockedChunks.size() - 10;
        source.sendSuccess(() -> Component.literal("§7... and " + remaining + " more"), false);
        break;
      }

      source.sendSuccess(() -> Component.literal("  §7- Chunk [" + chunk.x + ", " + chunk.z + "]"), false);
      count++;
    }

    return unlockedChunks.size();
  }

  /**
   * Force unlocks a chunk for a player without spending credits (admin only).
   */
  private static int forceUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = EntityArgument.getPlayer(context, "player");
    int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
    int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
    ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    ChunkManager chunkManager = Chunklocked.getChunkManager();

    // Check if already unlocked
    if (chunkAccessManager.canAccessChunk(player.getUUID(), chunk)) {
      context.getSource().sendSuccess(() -> Component.literal(
          "§eChunk [" + chunkX + ", " + chunkZ + "] is already unlocked for " +
              player.getName().getString()),
          false);
      return 0;
    }

    // Force unlock (bypasses adjacency check and credit cost)
    net.minecraft.server.level.ServerLevel world = context.getSource().getLevel();
    chunkManager.forceUnlockChunk(player.getUUID(), chunk, world);

    context.getSource().sendSuccess(() -> Component.literal(
        "§aForce unlocked chunk [" + chunkX + ", " + chunkZ + "] for " +
            player.getName().getString()),
        true);

    player.sendSystemMessage(Component.literal("§aChunk [" + chunkX + ", " + chunkZ + "] has been unlocked!"));

    // Mark data as dirty
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData != null) {
      persistentData.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after force unlock for {}",
          player.getName().getString());
    }

    return 1;
  }

  /**
   * Unlocks a chunk by spending a credit (player command).
   */
  private static int unlockChunk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
    int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
    ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();

    // Check if already unlocked
    if (chunkAccessManager.canAccessChunk(player.getUUID(), chunk)) {
      player.sendSystemMessage(Component.literal("§eChunk [" + chunkX + ", " + chunkZ + "] is already unlocked!"));
      return 0;
    }

    // Check adjacency requirement (unless player has no chunks unlocked)
    Set<ChunkPos> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.getUUID());
    if (!unlockedChunks.isEmpty() && !chunkAccessManager.isAdjacentToUnlockedChunk(player.getUUID(), chunk)) {
      player.sendSystemMessage(Component.literal("§cChunk must be adjacent to your unlocked area!"));
      return 0;
    }

    // Check if player has credits
    var playerData = creditManager.getPlayerData(player.getUUID());
    if (playerData == null || playerData.getAvailableCredits() < 1) {
      player.sendSystemMessage(Component.literal("§cNot enough credits! Complete more advancements."));
      return 0;
    }

    // Attempt to unlock chunk (validates and spends credit atomically)
    boolean success = chunkManager.tryUnlockChunk(player.getUUID(), chunk);

    if (!success) {
      player.sendSystemMessage(Component.literal("§cFailed to unlock chunk!"));
      return 0;
    }

    // Success feedback
    int remainingCredits = creditManager.getPlayerData(player.getUUID()).getAvailableCredits();
    player.sendSystemMessage(Component.literal(
        "§aChunk [" + chunkX + ", " + chunkZ + "] unlocked! " +
            "§7(" + remainingCredits + " credits remaining)"));

    // Sync updated credits to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        player,
        remainingCredits,
        creditManager.getPlayerData(player.getUUID()).getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData2 = Chunklocked.getPersistentData();
    if (persistentData2 != null) {
      persistentData2.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after credit spend for {}",
          player.getName().getString());
    }

    return 1;
  }

  /**
   * Shows help information for the command.
   */
  private static int showHelp(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    boolean isAdmin = true; // TODO: Add proper permission check

    source.sendSuccess(() -> Component.literal("§6§l=== Chunk Locked Commands ==="), false);
    source.sendSuccess(() -> Component.literal("§e/chunklocked credits §7- Show your credits"), false);
    source.sendSuccess(() -> Component.literal("§e/chunklocked listchunks §7- List your unlocked chunks"), false);
    source.sendSuccess(() -> Component.literal("§e/chunklocked unlock <chunkX> <chunkZ> §7- Unlock a chunk"), false);
    source.sendSuccess(() -> Component.literal("§e/chunklocked unlockfacing §7- Unlock chunk you're facing"), false);
    source.sendSuccess(() -> Component.literal("§e/chunklocked help §7- Show this help"), false);

    if (isAdmin) {
      source.sendSuccess(() -> Component.literal("§c§lAdmin Commands:"), false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked givecredits <amount> §7- Give yourself credits"),
          false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked credits <player> §7- Show player's credits"), false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked credits add <player> <amount> §7- Add credits"),
          false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked credits set <player> <amount> §7- Set credits"),
          false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked listchunks <player> §7- List player's chunks"), false);
      source.sendSuccess(() -> Component.literal("§e/chunklocked forceunlock <player> <x> <z> §7- Force unlock chunk"),
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
  private static int unlockFacing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();

    // Get player's eye position
    double eyeX = player.getX();
    double eyeY = player.getEyeY();
    double eyeZ = player.getZ();

    // Get look direction from yaw and pitch
    float yaw = player.getYRot();
    float pitch = player.getXRot();

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

    ChunkPos targetChunk = null;
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
        ChunkPos candidateChunk;
        if (currentChunkX != nextChunkX) {
          // Crossed X boundary (East/West)
          candidateChunk = new ChunkPos(nextChunkX, currentChunkZ);
          direction = (nextChunkX > currentChunkX) ? "East" : "West";
        } else {
          // Crossed Z boundary (North/South)
          candidateChunk = new ChunkPos(currentChunkX, nextChunkZ);
          direction = (nextChunkZ > currentChunkZ) ? "South" : "North";
        }

        // Only stop if the candidate chunk is LOCKED (not unlocked)
        // Skip boundaries between unlocked chunks
        if (!chunkAccessManager.canAccessChunk(player.getUUID(), candidateChunk)) {
          targetChunk = candidateChunk;
          break;
        }
        // Otherwise, continue raycasting through unlocked chunks
      }
    }

    // If no boundary found, use simple direction fallback
    if (targetChunk == null) {
      player.sendSystemMessage(Component.literal("§cNo locked chunk boundary found in that direction!"));
      return 0;
    }

    int targetChunkX = targetChunk.x;
    int targetChunkZ = targetChunk.z;

    ChunkManager chunkManager = Chunklocked.getChunkManager();
    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();

    // Check if already unlocked (this shouldn't happen now, but kept as safety
    // check)
    if (chunkAccessManager.canAccessChunk(player.getUUID(), targetChunk)) {
      player.sendSystemMessage(Component.literal(
          "§eThe chunk " + direction + " of you [" + targetChunkX + ", " + targetChunkZ + "] is already unlocked!"));
      return 0;
    }

    // Check adjacency requirement (should always pass since we're getting adjacent
    // chunk)
    Set<ChunkPos> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.getUUID());
    if (!unlockedChunks.isEmpty() && !chunkAccessManager.isAdjacentToUnlockedChunk(player.getUUID(), targetChunk)) {
      player.sendSystemMessage(Component.literal("§cChunk must be adjacent to your unlocked area!"));
      return 0;
    }

    // Check if player has credits
    var playerData = creditManager.getPlayerData(player.getUUID());
    if (playerData == null || playerData.getAvailableCredits() < 1) {
      player.sendSystemMessage(Component.literal("§cNot enough credits! Complete more advancements."));
      return 0;
    }

    // Attempt to unlock chunk
    boolean success = chunkManager.tryUnlockChunk(player.getUUID(), targetChunk);

    if (!success) {
      player.sendSystemMessage(Component.literal("§cFailed to unlock chunk!"));
      return 0;
    }

    // Update barriers now that chunk is unlocked
    net.minecraft.server.level.ServerLevel world = context.getSource().getServer().overworld();
    chunkManager.updateBarriersAfterUnlock(world, player.getUUID(), targetChunk);

    // Success feedback
    int remainingCredits = creditManager.getPlayerData(player.getUUID()).getAvailableCredits();
    player.sendSystemMessage(Component.literal(
        "§aUnlocked chunk " + direction + " [" + targetChunkX + ", " + targetChunkZ + "]! " +
            "§7(" + remainingCredits + " credits remaining)"));

    // Sync updated credits to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        player,
        remainingCredits,
        creditManager.getPlayerData(player.getUUID()).getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData3 = Chunklocked.getPersistentData();
    if (persistentData3 != null) {
      persistentData3.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after unlockfacing for {}",
          player.getName().getString());
    }

    return 1;
  }

  /**
   * Debug command: Shows all barriers placed for the player and their locations.
   */
  private static int debugBarriers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    ChunkAccessManager chunkAccessManager = Chunklocked.getChunkAccessManager();

    Set<ChunkPos> unlockedChunks = chunkAccessManager.getUnlockedChunks(player.getUUID());

    player.sendSystemMessage(Component.literal("§6=== Chunk Barrier Debug ==="));
    player.sendSystemMessage(Component.literal("§eUnlocked chunks: §f" + unlockedChunks.size()));

    for (ChunkPos chunk : unlockedChunks) {
      int worldX = chunk.x * 16;
      int worldZ = chunk.z * 16;
      player.sendSystemMessage(Component.literal("  §7[" + chunk.x + ", " + chunk.z + "] §f(world: " + worldX + " to "
          + (worldX + 15) + ", " + worldZ + " to " + (worldZ + 15) + ")"));
    }

    // Get frontier chunks (what we calculate as needing barriers)
    ChunkManager chunkManager = Chunklocked.getChunkManager();
    Set<ChunkPos> frontierChunks = chunkManager.calculateFrontierChunks(player.getUUID());

    player.sendSystemMessage(Component.literal("§eFrontier (locked adjacent) chunks: §f" + frontierChunks.size()));
    for (ChunkPos chunk : frontierChunks) {
      int worldX = chunk.x * 16;
      int worldZ = chunk.z * 16;

      // Check which unlocked neighbors this chunk has
      ChunkPos north = new ChunkPos(chunk.x, chunk.z - 1);
      ChunkPos south = new ChunkPos(chunk.x, chunk.z + 1);
      ChunkPos east = new ChunkPos(chunk.x + 1, chunk.z);
      ChunkPos west = new ChunkPos(chunk.x - 1, chunk.z);

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
      player
          .sendSystemMessage(Component.literal("  §7[" + chunk.x + ", " + chunk.z + "] §fbarriers: §e" + neighborStr));
    }

    return 1;
  }

  /**
   * Gives credits to yourself (admin command for testing).
   */
  private static int giveCreditsToSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(player.getUUID());

    if (playerData == null) {
      // Create new player data if it doesn't exist
      playerData = new chunklocked.advancement.PlayerProgressionData();
      creditManager.loadPlayerData(player.getUUID(), playerData);
    }

    // Add credits
    int oldCredits = playerData.getAvailableCredits();
    playerData.addCredits(amount);
    int newCredits = playerData.getAvailableCredits();

    player.sendSystemMessage(
        Component.literal("§aGiven §e" + amount + " §acredits! §7(" + oldCredits + " → " + newCredits + ")"));

    // Sync updated credits to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        player,
        newCredits,
        playerData.getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData4 = Chunklocked.getPersistentData();
    if (persistentData4 != null) {
      persistentData4.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after givecredits to self for {}",
          player.getName().getString());
    }

    return amount;
  }

  /**
   * Gives credits to another player (admin command for testing).
   * Usage: /chunklocked givecredits <player> <amount>
   */
  private static int giveCreditsToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");

    AdvancementCreditManager creditManager = Chunklocked.getCreditManager();
    var playerData = creditManager.getPlayerData(targetPlayer.getUUID());

    if (playerData == null) {
      // Create new player data if it doesn't exist
      playerData = new chunklocked.advancement.PlayerProgressionData();
      creditManager.loadPlayerData(targetPlayer.getUUID(), playerData);
    }

    // Add credits
    int oldCredits = playerData.getAvailableCredits();
    playerData.addCredits(amount);
    int newCredits = playerData.getAvailableCredits();

    targetPlayer
        .sendSystemMessage(
            Component.literal("§aGiven §e" + amount + " §acredits! §7(" + oldCredits + " → " + newCredits + ")"));
    source.sendSuccess(() -> Component.literal("§aGave §e" + amount + " §acredits to §b"
        + targetPlayer.getName().getString() + "§a! §7(" + oldCredits + " → " + newCredits + ")"), false);

    // Sync updated credits to client
    chunklocked.advancement.NotificationManager.syncCreditsToClient(
        targetPlayer,
        newCredits,
        playerData.getTotalAdvancementsCompleted());

    // Mark data as dirty
    ChunkUnlockData persistentData5 = Chunklocked.getPersistentData();
    if (persistentData5 != null) {
      persistentData5.markDirtyAndSave();
    } else {
      Chunklocked.LOGGER.warn("persistentData is null when saving after givecredits to player {}",
          targetPlayer.getName().getString());
    }

    return amount;
  }

  /**
   * Sets the Chunklocked mode for the world (admin only).
   * Usage: /chunklocked set_mode <easy|extreme|disabled>
   * <p>
   * Note: Mode can only be changed if it's currently DISABLED or hasn't been set
   * yet.
   * This prevents exploits where players switch modes to game the system.
   */
  private static int setMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    String modeString = StringArgumentType.getString(context, "mode");

    // Parse mode from string
    ChunklockedMode newMode;
    try {
      newMode = ChunklockedMode.valueOf(modeString.toUpperCase());
    } catch (IllegalArgumentException e) {
      source.sendFailure(Component.literal("§cInvalid mode: " + modeString + ". Valid modes: easy, extreme, disabled"));
      return 0;
    }

    // Get persistent data
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData == null) {
      source.sendFailure(Component.literal("§cFailed to access world data"));
      return 0;
    }

    ChunklockedMode currentMode = persistentData.getMode();

    // Check if mode change is allowed
    if (currentMode != ChunklockedMode.DISABLED && currentMode != newMode) {
      source.sendFailure(Component.literal(
          "§cCannot change mode from " + currentMode + " to " + newMode + ". Mode can only be set once per world."));
      source.sendFailure(Component.literal("§7(This prevents players from switching modes to exploit the system)"));
      return 0;
    }

    // Set the mode
    persistentData.setMode(newMode);
    persistentData.markDirtyAndSave();

    source.sendSuccess(() -> Component.literal("§aChunklocked mode set to: §e" + newMode), true);
    Chunklocked.LOGGER.info("Chunklocked mode changed from {} to {} by {}", currentMode, newMode, source.getTextName());

    return 1;
  }

  /**
   * Toggles barriers on/off globally (emergency use for stuck players).
   * Usage: /chunklocked toggle_barriers
   * <p>
   * When barriers are disabled:
   * - All existing barriers are removed from the world
   * - Progression still works (credits, chunk unlocking)
   * - Players can explore freely
   * <p>
   * When barriers are re-enabled:
   * - Barriers are restored at chunk boundaries
   * - Normal chunk locking behavior resumes
   */
  private static int toggleBarriers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();

    // Get persistent data
    ChunkUnlockData persistentData = Chunklocked.getPersistentData();
    if (persistentData == null) {
      source.sendFailure(Component.literal("§cFailed to access world data"));
      return 0;
    }

    // Get barrier manager and chunk manager
    chunklocked.border.ChunkBarrierManager barrierManager = Chunklocked.getBarrierManager();
    ChunkManager chunkManager = Chunklocked.getChunkManager();

    if (barrierManager == null || chunkManager == null) {
      source.sendFailure(Component.literal("§cChunk management system not initialized"));
      return 0;
    }

    // Toggle the state
    boolean currentlyEnabled = persistentData.areBarriersEnabled();
    boolean newState = !currentlyEnabled;
    persistentData.setBarriersEnabled(newState);

    // Get server and overworld
    net.minecraft.server.MinecraftServer server = source.getServer();
    ServerLevel overworld = server.overworld();

    if (newState) {
      // Re-enable barriers: restore them at chunk boundaries
      source.sendSuccess(() -> Component.literal("§aRe-enabling barriers..."), false);

      Set<ChunkPos> unlockedChunks = chunkManager.getGlobalUnlockedChunks();
      int barrierCount = 0;

      // Place barriers for all locked chunks adjacent to unlocked ones
      for (ChunkPos unlockedChunk : unlockedChunks) {
        // Check all 4 adjacent chunks
        ChunkPos[] adjacents = {
            new ChunkPos(unlockedChunk.x + 1, unlockedChunk.z),
            new ChunkPos(unlockedChunk.x - 1, unlockedChunk.z),
            new ChunkPos(unlockedChunk.x, unlockedChunk.z + 1),
            new ChunkPos(unlockedChunk.x, unlockedChunk.z - 1)
        };

        for (ChunkPos adjacent : adjacents) {
          if (!chunkManager.isChunkUnlockedGlobally(adjacent)) {
            barrierManager.placeBarriers(overworld, adjacent, unlockedChunks);
            barrierCount++;
          }
        }
      }

      final int finalBarrierCount = barrierCount;
      source.sendSuccess(() -> Component.literal("§aBarriers restored! §7(" + finalBarrierCount + " chunks protected)"),
          true);
      Chunklocked.LOGGER.info("Barriers re-enabled by {}. Restored barriers for {} locked chunks.",
          source.getTextName(), barrierCount);

    } else {
      // Disable barriers: remove all barriers from the world
      source.sendSuccess(() -> Component.literal("§cDisabling barriers..."), false);

      int removedCount = barrierManager.clearAllBarriers(overworld);

      source.sendSuccess(() -> Component.literal("§cBarriers disabled! §7(Removed " + removedCount + " barriers)"),
          true);
      source.sendSuccess(
          () -> Component
              .literal("§eWarning: §7Progression still active. Use §e/chunklocked toggle_barriers §7again to restore."),
          false);
      Chunklocked.LOGGER.warn("Barriers disabled by {}. Removed {} barriers. Progression still tracked.",
          source.getTextName(), removedCount);
    }

    // Save the state
    persistentData.markDirtyAndSave();

    return 1;
  }
}
