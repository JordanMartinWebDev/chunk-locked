# Advancement Credit System - API Documentation

## Overview

The advancement credit system tracks player progression through Minecraft advancements and awards unlock credits that can be spent to expand playable areas. This system integrates with Minecraft's vanilla advancement framework to provide a seamless progression mechanic.

**Package**: `chunkloaded.advancement`

**Key Features**:

- Automatic credit awards on advancement completion
- Configurable rewards per advancement
- Advancement blacklist support
- Player progression tracking with persistence
- In-game notifications
- Recipe advancement filtering

---

## Architecture

### Component Interactions

```
AdvancementCompletedCallback (Fabric Event)
           ↓
    onAdvancementCompleted()
           ↓
  AdvancementCreditManager ──→ NotificationManager
           ↓                            ↓
  PlayerProgressionData            [Player Chat]
           ↓
    [World Save Data]
```

### Data Flow

1. **Event Registration**: Mod initialization registers advancement callback
2. **Event Trigger**: Player completes advancement in-game
3. **Filtering**: System checks if advancement is recipe or blacklisted
4. **Duplicate Check**: Verifies player hasn't already been rewarded
5. **Credit Award**: Adds credits to player's progression data
6. **Notification**: Sends feedback to player (if enabled)
7. **Persistence**: Data auto-saves with world data

---

## API Reference

### AdvancementCreditManager

Central manager for advancement-based credit awards.

#### Constructor

```java
public AdvancementCreditManager()
```

Creates a new manager with default configuration:

- Default credits: 1 per advancement
- Notifications: enabled
- Custom rewards: empty
- Blacklist: empty

#### Core Methods

##### onAdvancementCompleted

```java
public void onAdvancementCompleted(
    ServerPlayerEntity player,
    AdvancementEntry advancement,
    String criterionName
)
```

**Purpose**: Handles advancement completion events. Main entry point for the credit system.

**Parameters**:

- `player`: The player who completed the advancement
- `advancement`: The advancement that was completed
- `criterionName`: The specific criterion that triggered completion

**Behavior**:

1. Extracts advancement ID and player UUID
2. Filters out recipe advancements (`:recipes/` pattern)
3. Checks blacklist
4. Verifies not already rewarded
5. Calculates credit amount (custom or default)
6. Awards credits and marks advancement as rewarded
7. Sends notification to player

**Example**:

```java
// Called automatically by AdvancementCompletedCallback
manager.onAdvancementCompleted(player, advancement, "requirement");
```

##### getPlayerData (UUID)

```java
public PlayerProgressionData getPlayerData(UUID playerId)
```

**Purpose**: Retrieves or creates progression data for a player by UUID.

**Parameters**:

- `playerId`: The player's unique identifier

**Returns**: PlayerProgressionData (never null)

**Example**:

```java
UUID playerId = player.getUuid();
PlayerProgressionData data = manager.getPlayerData(playerId);
int credits = data.getAvailableCredits();
```

##### getPlayerData (ServerPlayerEntity)

```java
public PlayerProgressionData getPlayerData(ServerPlayerEntity player)
```

**Purpose**: Convenience method to get progression data from player entity.

**Parameters**:

- `player`: The player entity

**Returns**: PlayerProgressionData (never null)

**Example**:

```java
PlayerProgressionData data = manager.getPlayerData(player);
```

#### Configuration Methods

##### loadConfig

```java
public void loadConfig(AdvancementRewardConfig config)
```

**Purpose**: Loads configuration and updates internal state. Clears existing config before loading.

**Parameters**:

- `config`: The configuration to load

**Side Effects**:

- Clears all custom rewards
- Clears blacklist
- Updates default credits
- Updates notification settings
- Loads new custom rewards and blacklist

**Example**:

```java
AdvancementRewardConfig config = ConfigManager.getAdvancementRewardConfig();
manager.loadConfig(config);
```

##### setCustomReward

```java
public void setCustomReward(String advancementId, int credits)
```

