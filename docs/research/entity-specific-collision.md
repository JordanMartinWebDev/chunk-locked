# Entity-Specific Collision Research

**Date**: January 12, 2026  
**Status**: Research - Not yet implemented  
**Goal**: Make BarrierBlockV2 block only players while allowing mobs, items, and fluids to pass through

---

## Overview

We want barrier blocks that:
- **Block players** - solid collision, cannot walk through
- **Allow mobs** - zombies, skeletons, etc. can walk through
- **Allow items** - dropped items can pass through
- **Allow fluids** - water and lava can flow through the block

This would make barriers less intrusive for non-player entities while still preventing players from entering locked chunks.

---

## Technical Approach

### Collision Shape Method

Minecraft uses `getCollisionShape()` to determine if an entity collides with a block. The method receives a `CollisionContext` parameter that can tell us **what** is colliding.

**Key Classes** (Mojang Mappings):
- `VoxelShape` - Defines the collision box shape
- `CollisionContext` - Abstract context for collision checks
- `EntityCollisionContext` - Concrete implementation wrapping an `Entity`
- `Shapes.block()` - Full cube shape (1x1x1)
- `Shapes.empty()` - No collision shape

### Implementation Code

```java
package chunkloaded.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Semi-transparent barrier block with player-only collision.
 * 
 * Players are blocked by a full collision box, but mobs, items, and fluids
 * can pass through as if the block doesn't exist.
 */
public class BarrierBlockV2 extends Block {
  
  // Cache collision shapes
  private static final VoxelShape PLAYER_COLLISION = Shapes.block(); // Full cube
  private static final VoxelShape NO_COLLISION = Shapes.empty();    // Empty shape
  
  public BarrierBlockV2(BlockBehaviour.Properties settings) {
    super(settings);
  }
  
  /**
   * Return collision shape based on what entity is colliding.
   * 
   * @param state The block state
   * @param level The world
   * @param pos The block position
   * @param context Collision context containing entity information
   * @return Full cube for players, empty for everything else
   */
  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level, 
                                     BlockPos pos, CollisionContext context) {
    // Check if context contains entity information
    if (context instanceof EntityCollisionContext entityContext) {
      Entity entity = entityContext.getEntity();
      
      // Block players with full collision
      if (entity instanceof Player) {
        return PLAYER_COLLISION;
      }
    }
    
    // Everything else passes through (mobs, items, fluids)
    return NO_COLLISION;
  }
  
  // ... rest of existing methods (getShadeBrightness, propagatesSkylightDown, skipRendering)
}
```

---

## How It Works

### Collision Shape Checking

1. **Player approaches barrier**: 
   - `getCollisionShape()` called with `EntityCollisionContext` wrapping the player
   - Method detects `entity instanceof Player`
   - Returns `PLAYER_COLLISION` (full cube)
   - Player is blocked

2. **Zombie approaches barrier**:
   - `getCollisionShape()` called with `EntityCollisionContext` wrapping the zombie
   - Method checks entity type, not a player
   - Returns `NO_COLLISION` (empty shape)
   - Zombie walks through

3. **Water flows toward barrier**:
   - Fluid system checks collision shape
   - `getCollisionShape()` called (context may be empty or fluid-specific)
   - Returns `NO_COLLISION`
   - Water flows through the block

### Visual Shape vs Collision Shape

**Important distinction**:
- `getShape()` - Visual outline and selection box (what you see)
- `getCollisionShape()` - Physical collision (what you bump into)

We only override collision shape. The block still appears as a full cube visually, but collision behavior varies by entity.

---

## Vanilla Examples

Minecraft has several blocks with entity-specific collision:

### Powder Snow (`PowderSnowBlock`)
- Players and some mobs fall through (unless wearing leather boots)
- Other entities walk on top
- Fluids cannot flow through

### Scaffolding (`ScaffoldingBlock`)
- Players can climb up/down inside
- Mobs walk on top surface
- Different collision shapes based on context

### Cobweb (`WebBlock`)
- Slows entities inside
- No solid collision, but movement penalty
- Fluids flow through

### Structure Void (`StructureVoidBlock`)
- No collision at all
- Used for structure generation templates

---

## Testing Requirements

### Manual Tests to Perform

1. **Player Collision**
   - Walk into barrier as player → should be blocked
   - Try jumping over → should collide
   - Try sneaking through → should collide

2. **Mob Behavior**
   - Spawn zombie near barrier → should walk through
   - Lead passive mob through → should pass
   - Check if mobs pathfind through barriers correctly

3. **Fluid Flow**
   - Place water source next to barrier → should flow through
   - Place lava next to barrier → should flow through
   - Check if fluids stop at proper boundaries

