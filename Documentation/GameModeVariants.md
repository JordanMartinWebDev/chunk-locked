# Game Mode Variants

## Overview

The Game Mode Variants system provides three difficulty modes for Chunk Locked, allowing players to customize their progression experience from casual to hardcore. Modes are automatically detected from world creation and persist for the lifetime of the world.

## Architecture

### Key Components

**Client-Side**:

- `WorldPresetsMixin` - Injects custom presets into world creation GUI and tracks selection
- World presets defined in `data/chunk-locked/worldgen/world_preset/` (easy.json, extreme.json)
- Marker file creation when user selects custom preset

**Server-Side**:

- `ChunklockedMode` enum - EASY, EXTREME, DISABLED
- `detectModeFromWorldSettings()` - Automatic mode detection on server start
- `StarterItemsManager` - Gives initial resources in Easy/Extreme modes
- `AdvancementCreditManager` - Calculates credits based on mode

### Data Flow

1. **World Creation**:

   - User opens Create World screen
   - WorldPresetsMixin injects "Chunklocked: Easy" and "Chunklocked: Extreme" presets
   - User selects preset from World Type dropdown
   - Mixin intercepts `setWorldType()` method call
   - Client saves marker file to game directory

2. **World Load**:

   - Server starts, loads world
   - `detectModeFromWorldSettings()` called during SERVER_STARTED event
   - Uses `FabricLoader.getInstance().getGameDir()` to locate marker files
   - Reads marker file content (e.g., "chunk-locked:easy")
   - Sets mode immutably and deletes marker file

3. **First Player Join**:
   - StarterItemsManager checks if mode is active (EASY/EXTREME)
   - If first join for this player, gives 3 Oak Saplings + 10 Bone Meal
   - Marks player as having received items (persists in NBT)

## Modes

### Easy Mode

**Target Audience**: Casual players, first-time experience

**Credit Rewards**:

- TASK advancements: +1 credit
- GOAL advancements: +5 credits
- CHALLENGE advancements: +10 credits

**Total Credits**: ~390 from all vanilla advancements

**Starter Items**: 3 Oak Saplings + 10 Bone Meal

**World Type**: "Chunklocked: Easy"

### Extreme Mode

**Target Audience**: Challenge seekers, experienced players

**Credit Rewards**:

- TASK advancements: +1 credit
- GOAL advancements: +2 credits (reduced from 5)
- CHALLENGE advancements: +5 credits (reduced from 10)

**Total Credits**: ~235 from all vanilla advancements

**Starter Items**: 3 Oak Saplings + 10 Bone Meal

**World Type**: "Chunklocked: Extreme"

### Disabled Mode

**Target Audience**: Normal Minecraft gameplay

**Credit Rewards**: None (advancements do nothing)

**Starter Items**: None

**World Type**: Default, Flat, Amplified, or any other standard preset

**Behavior**: Mod is completely inactive, no restrictions

## Implementation Details

### Automatic Mode Detection

**Client-Side Marker Creation** (`WorldPresetsMixin.java`):

```java
@Inject(method = "setWorldType", at = @At("HEAD"))
private void onWorldTypeChanged(WorldTypeEntry worldType, CallbackInfo ci) {
    ResourceKey<WorldPreset> presetKey = worldType.preset();

    if (presetKey.equals(CHUNKLOCKED_EASY) || presetKey.equals(CHUNKLOCKED_EXTREME)) {
        savePresetSelection(presetKey.location().toString());
    }
}

private void savePresetSelection(String presetId) {
    Path gameDir = FabricLoader.getInstance().getGameDir();

    // Delete old markers
    deleteMarkerFile(gameDir, "chunklocked_preset_chunk-locked_easy.tmp");
    deleteMarkerFile(gameDir, "chunklocked_preset_chunk-locked_extreme.tmp");

    // Save new marker
    String filename = "chunklocked_preset_" + presetId.replace(":", "_") + ".tmp";
    Path markerFile = gameDir.resolve(filename);
    Files.writeString(markerFile, presetId);
}
```

**Server-Side Detection** (`Chunklocked.java`):

```java
private static ChunklockedMode detectModeFromWorldSettings(MinecraftServer server) {
    Path gameDir = FabricLoader.getInstance().getGameDir();

    Path easyMarker = gameDir.resolve("chunklocked_preset_chunk-locked_easy.tmp");
    Path extremeMarker = gameDir.resolve("chunklocked_preset_chunk-locked_extreme.tmp");

    if (Files.exists(easyMarker)) {
        String content = Files.readString(easyMarker).trim();
        if (content.equals("chunk-locked:easy")) {
            Files.delete(easyMarker); // Clean up
            return ChunklockedMode.EASY;
        }
    }

    if (Files.exists(extremeMarker)) {
        String content = Files.readString(extremeMarker).trim();
        if (content.equals("chunk-locked:extreme")) {
            Files.delete(extremeMarker); // Clean up
            return ChunklockedMode.EXTREME;
        }
    }

    return ChunklockedMode.DISABLED;
}
```

### Credit Calculation

**Frame-Based Rewards** (`AdvancementCreditManager.java`):

