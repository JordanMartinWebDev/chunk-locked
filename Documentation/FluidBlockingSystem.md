# Fluid Blocking System

## Overview

The barrier blocks used in ChunkLocked must resist destruction by flowing water and lava while maintaining their selective collision properties (blocking players but allowing mobs to pass through).

## Problem

Water and lava in Minecraft have special flow mechanics that can destroy certain blocks. When water or lava flows into a space occupied by a "replaceable" or "non-solid" block, the fluid destroys that block and replaces it with fluid.

Initial barrier implementations were being destroyed by flowing fluids because:

1. The barriers had selective collision (empty collision shape for non-players)
2. Minecraft's fluid system was treating them as "passable" or "replaceable"
3. Adjacent fluid sources would flow into barrier positions and destroy them

## Solution

The fluid blocking system uses a combination of block properties and method overrides to make barriers appear solid to fluids while maintaining selective collision for entities.

### Key Components

#### 1. Block Properties (Chunklocked.java)

```java
new BarrierBlockV2(BlockBehaviour.Properties.of()
    .setId(blockKey)
    .mapColor(MapColor.NONE)
    .strength(-1.0f, 3600000.0f)        // Unbreakable
    .noOcclusion()                       // Transparent rendering
    .noLootTable()                       // No drops
    .pushReaction(PushReaction.BLOCK)    // Can't be pushed
    .forceSolidOn()                      // ⭐ Critical: Treated as solid for fluids
    .isRedstoneConductor((state, level, pos) -> true));
```

**Critical Property: `.forceSolidOn()`**

- Forces Minecraft to treat this block as solid for various internal checks
- Essential for preventing fluid flow through the block
- Does NOT affect collision detection (handled separately)

#### 2. Shape Methods (BarrierBlockV2.java)

**`getShape()` - Fluid Detection**

```java
@Override
public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.block(); // Full cube
}
```

- Returns a full cube shape (1x1x1)
- Fluids check this method to determine if they can flow into a space
- Returning `Shapes.block()` tells fluids this is a solid block

**`getCollisionShape()` - Entity Collision**

```java
@Override
public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    if (entity instanceof Player) {
        return Shapes.block(); // Block players
    }
    return Shapes.empty(); // Mobs pass through
}
```

- Returns different shapes based on entity type
- Players get full collision, mobs get empty collision
- Fluids don't use this method - they use `getShape()` instead

**`getVisualShape()` - Mob Pathfinding**

```java
@Override
public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
}
```

- Returns empty shape for pathfinding
- Allows mobs to pathfind through barriers

#### 3. Fluid State Override

**`getFluidState()` - Prevent Waterlogging**

```java
@Override
public FluidState getFluidState(BlockState state) {
    return Fluids.EMPTY.defaultFluidState();
}
```

- Always returns empty fluid state
- Prevents the block from being waterloggable
- Ensures fluids cannot coexist with the barrier

#### 4. Replacement Prevention

**`canBeReplaced()` - Block Replacement**

```java
@Override
public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    return false;
}
```

- Prevents other blocks from replacing barriers
- Additional safeguard against fluid replacement

### Block Placement Flags

When placing barriers, we use specific flags to notify adjacent fluids:

```java
world.setBlock(pos, BARRIER_BLOCK_V2.defaultBlockState(), 2 | 1);
// Flag 2 = UPDATE_CLIENTS (sync to clients)
// Flag 1 = NOTIFY_NEIGHBORS (notify adjacent blocks)
```

**Why Neighbor Notification Matters:**

- Flag 1 tells adjacent fluid blocks that a solid block was placed
- Fluids recalculate their flow state when notified
- Without notification, fluids might not recognize the barrier as solid

## How It Works

### Fluid Flow Detection Process

1. **Fluid Tries to Flow**: Water/lava checks if it can flow into adjacent space
2. **Shape Check**: Calls `getShape()` on target block
3. **Solidity Check**: Uses `.forceSolidOn()` property
4. **Flow Decision**:
   - If `getShape()` returns full cube AND block is solid → fluid stops
   - If `getShape()` returns empty OR block not solid → fluid flows through

### Why Selective Collision Works

The key insight is that **different Minecraft systems check different methods**:

| System            | Method Checked                   | Barrier Behavior                |
| ----------------- | -------------------------------- | ------------------------------- |
| Fluid Flow        | `getShape()` + `.forceSolidOn()` | Full cube → Fluids blocked      |
| Entity Collision  | `getCollisionShape()`            | Players: full cube, Mobs: empty |
| Mob Pathfinding   | `getVisualShape()`               | Empty → Mobs pathfind through   |
| Block Replacement | `canBeReplaced()`                | false → Can't be replaced       |
| Waterlogging      | `getFluidState()`                | Empty → Not waterloggable       |

By returning different values for different methods, we achieve:

- ✅ Players blocked (collision)
- ✅ Mobs pass through (collision)
- ✅ Mobs pathfind through (visual shape)
- ✅ Fluids blocked (shape + solid property)

## Testing

### Manual Testing

1. **Water Resistance**: Place water bucket next to barrier wall
   - Expected: Water flows around barrier, doesn't destroy it
2. **Lava Resistance**: Place lava bucket next to barrier wall

   - Expected: Lava flows around barrier, doesn't destroy it

3. **Mob Pathfinding**: Spawn mob on one side, tempt to other side

   - Expected: Mob walks through barrier to reach target

4. **Player Collision**: Walk toward barrier
   - Expected: Player is blocked, cannot walk through

### Automated Testing

See `BarrierBlockFluidTest.java` for unit tests covering:

- Shape method return values
- Fluid state checks
- Replacement prevention
- Property validation

## Troubleshooting

### Problem: Fluids still destroying barriers

**Check:**

1. Is `.forceSolidOn()` in block properties?
2. Does `getShape()` return `Shapes.block()`?
3. Are you using flag `2 | 1` when placing blocks?
4. Does `getFluidState()` return empty?

### Problem: Mobs can't pathfind through barriers

**Check:**

1. Does `getVisualShape()` return `Shapes.empty()`?
2. Is `isPathfindable()` NOT overridden (or returns true)?

### Problem: Players can walk through barriers

**Check:**

1. Does `getCollisionShape()` check for Player entity type?
2. Does it return `Shapes.block()` for players?

## Performance Considerations

- Shape objects are cached as static fields to avoid allocation overhead
- Method calls are minimal and lightweight
- Fluid updates only occur when blocks change (not per-tick)

## Future Improvements

Potential enhancements:

- [ ] Add debug mode to visualize which systems check which methods
- [ ] Performance profiling of fluid interaction in large barrier walls
- [ ] Consider forge-specific fluid APIs if porting to Forge

## References

- `BarrierBlockV2.java` - Main implementation
- `Chunklocked.java` - Block registration with properties
- `ChunkBarrierManager.java` - Barrier placement with flags
- `BatchBarrierOperations.java` - Batch placement logic

## Change Log

### 2026-01-15 - Initial Implementation

- Added `.forceSolidOn()` property to block registration
- Implemented `getShape()` returning full cube for fluid blocking
- Implemented `getFluidState()` returning empty to prevent waterlogging
- Changed block placement flags from `2 | 16` to `2 | 1` for neighbor notification
- Removed `isPathfindable()` override to restore mob pathfinding
- Confirmed working: Fluids blocked, mobs pass through and pathfind correctly