**Purpose**: Sets a custom credit reward for a specific advancement.

**Parameters**:

- `advancementId`: The advancement identifier (e.g., "minecraft:end/kill_dragon")
- `credits`: Number of credits to award (must be >= 0)

**Throws**: IllegalArgumentException if credits < 0

**Example**:

```java
manager.setCustomReward("minecraft:end/kill_dragon", 10);
manager.setCustomReward("minecraft:story/mine_diamond", 2);
```

##### addBlacklistedAdvancement

```java
public void addBlacklistedAdvancement(String advancementId)
```

**Purpose**: Adds an advancement to the blacklist. Blacklisted advancements never grant credits.

**Parameters**:

- `advancementId`: The advancement identifier to blacklist

**Example**:

```java
manager.addBlacklistedAdvancement("minecraft:story/root");
manager.addBlacklistedAdvancement("minecraft:recipes/misc/bucket");
```

##### clearCustomRewards

```java
public void clearCustomRewards()
```

**Purpose**: Removes all custom reward configurations. Used during config reload.

##### clearBlacklist

```java
public void clearBlacklist()
```

**Purpose**: Removes all blacklisted advancements. Used during config reload.

#### Query Methods

##### getCreditAmount

```java
public int getCreditAmount(String advancementId)
```

**Purpose**: Gets the credit amount that would be awarded for an advancement.

**Parameters**:

- `advancementId`: The advancement identifier

**Returns**: Credit amount (0 if blacklisted, custom amount if configured, default otherwise)

**Example**:

```java
int credits = manager.getCreditAmount("minecraft:end/kill_dragon");
// Returns 10 if custom reward set, 1 if default, 0 if blacklisted
```

##### isBlacklisted

```java
public boolean isBlacklisted(String advancementId)
```

**Purpose**: Checks if an advancement is blacklisted.

**Parameters**:

- `advancementId`: The advancement identifier

**Returns**: true if blacklisted, false otherwise

**Example**:

```java
if (manager.isBlacklisted("minecraft:story/root")) {
    // This advancement won't grant credits
}
```

##### getTrackedPlayerCount / getPlayerCount

```java
public int getTrackedPlayerCount()
public int getPlayerCount()  // Alias
```

**Purpose**: Gets the number of players with progression data.

**Returns**: Player count

**Example**:

```java
int playerCount = manager.getTrackedPlayerCount();
LOGGER.info("Tracking {} players", playerCount);
```

#### Persistence Methods

##### getAllPlayerData

```java
public Map<UUID, PlayerProgressionData> getAllPlayerData()
```

**Purpose**: Gets all player progression data for serialization.

**Returns**: Unmodifiable map of player data

**Example**:

```java
Map<UUID, PlayerProgressionData> allData = manager.getAllPlayerData();
// Save to NBT...
```

##### loadPlayerData

```java
public void loadPlayerData(UUID playerId, PlayerProgressionData data)
```

**Purpose**: Loads player data from deserialization.

**Parameters**:

- `playerId`: The player's UUID
- `data`: The progression data to load

**Example**:

```java
// During world load
PlayerProgressionData data = PlayerProgressionData.fromNbt(nbt);
manager.loadPlayerData(playerId, data);
```

#### Other Methods

##### getNotificationManager

```java
public NotificationManager getNotificationManager()
```

**Purpose**: Gets the notification manager instance.

**Returns**: NotificationManager

**Example**:

```java
NotificationManager notif = manager.getNotificationManager();
notif.setNotificationsEnabled(false);
```

---

### PlayerProgressionData

Tracks individual player progression including credits and completed advancements.

#### Constructor

```java
public PlayerProgressionData()
```

Creates new progression data with:

- 0 available credits
- Empty advancement history

#### Credit Management

##### addCredits

```java
public void addCredits(int amount)
```

**Purpose**: Adds credits to the player's available balance.

**Parameters**:

- `amount`: Number of credits to add (must be > 0)

**Throws**: IllegalArgumentException if amount <= 0