```java
private int calculateFrameBonus(AdvancementFrame frame) {
    if (!mode.isActive()) return 0; // DISABLED mode

    return switch (frame) {
        case TASK -> 0; // Base reward only (1 credit)
        case GOAL -> mode == ChunklockedMode.EASY ? 4 : 1; // +4 Easy, +1 Extreme
        case CHALLENGE -> mode == ChunklockedMode.EASY ? 9 : 4; // +9 Easy, +4 Extreme
    };
}
```

### Starter Items

**Delivery Logic** (`StarterItemsManager.java`):

```java
public static boolean shouldGiveStarterItems(ServerPlayer player) {
    if (!mode.isActive()) return false; // Only in EASY/EXTREME

    UUID playerId = player.getUUID();
    return !playersReceivedStarterItems.contains(playerId);
}

public static void giveStarterItems(ServerPlayer player) {
    // 3 Oak Saplings
    ItemStack saplings = new ItemStack(Items.OAK_SAPLING, 3);
    player.getInventory().add(saplings);

    // 10 Bone Meal
    ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 10);
    player.getInventory().add(boneMeal);

    // Mark as received
    playersReceivedStarterItems.add(player.getUUID());

    // Send feedback
    player.sendSystemMessage(Component.literal("You received starter items!"));
}
```

### Mode Immutability

Once a player unlocks their first chunk, the mode becomes immutable to prevent exploits:

```java
public static boolean setMode(ChunklockedMode newMode) {
    if (isModeLocked) {
        return false; // Cannot change after first unlock
    }
    mode = newMode;
    return true;
}
```

## Configuration

No configuration required! Mode is automatically detected from world preset selection.

**Alternative**: Manual mode setting via command (for standard world types):

```
/chunklocked set_mode <easy|extreme|disabled>
```

## Persistence

**Mode State**: Saved in `chunklocked_data.nbt` under `mode` key

**Starter Items State**: Saved in `playersReceivedStarterItems` set (UUIDs)

**Marker Files**: Temporary, deleted after server reads them

## Testing

### Manual Test Cases

See `manual-tests/GameModeVariants.md` for comprehensive test scenarios:

1. World creation with Easy preset
2. World creation with Extreme preset
3. World creation with Default type (Disabled mode)
4. Credit rewards in Easy mode (TASK/GOAL/CHALLENGE)
5. Credit rewards in Extreme mode (reduced)
6. Starter items on first join
7. Starter items NOT given in Disabled mode
8. Mode persistence across saves
9. Mode immutability after first unlock
10. Multiple players (each receives starter items once)

### Unit Tests

Due to Minecraft bootstrap requirements, the following require integration testing:

- Mode detection (requires FabricLoader and file I/O)
- Preset injection (requires Minecraft GUI classes)
- Starter item delivery (requires ServerPlayer)

Testable components:

- Credit calculation logic (frame bonus calculation)
- Mode immutability enforcement
- UUID tracking for starter items

## Known Limitations

1. **Marker file approach**: Relies on temporary files in game directory. If marker file is deleted before server starts, mode defaults to DISABLED.

2. **Client-side preset tracking**: If user cancels world creation after selecting preset, marker file persists. Solution: File is overwritten on next preset selection.

3. **Multiplayer sync**: Each player receives starter items independently. If a player joins before mode is set, they won't receive items retroactively.

4. **Immutability timing**: Mode locks after first chunk unlock, not first world load. Players could theoretically change mode before unlocking any chunks (though this is by design).

## Future Improvements

- [ ] Add mode indicator to HUD (show current mode to player)
- [ ] Add `/chunklocked mode` query command (shows current mode)
- [ ] Support custom modes via data packs (define custom credit multipliers)
- [ ] Add "Hardcore" mode variant (permadeath + Extreme rewards)
- [ ] Dedicated server support (config file override for marker detection)

## API Reference

### Public Methods

**ChunklockedMode.isActive()**: Returns true if mode is EASY or EXTREME

**StarterItemsManager.shouldGiveStarterItems(ServerPlayer)**: Check if player should receive items

**StarterItemsManager.giveStarterItems(ServerPlayer)**: Give items to player

**ChunklockedMode.fromString(String)**: Parse mode from string (for commands)

### Events

No custom events - uses existing Fabric callbacks:

- `ServerLifecycleEvents.SERVER_STARTED` - for mode detection
- `ServerPlayConnectionEvents.JOIN` - for starter items

## Examples

### Checking Current Mode

```java
ChunklockedMode mode = ChunklockedMode.getCurrentMode();
if (mode.isActive()) {
    // Mod is enabled
    LOGGER.info("Chunklocked is active in {} mode", mode);
} else {
    // Mod is disabled
    LOGGER.info("Chunklocked is disabled");
}
```

### Custom Advancement Rewards

```java
public int calculateCredits(Advancement advancement) {
    AdvancementFrame frame = getFrame(advancement);
    int baseCredits = 1;
    int frameBonus = calculateFrameBonus(frame);
    return baseCredits + frameBonus;
}
```

### Detecting First-Time Players

```java
if (StarterItemsManager.shouldGiveStarterItems(player)) {
    StarterItemsManager.giveStarterItems(player);
    LOGGER.info("Gave starter items to {}", player.getName().getString());
}
```
