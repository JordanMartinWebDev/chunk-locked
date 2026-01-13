# Chunk Locked Commands API

## Overview

Complete reference for all `/chunklocked` commands in the Chunk Locked mod.

---

## Player Commands

### `/chunklocked credits`

Display your current unlock credits.

**Usage**:

```
/chunklocked credits
```

**Output Example**:

```
You have 5 unlock credits (completed 12 advancements)
```

**Permissions**: Everyone

---

### `/chunklocked unlockfacing`

Unlock the chunk in the direction you're facing using intelligent raycasting.

**Usage**:

```
/chunklocked unlockfacing
```

**How It Works**:

1. Casts a ray from your eyes along your look direction
2. Detects the first locked chunk boundary (up to 256 blocks away)
3. Unlocks that chunk if you have credits
4. Updates barriers automatically

**Output Examples**:

- Success: `Unlocked chunk North [7, -1]! (4 credits remaining)`
- No boundary: `No locked chunk boundary found in that direction!`
- Already unlocked: `The chunk North of you [7, -1] is already unlocked!`
- No credits: `Not enough credits! Complete more advancements.`
- Not adjacent: `Chunk must be adjacent to your unlocked area!`

**Cost**: 1 credit per chunk

**Permissions**: Everyone (but requires credits)

**Note**: Direction detection is intelligent:

- Skips internal boundaries between unlocked chunks
- Works with 2x2+ spawn areas correctly
- Identifies exact direction (North/South/East/West)

---

## Admin Commands

### `/chunklocked unlock <chunkX> <chunkZ>`

Directly unlock a specific chunk without spending credits.

**Usage**:

```
/chunklocked unlock 7 -1
```

**Output Example**:

```
Unlocked chunk [7, -1]!
```

**Permissions**: OP level 2+

**Cost**: Free (admin command)

---

### `/chunklocked givecredits <amount>`

Give yourself unlock credits for testing.

**Usage**:

```
/chunklocked givecredits 5
```

**Output Example**:

```
Given 5 credits! (0 → 5)
```

**Permissions**: OP level 2+

**Validation**:

- Amount must be 1 or greater
- Max value limited by integer range (2,147,483,647)

---

### `/chunklocked givecredits <player> <amount>`

Give another player unlock credits.

**Usage**:

```
/chunklocked givecredits Player205 3
```

**Output**:

- Executor: `Gave 3 credits to Player205 (0 → 3)`
- Target: `You received 3 credits (0 → 3)`

**Permissions**: OP level 2+

**Validation**:

- Player must be online
- Amount must be 1 or greater

---

### `/chunklocked debugbarriers`

Display detailed information about unlocked chunks, frontier chunks, and barrier configuration.

**Usage**:

```
/chunklocked debugbarriers
```

**Output Example**:

```
=== Chunk Barrier Debug ===
Unlocked chunks: 4
  [8, -1] (world: 128 to 143, -16 to -1)
  [8, 0] (world: 128 to 143, 0 to 15)
  [7, -1] (world: 112 to 127, -16 to -1)
  [7, 0] (world: 112 to 127, 0 to 15)

Frontier (locked adjacent) chunks: 8
  [9, -1] barriers: W
  [8, -2] barriers: S
  [6, 0] barriers: E
  [7, -2] barriers: S
  [8, 1] barriers: N
  [6, -1] barriers: E
  [7, 1] barriers: N
  [9, 0] barriers: W
```

**Information Provided**:

- **Unlocked chunks**: All chunks player can access (includes world coordinates)
- **Frontier chunks**: All locked chunks adjacent to unlocked area
- **Barrier directions**: Which sides have barriers (N/S/E/W)

**Permissions**: OP level 2+

**Use Cases**:

- Verify barrier placement is correct
- Debug frontier calculation
- Check unlock status
- Identify configuration issues

---

### `/chunklocked help`

Display help text for all commands.

**Usage**:

```
/chunklocked help
```

**Output**: Brief description of each command

**Permissions**: Everyone

---

## Command Syntax & Examples

### Example 1: Player Progression

```
# Player joins, gets 2x2 spawn area automatically
# Player completes advancement, gets 1 credit
/chunklocked credits
> You have 1 unlock credit

# Player looks at chunk to north and unlocks
/chunklocked unlockfacing
> Unlocked chunk North [7, -2]!

# Check new configuration
/chunklocked debugbarriers
> Shows updated frontier and barriers
```

### Example 2: Admin Testing

```
# Give yourself credits
/chunklocked givecredits 10

# Give other player credits for testing
/chunklocked givecredits TestPlayer 5

# Unlock specific chunks
/chunklocked unlock 7 -3
/chunklocked unlock 8 -2

# Verify barrier configuration
/chunklocked debugbarriers
```

### Example 3: Server Setup

```
# Spawn players with starting credits
/chunklocked givecredits @a 5

# Unlock chunks for new players
/chunklocked unlock 0 0
/chunklocked unlock 1 0
/chunklocked unlock 0 1
/chunklocked unlock 1 1
```

