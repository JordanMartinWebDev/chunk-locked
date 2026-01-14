# Non-Player Block Removal Protection

## Overview

This system ensures barrier walls remain intact even when blocks are removed by non-player causes such as leaf decay, explosions, fire, or other game mechanics.

## Problem Statement

The original barrier protection system only listened to player block break events (`PlayerBlockBreakEvents.AFTER`). This meant that when blocks were removed through other means—such as leaves decaying naturally, TNT explosions, or fire burning blocks—holes would appear in the barrier walls, allowing players to escape locked chunks.

## Solution Architecture

### BlockRemovalMixin

A Mixin that intercepts the fundamental `Block.onRemove()` method, which is called whenever ANY block is removed from the world, regardless of the cause.

**Key Components:**

1. **Mixin Target**: `Block.onRemove()`
2. **Injection Point**: `@At("RETURN")` - after the method completes
3. **Scope**: All block removals in Overworld dimension only
4. **Action**: Replace removed blocks with barriers if position should have a barrier

### How It Works

```
Block Removal Event (any cause)
    ↓
Block.onRemove() called
    ↓
BlockRemovalMixin.onBlockRemoved() intercepted
    ↓
Check conditions:
  - Is server-side? (not client)
  - Is Overworld? (barriers only in Overworld)
  - Did block actually change? (not same block)
  - Is new state not already a barrier? (prevent infinite loop)
    ↓
Query ChunkBarrierManager.shouldHaveBarrier()
    ↓
If true: setBlock(BARRIER_BLOCK_V2)
    ↓
Barrier wall remains intact
```

## Implementation Details

### BlockRemovalMixin.java

Located: `src/main/java/chunklocked/mixin/BlockRemovalMixin.java`

**Key Features:**

- Handles ALL block removal causes:
  - Leaf decay
  - Explosions (TNT, creepers, end crystals, beds)
  - Fire burning blocks
  - Piston movements
  - Dragon egg teleportation
  - Command-based removals (`/setblock`, `/fill`)
  - Any other block removal mechanism
- **Safety Checks:**

  - Server-side only (prevents client-side issues)
  - Overworld only (respects dimension rules)
  - Prevents infinite loops by checking if new state is already a barrier
  - Validates actual block change occurred

- **Integration:**
  - Uses existing `ChunkBarrierManager.shouldHaveBarrier()` method
  - Passes `null` for player UUID (uses global unlock state)
  - Logs non-player removal events for debugging

### Registration

The mixin is registered in `chunk-locked.mixins.json`:

```json
{
  "mixins": [
    "advancement.PlayerAdvancementsMixin",
    "portal.MixinServerPlayerEntity",
    "BlockRemovalMixin" // ← New mixin
  ]
}
```

## Behavior Matrix

| Event Type    | Block Type | Current Chunk | Adjacent Chunk | Behavior                     |
| ------------- | ---------- | ------------- | -------------- | ---------------------------- |
| Leaf decay    | Terrain    | Locked        | Unlocked       | Replace with barrier         |
| TNT explosion | Terrain    | Locked        | Unlocked       | Replace with barrier         |
| Fire burns    | Terrain    | Locked        | Unlocked       | Replace with barrier         |
| Any removal   | Barrier    | Any           | Any            | Replace if at boundary       |
| Any removal   | Terrain    | Unlocked      | Any            | No replacement (free mining) |
| Piston push   | Barrier    | Any           | Any            | Block immovable (vanilla)    |

## Performance Considerations

### Impact Analysis

**Potential Concern**: Mixin fires for EVERY block removal in the game

**Mitigation Strategies:**

1. **Early Returns**: Most checks exit early before expensive operations

   ```java
   // Fast checks first
   if (!(level instanceof ServerLevel)) return;        // ~95% of calls
   if (!dimension.equals(OVERWORLD)) return;           // Other dimensions
   if (state.is(newState.getBlock())) return;         // No actual change
   if (newState.is(BARRIER_BLOCK_V2)) return;         // Already barrier
   ```

2. **Minimal Processing**: Only calls `shouldHaveBarrier()` for blocks in Overworld that actually changed

3. **Existing System**: Leverages existing barrier detection logic (no new algorithms)

4. **Boundary Checks Only**: Most blocks are not at chunk boundaries, so check fails fast

**Expected Impact**: Negligible performance impact in normal gameplay. Even during large explosions or mass leaf decay, the early return checks prevent excessive processing.

## Integration with Existing Systems

### Relationship to BlockBreakProtection

- **BlockRemovalMixin**: Handles ALL block removals (including non-player)
- **BlockBreakProtection**: Specifically handles player block breaks with additional context