4. **Item Drops**
   - Drop items on one side → should pass through to other side
   - Test with flowing water pushing items

5. **Projectiles**
   - Shoot arrows at barrier → should pass through
   - Test with snowballs, eggs, other projectiles

### Edge Cases

- **Riding entities**: What happens if player rides a boat/minecart through?
- **Push mechanics**: Can pistons push entities through?
- **Teleportation**: Does Ender Pearl pass through?
- **Redstone**: Can redstone signals pass through?

---

## Potential Issues

### 1. Mob AI and Pathfinding

**Problem**: Mobs might not pathfind through barriers even if they can physically walk through.

**Solution**: May need to override `getVisualShape()` or implement custom pathfinding behavior.

### 2. Player Riding Entities

**Problem**: Player on a boat might pass through if boat's collision is checked instead of player's.

**Solution**: Check if player is *controlling* the entity and block accordingly.

```java
if (entity instanceof Player) {
  return PLAYER_COLLISION;
}
// Also check if entity is controlled by a player
if (entity.getControllingPassenger() instanceof Player) {
  return PLAYER_COLLISION;
}
```

### 3. Fluid Behavior

**Problem**: Fluids might behave unexpectedly if they can flow through barriers.

**Observation needed**: Test if water creates infinite sources or behaves oddly.

### 4. Server/Client Desync

**Problem**: Collision is calculated on both client and server. Entity type checking must be deterministic.

**Solution**: Ensure `CollisionContext` works the same way on both sides.

---

## Implementation Plan

### Phase 1: Basic Implementation
- [ ] Add `getCollisionShape()` override to `BarrierBlockV2`
- [ ] Add required imports (`VoxelShape`, `Shapes`, `EntityCollisionContext`)
- [ ] Compile and test basic player collision

### Phase 2: Refinement
- [ ] Handle player-controlled entities (boats, minecarts)
- [ ] Test with various mob types
- [ ] Verify fluid flow works correctly

### Phase 3: Testing
- [ ] Complete all manual test cases
- [ ] Document any unexpected behaviors
- [ ] Create test world for demonstrations

### Phase 4: Documentation
- [ ] Update `BarrierBlockV2` Javadoc
- [ ] Add manual test cases to `manual-tests/`
- [ ] Update `Documentation/CoreSystems/` with collision behavior

---

## Configuration Consideration

**Future Enhancement**: Make collision behavior configurable?

```json
{
  "barrier_collision": {
    "block_players": true,
    "block_hostile_mobs": false,
    "block_passive_mobs": false,
    "allow_fluids": true,
    "allow_projectiles": true
  }
}
```

This would allow server admins to customize barrier behavior per their needs.

---

## Performance Notes

- **VoxelShape caching**: Using static `PLAYER_COLLISION` and `NO_COLLISION` means no shape allocation per collision check
- **Entity type check**: `instanceof Player` is very fast (single pointer check)
- **No additional data**: Doesn't require extra block state or NBT data

Performance impact should be negligible.

---

## Alternatives Considered

### 1. Entity Spawn Prevention
Instead of collision, just prevent mobs from spawning in locked chunks.
- **Pro**: Simpler implementation
- **Con**: Doesn't handle mobs wandering in from adjacent chunks

### 2. Damage/Teleport System
Allow entities through but damage/teleport players.
- **Pro**: More lenient barrier
- **Con**: Less intuitive, requires teleportation logic

### 3. Per-Entity-Type Configuration
Check entity registry and have whitelist/blacklist.
- **Pro**: Maximum flexibility
- **Con**: Complex configuration, harder to maintain

**Chosen Approach**: Player-only collision is the cleanest solution that meets requirements.

---

## References

### Mojang Mapping Classes
- `net.minecraft.world.phys.shapes.VoxelShape`
- `net.minecraft.world.phys.shapes.Shapes`
- `net.minecraft.world.phys.shapes.CollisionContext`
- `net.minecraft.world.phys.shapes.EntityCollisionContext`
- `net.minecraft.world.entity.Entity`
- `net.minecraft.world.entity.player.Player`

### Vanilla Block Examples
- `PowderSnowBlock` - Player-specific collision
- `ScaffoldingBlock` - Context-dependent shapes
- `WebBlock` - Movement effects without solid collision
- `BarrierBlock` - Baseline (blocks everything)

---

## Next Steps

1. **Commit current transparency work** (skipRendering, translucent layer)
2. **Create feature branch** for collision experiments
3. **Implement getCollisionShape()** override
4. **Test in dev environment** with creative mode
5. **Document findings** based on actual testing
6. **Decide** if this feature should be:
   - Always enabled
   - Configurable option
   - Separate block type (BarrierBlockV3?)

---

**Status**: Ready for implementation once current changes are committed.