---

## Coordinate Systems

### Chunk Coordinates

- X: Chunk column (16 blocks per chunk)
- Z: Chunk row (16 blocks per chunk)

**Conversion**:

- Block X → Chunk X: `block_x >> 4` or `block_x / 16`
- Block Z → Chunk Z: `block_z >> 4` or `block_z / 16`

**Example**: Player at block [118, 4.5, -4] is in chunk [7, -1]

### Cardinal Directions

- **North**: Negative Z (-Z direction)
- **South**: Positive Z (+Z direction)
- **East**: Positive X (+X direction)
- **West**: Negative X (-X direction)

---

## Raycasting Behavior

### How `/chunklocked unlockfacing` Works

1. **Starting Point**: Player's eye height
2. **Direction**: Calculated from yaw and pitch
3. **Range**: Up to 256 blocks
4. **Step Size**: 0.5 blocks
5. **Detection**: First locked/unlocked boundary

### Yaw Reference

```
      North (180°)
           ▲
           │
West ◄─────┼─────► East
(90°)      │      (270°)
           │
           ▼
      South (0°)
```

### Pitch Reference

- 0° = Looking straight ahead (horizontal)
- 90° = Looking straight up
- -90° = Looking straight down

### Examples

- Facing North (yaw 180°, pitch 0°): Unlocks chunk to the north
- Facing Northeast (yaw 225°, pitch 0°): Detects boundary in NE direction
- Looking up while facing north: Still detects north boundary (pitch ignored for chunk detection)

---

## Permissions & Requirements

### `/chunklocked credits`

- ✅ Permission: None (everyone)
- ✅ Requires: Joined server
- ✅ Cooldown: None

### `/chunklocked unlockfacing`

- ✅ Permission: None (everyone)
- ✅ Requires: At least 1 credit
- ✅ Requires: Locked chunk adjacent to unlocked area
- ✅ Cooldown: None

### `/chunklocked unlock`

- ✅ Permission: OP level 2+
- ✅ Requires: Valid chunk coordinates
- ✅ Cooldown: None

### `/chunklocked givecredits <amount>`

- ✅ Permission: OP level 2+
- ✅ Requires: Valid amount (≥1)
- ✅ Cooldown: None

### `/chunklocked givecredits <player> <amount>`

- ✅ Permission: OP level 2+
- ✅ Requires: Player online
- ✅ Requires: Valid amount (≥1)
- ✅ Cooldown: None

### `/chunklocked debugbarriers`

- ✅ Permission: OP level 2+
- ✅ Requires: None
- ✅ Cooldown: None

### `/chunklocked help`

- ✅ Permission: None (everyone)
- ✅ Requires: None
- ✅ Cooldown: None

---

## Error Messages & Solutions

| Error                                               | Cause                                   | Solution                                  |
| --------------------------------------------------- | --------------------------------------- | ----------------------------------------- |
| "No locked chunk boundary found in that direction!" | No locked chunks within 256 blocks      | Face a locked chunk boundary, move closer |
| "The chunk [X, Z] is already unlocked!"             | Chunk already unlocked                  | Face a different direction                |
| "Not enough credits! Complete more advancements."   | Player has 0 credits                    | Complete advancements or ask admin        |
| "Chunk must be adjacent to your unlocked area!"     | Target chunk not touching unlocked area | Unlock adjacent chunks first              |
| "Failed to unlock chunk!"                           | Server error                            | Report to admin                           |

---

## Tips & Best Practices

### For Players

1. **Use `/chunklocked credits`** to check progress
2. **Look at barriers** before using `/chunklocked unlockfacing` for best results
3. **Face chunk boundaries** squarely for reliable detection
4. **Expand systematically** outward from spawn area

### For Admins

1. **Use `/chunklocked debugbarriers`** to verify setup
2. **Give credits sparingly** to prevent progression issues
3. **Use `/chunklocked unlock`** only for one-time world setup
4. **Restart after major unlock changes** to ensure barriers sync

### For Server Setup

```bash
# Give all players starting credits
/chunklocked givecredits @a[tag=newplayer] 5

# Unlock initial spawn area (before players join)
/chunklocked unlock 0 0
/chunklocked unlock 1 0
/chunklocked unlock 0 1
/chunklocked unlock 1 1
```

---

## Version History

### v1.0.0 (Current)

- ✅ `/chunklocked credits` - Player credit check
- ✅ `/chunklocked unlockfacing` - Intelligent chunk unlocking
- ✅ `/chunklocked unlock` - Admin direct unlock
- ✅ `/chunklocked givecredits` - Dual format credit giving
- ✅ `/chunklocked debugbarriers` - Debug frontier config
- ✅ `/chunklocked help` - Help text

---

## Support

For issues or questions:

1. Run `/chunklocked debugbarriers` to check configuration
2. Verify player has credits with `/chunklocked credits`
3. Check server logs for errors
4. Consult manual test cases in documentation