**Example**:

```java
data.addCredits(5);
```

##### spendCredits

```java
public boolean spendCredits(int amount)
```

**Purpose**: Attempts to spend credits from the player's balance.

**Parameters**:

- `amount`: Number of credits to spend (must be > 0)

**Returns**: true if successful, false if insufficient credits

**Throws**: IllegalArgumentException if amount <= 0

**Example**:

```java
if (data.spendCredits(2)) {
    // Unlock chunks
} else {
    // Not enough credits
}
```

##### getAvailableCredits

```java
public int getAvailableCredits()
```

**Purpose**: Gets the current available credit balance.

**Returns**: Number of available credits

**Example**:

```java
int credits = data.getAvailableCredits();
player.sendMessage(Text.literal("You have " + credits + " credits"));
```

#### Advancement Tracking

##### markAdvancementRewarded

```java
public void markAdvancementRewarded(String advancementId)
```

**Purpose**: Records that a player has received a reward for an advancement.

**Parameters**:

- `advancementId`: The advancement identifier

**Example**:

```java
data.markAdvancementRewarded("minecraft:end/kill_dragon");
```

##### hasReceivedReward

```java
public boolean hasReceivedReward(String advancementId)
```

**Purpose**: Checks if player has already been rewarded for an advancement.

**Parameters**:

- `advancementId`: The advancement identifier

**Returns**: true if already rewarded, false otherwise

**Example**:

```java
if (!data.hasReceivedReward("minecraft:story/mine_diamond")) {
    data.markAdvancementRewarded("minecraft:story/mine_diamond");
    data.addCredits(2);
}
```

##### getRewardedAdvancements

```java
public Set<String> getRewardedAdvancements()
```

**Purpose**: Gets all advancements for which the player has been rewarded.

**Returns**: Unmodifiable set of advancement identifiers

**Example**:

```java
Set<String> rewarded = data.getRewardedAdvancements();
LOGGER.info("Player has been rewarded for {} advancements", rewarded.size());
```

#### Serialization

##### toNbt

```java
public NbtCompound toNbt()
```

**Purpose**: Serializes progression data to NBT format for persistence.

**Returns**: NbtCompound containing all progression data

**Format**:

```
{
    "availableCredits": int,
    "rewardedAdvancements": [string array]
}
```

**Example**:

```java
NbtCompound nbt = data.toNbt();
worldData.put(playerId.toString(), nbt);
```

##### fromNbt

```java
public static PlayerProgressionData fromNbt(NbtCompound nbt)
```

**Purpose**: Deserializes progression data from NBT format.

**Parameters**:

- `nbt`: The NBT compound to deserialize

**Returns**: PlayerProgressionData instance

**Example**:

```java
NbtCompound nbt = worldData.getCompound(playerId.toString());
PlayerProgressionData data = PlayerProgressionData.fromNbt(nbt);
```

#### Deserialization Setters

##### setAvailableCredits

```java
public void setAvailableCredits(int credits)
```

**Purpose**: Sets the available credit balance. Used during deserialization.

**Parameters**:

- `credits`: The credit amount to set

**Note**: This bypasses validation. Only use during deserialization.

##### addRewardedAdvancement

```java
public void addRewardedAdvancement(String advancementId)
```

**Purpose**: Adds an advancement to the rewarded set. Used during deserialization.

**Parameters**:

- `advancementId`: The advancement identifier

---

### NotificationManager

Handles player notifications for credit awards.

#### Constructor

```java
public NotificationManager()
```

Creates manager with notifications enabled by default.

#### Notification Methods

##### notifyCreditAwarded

```java
public void notifyCreditAwarded(
    ServerPlayerEntity player,
    AdvancementEntry advancement,
    int creditsAwarded
)
```

**Purpose**: Sends notification when credits are awarded.

**Parameters**:

- `player`: The player to notify
- `advancement`: The completed advancement
- `creditsAwarded`: Number of credits awarded

**Behavior**:

- Sends formatted chat message with advancement name and credit count
- Plays experience orb pickup sound
- Only if notifications are enabled

**Example**:

```java
notificationManager.notifyCreditAwarded(player, advancement, 5);
// Player sees: "✓ Completed [Advancement Name] (+5 credits)"
```

##### notifyCreditsAvailable

```java
public void notifyCreditsAvailable(ServerPlayerEntity player, int creditsAvailable)
```

**Purpose**: Sends notification about available credit balance.

**Parameters**:

- `player`: The player to notify
- `creditsAvailable`: Current credit balance

**Example**:

```java
notificationManager.notifyCreditsAvailable(player, 10);
// Player sees: "You have 10 unlock credits available"
```

#### Configuration

##### setNotificationsEnabled

```java
public void setNotificationsEnabled(boolean enabled)
```

**Purpose**: Enables or disables notifications.

**Parameters**:

- `enabled`: true to enable, false to disable

**Example**:

```java
notificationManager.setNotificationsEnabled(false);
```

##### isNotificationsEnabled

```java
public boolean isNotificationsEnabled()
```

**Purpose**: Checks if notifications are currently enabled.

**Returns**: true if enabled, false otherwise

#### Utility Methods

##### formatIdentifier (static)

```java
public static String formatIdentifier(String identifier)
```

**Purpose**: Formats advancement identifiers into human-readable names.

**Parameters**:

- `identifier`: Raw identifier (e.g., "mine_diamond")

**Returns**: Formatted string (e.g., "Mine Diamond")

**Behavior**:

- Converts underscores to spaces
- Capitalizes each word

**Example**:

```java
String formatted = NotificationManager.formatIdentifier("mine_diamond");
// Returns: "Mine Diamond"

String formatted2 = NotificationManager.formatIdentifier("obtain_ancient_debris");
// Returns: "Obtain Ancient Debris"
```

---

## Configuration System

### AdvancementRewardConfig

Configuration data class for the advancement credit system.

#### Properties

```java
private int defaultCredits;                    // Default: 1
private Map<String, Integer> customRewards;    // Default: empty
private List<String> blacklist;                // Default: empty
private boolean enableNotifications;           // Default: true
```

#### Example Configuration

```json
{
  "defaultCredits": 1,
  "customRewards": {
    "minecraft:end/kill_dragon": 10,
    "minecraft:nether/obtain_ancient_debris": 5,
    "minecraft:story/mine_diamond": 2
  },
  "blacklist": ["minecraft:story/root", "minecraft:story/craft_workbench"],
  "enableNotifications": true
}
```

#### Loading Configuration

```java
// During mod initialization
AdvancementRewardConfig config = ConfigManager.loadAdvancementRewardConfig();
advancementCreditManager.loadConfig(config);
```

---

## Usage Examples

### Basic Setup

```java
public class ChunkLockedMod implements ModInitializer {
    private AdvancementCreditManager creditManager;

    @Override
    public void onInitialize() {
        creditManager = new AdvancementCreditManager();

        // Load configuration
        AdvancementRewardConfig config = ConfigManager.loadAdvancementRewardConfig();
        creditManager.loadConfig(config);

        // Register advancement callback
        AdvancementCompletedCallback.register((player, advancement, criterionName) -> {
            creditManager.onAdvancementCompleted(player, advancement, criterionName);
        });
    }
}
```

### Checking Player Credits

```java
public void checkPlayerCredits(ServerPlayerEntity player) {
    PlayerProgressionData data = creditManager.getPlayerData(player);
    int credits = data.getAvailableCredits();

    player.sendMessage(Text.literal("You have " + credits + " unlock credits"));
}
```

### Spending Credits for Chunk Unlock

```java
public boolean unlockChunk(ServerPlayerEntity player, ChunkPos chunkPos) {
    PlayerProgressionData data = creditManager.getPlayerData(player);

    if (data.spendCredits(1)) {
        // Unlock the chunk
        chunkManager.unlockChunk(player.getUuid(), chunkPos);
        player.sendMessage(Text.literal("Chunk unlocked!"));
        return true;
    } else {
        player.sendMessage(Text.literal("Not enough credits. Complete advancements to earn more."));
        return false;
    }
}
```