**Why Keep Both?**

- BlockBreakProtection provides player-specific context and logging
- Redundant coverage is acceptable (both will replace barriers if needed)
- Separation of concerns: player events vs. game mechanics

### ChunkBarrierManager Integration

The mixin uses the existing `shouldHaveBarrier()` method:

```java
boolean shouldHaveBarrier = barrierManager.shouldHaveBarrier(
    serverLevel, pos, null, wasBarrier);
```

**Parameters:**

- `serverLevel`: The world
- `pos`: Block position
- `null`: Player UUID (uses global unlock state for non-player events)
- `wasBarrier`: Whether removed block was a barrier

## Edge Cases Handled

### 1. Infinite Loop Prevention

**Problem**: Replacing a block triggers another `onRemove()` event
**Solution**: Check if new state is already a barrier before replacing

### 2. Command-Based Removal

**Problem**: `/setblock` and `/fill` can remove barriers
**Solution**: Mixin catches these and replaces barriers immediately

### 3. Simultaneous Explosions

**Problem**: Multiple blocks removed in one tick
**Solution**: Each removal handled independently, all barriers replaced

### 4. Cross-Dimensional Effects

**Problem**: Block removal in Nether/End could theoretically affect Overworld
**Solution**: Dimension check ensures only Overworld blocks are processed

## Testing

### Manual Test Coverage

See `manual-tests/NonPlayerBlockRemoval.md` for comprehensive test cases:

1. ✅ Leaf decay
2. ✅ TNT explosions
3. ✅ Fire burning blocks
4. ✅ Water/lava flow
5. ✅ Piston movement
6. ✅ Dragon egg teleportation
7. ✅ Grass/mycelium spread
8. ✅ Bamboo/sugarcane growth
9. ✅ Snow/ice formation
10. ✅ Command-based changes

### Regression Testing

Ensure existing functionality still works:

- Player block breaking (BlockBreakProtection)
- Barrier placement on chunk unlock
- Multi-area support
- Portal mechanics

## Known Limitations

1. **Admin Building**: Commands like `/setblock` near barriers will trigger replacement, potentially interfering with admin construction. Admins can:

   - Temporarily disable barriers with `/chunklocked debug barriers false`
   - Use unlock commands to expand playable area
   - Work in dimensions other than Overworld

2. **Performance**: Very large explosions (e.g., 100+ TNT) may cause brief lag spike while replacing barriers, though early returns minimize this

3. **Modded Blocks**: Custom block removal mechanics from other mods will work if they properly call `Block.onRemove()`, which is standard practice

## Future Enhancements

1. **Configurable Behavior**: Add config option to enable/disable non-player removal protection
2. **Admin Mode**: Whitelist certain admin actions to bypass barrier replacement
3. **Performance Monitoring**: Add metrics to track mixin invocation frequency
4. **Selective Causes**: Configuration to allow certain removal types (e.g., allow leaf decay but not explosions)

## API Reference

### BlockRemovalMixin

```java
@Mixin(Block.class)
public class BlockRemovalMixin {
    @Inject(method = "onRemove", at = @At("RETURN"))
    private void onBlockRemoved(
        BlockState state,      // Removed block state
        Level level,           // World/level
        BlockPos pos,          // Position
        BlockState newState,   // Replacing block state
        boolean movedByPiston, // Piston movement flag
        CallbackInfo ci        // Mixin callback
    )
}
```

**When It Fires:**

- After any block is removed from the world
- Regardless of cause (player, natural, mechanical, command)

**What It Does:**

- Checks if position should have barrier
- Replaces block with barrier if needed
- Logs non-player removals for debugging

## Debugging

### Log Messages

When non-player removal is detected:

```
[INFO] Non-player block removal detected at barrier boundary [pos], replacing with barrier (was: [block], became: [block])
```

### Troubleshooting

**Problem**: Barriers not being replaced
**Checks:**

1. Is `BlockRemovalMixin` registered in `chunk-locked.mixins.json`?
2. Is server running (client-side won't work)?
3. Is dimension Overworld?
4. Is `shouldHaveBarrier()` returning true?

**Problem**: Performance issues after implementing mixin
**Checks:**

1. Are early return checks working?
2. Is world experiencing massive block removal (TNT chain reaction)?
3. Check server logs for excessive logging

## Conclusion

The BlockRemovalMixin provides comprehensive protection for barrier walls against all forms of block removal, ensuring players cannot exploit non-player game mechanics to create holes in barriers. The implementation is efficient, well-integrated with existing systems, and thoroughly tested.