### Custom Reward Configuration

```java
// Set up custom rewards programmatically
creditManager.setCustomReward("minecraft:end/kill_dragon", 10);
creditManager.setCustomReward("minecraft:nether/obtain_ancient_debris", 5);
creditManager.setCustomReward("minecraft:story/mine_diamond", 2);

// Blacklist tutorial advancements
creditManager.addBlacklistedAdvancement("minecraft:story/root");
creditManager.addBlacklistedAdvancement("minecraft:story/craft_workbench");
```

### Persistence

```java
// Save player data
public void savePlayerData(UUID playerId) {
    PlayerProgressionData data = creditManager.getPlayerData(playerId);
    NbtCompound nbt = data.toNbt();

    // Store in world data
    worldData.put(playerId.toString(), nbt);
}

// Load player data
public void loadPlayerData(UUID playerId, NbtCompound nbt) {
    PlayerProgressionData data = PlayerProgressionData.fromNbt(nbt);
    creditManager.loadPlayerData(playerId, data);
}
```

---

## Performance Considerations

### Memory Usage

- **PlayerProgressionData**: ~100 bytes per player base + ~50 bytes per rewarded advancement
- **AdvancementCreditManager**: ~1KB base + config size
- **Typical server with 20 players**: ~5-10KB total

### Computational Cost

- **Advancement completion**: O(1) lookup time
- **Credit queries**: O(1) map lookups
- **Persistence**: O(n) where n = number of rewarded advancements

### Best Practices

1. **Config loading**: Load once at startup, reload only when needed
2. **Player data caching**: Manager maintains in-memory cache
3. **Notification throttling**: Already built-in (one per advancement)
4. **Recipe filtering**: Prevents spam from recipe discoveries

---

## Error Handling

### Validation

- **Negative credits**: Throws IllegalArgumentException
- **Null parameters**: NPE (fail-fast design)
- **Missing config**: Falls back to defaults

### Edge Cases

- **Duplicate rewards**: Prevented by advancement tracking
- **Recipe advancements**: Automatically filtered out
- **Blacklisted advancements**: Return 0 credits
- **Insufficient credits**: spendCredits() returns false

---

## Thread Safety

⚠️ **Not Thread-Safe**: All methods must be called on the server thread.

Minecraft's server architecture is single-threaded for game logic. This system follows that pattern and should only be accessed from:

- Server tick events
- Command execution
- Advancement completion callbacks

---

## Testing

### Unit Tests

- ✅ **PlayerProgressionData**: 100% coverage (32 tests)
- ✅ **AdvancementCreditManager**: 66% coverage (37 tests)
- ✅ **NotificationManager**: 27% coverage (22 tests)

### Manual Tests

- See `manual-tests/AdvancementIntegration.md`
- In-game verification scenarios
- Multiplayer synchronization

---

## Known Limitations

1. **Recipe Advancements**: Pattern matching on `:recipes/` - may miss custom patterns
2. **Advancement Revocation**: Credits not automatically revoked if advancement removed
3. **Server-Side Only**: No client prediction for credit awards
4. **Single Threaded**: No concurrent access support

---

## Future Enhancements

- [ ] Support for advancement revocation with credit refunds
- [ ] Credit transaction history tracking
- [ ] Achievement milestones (bonus credits every N advancements)
- [ ] Team/shared credit pools for multiplayer
- [ ] Custom advancement categories with different credit values

---

## See Also

- [Configuration System Documentation](ConfigurationSystem.md)
- [Network Synchronization Documentation](NetworkSynchronization.md)
- [Manual Testing Guide](../manual-tests/AdvancementIntegration.md)
- [Phase 7 Testing Status](../docs/testing/Phase7-Status.md)
