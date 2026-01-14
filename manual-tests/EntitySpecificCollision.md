# Entity-Specific Collision - Manual Test Cases

**Feature**: BarrierBlockV2 player-only collision  
**Date**: January 13, 2026  
**Implementation**: [BarrierBlockV2.java](chunkloaded/block/BarrierBlockV2.java)

---

## Overview

BarrierBlockV2 now implements entity-specific collision:

- **Players are blocked** - solid collision prevents movement through barriers
- **Mobs pass through** - zombies, skeletons, etc. can walk through freely
- **Items pass through** - dropped items can move through barriers
- **Fluids flow through** - water and lava can flow through barriers
- **Player-ridden entities blocked** - boats/minecarts with players cannot pass

---

## Test Case 1: Player Collision

**Prerequisites**:

- Place BarrierBlockV2 blocks in creative mode
- Switch to survival mode for realistic testing

**Steps**:

1. Place a 3x3 wall of BarrierBlockV2 blocks
2. Attempt to walk through the barrier
3. Try jumping over while approaching
4. Try sneaking through
5. Try sprinting into the barrier

**Expected Result**:

- Player is completely blocked by the barrier in all cases
- Player cannot pass through from any direction
- Collision feels solid like a normal block

**Edge Cases**:

- Try from different angles (north, south, east, west)
- Try at chunk boundaries
- Test at different Y-levels (ground, mid-air, underground)

---

## Test Case 2: Hostile Mob Pass-Through

**Prerequisites**:

- Barrier wall setup
- Spawn hostile mobs (zombie, skeleton, creeper)

**Steps**:

1. Place barrier wall between player and open area
2. Spawn zombie on opposite side of barrier
3. Move close enough to aggro the zombie
4. Observe if zombie walks through barrier toward player

**Expected Result**:

- Zombie walks through barrier blocks as if they don't exist
- Zombie pathfinding treats barriers as passable
- Zombie can attack player through barrier if in range

**Edge Cases**:

- Test with different mob types: skeleton, creeper, spider, enderman
- Test with baby zombies
- Test with zombified piglins from Nether

---

## Test Case 3: Passive Mob Pass-Through

**Prerequisites**:

- Barrier wall setup
- Passive mobs (cow, sheep, pig, chicken)

**Steps**:

1. Spawn passive mob on one side of barrier
2. Use wheat/seeds to lure mob toward player through barrier
3. Push mob manually through barrier
4. Observe mob wandering behavior near barriers

**Expected Result**:

- Passive mobs can walk through barriers
- Mobs do not get stuck on barriers
- Pathfinding treats barriers as passable

**Edge Cases**:

- Test with different animals
- Test leading with leash through barrier
- Test baby animals vs adults

---

## Test Case 4: Item Drop Pass-Through

**Prerequisites**:

- Barrier wall setup
- Various items (dirt, diamonds, tools)

**Steps**:

1. Stand on one side of barrier wall
2. Throw items toward the barrier
3. Drop items directly on top of barrier
4. Place items on ground and push toward barrier using water

**Expected Result**:

- Items pass through barrier blocks
- Items do not get stuck or bounce back
- Items can be picked up from other side

**Edge Cases**:

- Test with stacked items (64 cobblestone)
- Test with unstackable items (tools, armor)
- Test with items in flowing water

---

## Test Case 5: Fluid Flow Through Barriers

**Prerequisites**:

- Barrier wall with gaps
- Water bucket and lava bucket

**Steps**:

1. Create a vertical barrier wall 3 blocks high
2. Place water source block above barrier
3. Observe if water flows through barrier
4. Repeat with lava

**Expected Result**:

- Water flows through barrier blocks
- Lava flows through barrier blocks
- Fluids treat barriers as air blocks
- Fluid mechanics remain normal (no infinite sources)

**Edge Cases**:

- Test with waterlogged blocks near barriers
- Test underwater placement of barriers
- Test if barriers can be waterlogged (they shouldn't be)

---

## Test Case 6: Projectile Pass-Through

**Prerequisites**:

- Barrier wall setup
- Bow and arrows, snowballs, eggs, ender pearls

**Steps**:

1. Stand in front of barrier wall
2. Shoot arrow at barrier
3. Throw snowball at barrier
4. Throw egg at barrier
5. Throw ender pearl through barrier

**Expected Result**:

- Arrows pass through barriers
- Snowballs pass through barriers
- Eggs pass through barriers
- Ender pearls pass through and teleport player

**Edge Cases**:

- Test with tipped arrows
- Test with spectral arrows
- Test tridents (thrown)
- Test fishing rod bobber

---

## Test Case 7: Player-Controlled Entity Blocking

**Prerequisites**:

- Barrier wall setup
- Boat, minecart, horse (tamed)

**Steps**:

1. Place boat near barrier
2. Enter boat
3. Attempt to move through barrier while in boat
4. Exit boat, move boat through barrier manually
5. Repeat with minecart on rails
6. Repeat with mounted horse

**Expected Result**:

- Player CANNOT pass through barrier while in boat
- Player CANNOT pass through barrier while in minecart
- Player CANNOT pass through barrier while riding horse
- Empty boat/minecart CAN pass through barrier
- Riderless horse CAN walk through barrier

**Edge Cases**:

- Test with pig (saddle and carrot on stick)
- Test with strider in Nether
- Test with player as passenger (not controller)

---

## Test Case 8: Redstone and Mechanisms

**Prerequisites**:

- Barrier wall
- Redstone dust, repeaters, pistons

**Steps**:

1. Place redstone dust on/near barriers
2. Test redstone signal through barriers
3. Place piston facing barrier, activate it
4. Place piston behind barrier, try to push block through

**Expected Result**:

- Redstone can be placed adjacent to barriers
- Redstone signals do NOT pass through barriers (expected)
- Pistons cannot push barriers (unbreakable)
- Pistons CAN push other blocks through barriers (if collision allows)

**Edge Cases**:

- Test with observers facing barriers
- Test with slime block flying machines

---

## Test Case 9: Multiplayer Behavior

**Prerequisites**:

- Multiplayer server or LAN world
- Two players

**Steps**:

1. Player 1 places barrier wall
2. Player 2 attempts to walk through barrier
3. Player 1 observes Player 2's collision
4. Test with both players on opposite sides

**Expected Result**:

- Both players see consistent collision behavior
- No desync between client and server
- Other player appears blocked at barrier

**Edge Cases**:

- Test with high latency
- Test with player pushing another player toward barrier
- Test with knockback effects (explosions, knockback enchantment)

---

## Test Case 10: Dimension Behavior

**Prerequisites**:

- Barriers in Overworld, Nether, and End
- Access to all dimensions

**Steps**:

1. Place barrier in Overworld, test collision
2. Place barrier in Nether, test collision
3. Place barrier in End, test collision
4. Test mob behavior in each dimension

**Expected Result**:

- Collision behavior is consistent across all dimensions
- Works identically in Overworld, Nether, and End

**Edge Cases**:

- Test with dimension-specific mobs (piglins, endermen)
- Test with ghast fireballs passing through barriers

---

## Test Case 11: Light Transmission

**Prerequisites**:

- Barrier wall in dark area
- Light source (torch, glowstone)

**Steps**:

1. Place barrier wall in underground cave
2. Place torch on one side of barrier
3. Observe light levels on opposite side

**Expected Result**:

- Light passes through barriers (transparency already tested)
- Skylight propagates through barriers
- Light level matches as if barrier were air

**Edge Cases**:

- Test with barrier at surface (skylight)
- Test with multiple stacked barriers vertically

---

## Test Case 12: Performance with Many Barriers

**Prerequisites**:

- Large area with many barriers
- F3 debug screen open

**Steps**:

1. Place 100+ barrier blocks in a large structure
2. Spawn 50+ mobs nearby
3. Drop 100+ items near barriers
4. Observe FPS and TPS

**Expected Result**:

- No significant performance degradation
- Collision checks remain fast (using cached VoxelShapes)
- FPS/TPS remain stable

**Edge Cases**:

- Test with thousands of entities
- Test with rapid collision checks (sprinting back and forth)

---

## Test Case 13: Creative Mode Interaction

**Prerequisites**:

- Creative mode enabled
- Barriers placed

**Steps**:

1. In creative mode, attempt to break barrier
2. Try to place blocks inside barrier space
3. Try flying through barriers

**Expected Result**:

- Creative mode players can break barriers (as intended)
- Players still collide with barriers (no flying through)
- Barrier functionality same as survival

**Edge Cases**:

- Test spectator mode (should pass through)
- Test with commands (/tp through barriers)

---

## Known Limitations

1. **Pathfinding**: Mobs may not pathfind optimally through barriers (AI treats them as obstacles despite being passable)
2. **Ender Pearl Edge Case**: Player might teleport to other side, but this is expected behavior
3. **Spectator Mode**: Not tested, but spectator mode should pass through all blocks anyway

---

## Regression Tests

Ensure old functionality still works:

- [ ] Barriers still have semi-transparent appearance
- [ ] Barriers are still unbreakable in survival
- [ ] Barriers still have correct texture
- [ ] Light still passes through barriers
- [ ] Barriers can still be placed/broken in creative

---

## Test Environment Setup

### Quick Test World Setup

```
1. Create new creative world
2. Run: /gamemode creative
3. Obtain BarrierBlockV2 blocks (creative inventory)
4. Build test arena:
   - 10x10 enclosed area
   - Barrier walls on sides
   - Spawn platform for mobs
   - Water test area
5. Switch to survival: /gamemode survival
6. Begin test cases
```

### Recommended Commands

```mcfunction
# Give barriers
/give @p chunk-locked:barrier_block 64

# Spawn test mobs
/summon minecraft:zombie ~ ~ ~5
/summon minecraft:skeleton ~ ~ ~-5
/summon minecraft:cow ~ ~ ~3

# Clear mobs after testing
/kill @e[type=!player]

# Set time (for visibility)
/time set day
```

---

## Test Results Template

| Test Case                     | Pass/Fail | Notes |
| ----------------------------- | --------- | ----- |
| 1. Player Collision           | ⬜        |       |
| 2. Hostile Mob Pass-Through   | ⬜        |       |
| 3. Passive Mob Pass-Through   | ⬜        |       |
| 4. Item Drop Pass-Through     | ⬜        |       |
| 5. Fluid Flow                 | ⬜        |       |
| 6. Projectile Pass-Through    | ⬜        |       |
| 7. Player-Controlled Entities | ⬜        |       |
| 8. Redstone Mechanisms        | ⬜        |       |
| 9. Multiplayer Behavior       | ⬜        |       |
| 10. Dimension Behavior        | ⬜        |       |
| 11. Light Transmission        | ⬜        |       |
| 12. Performance               | ⬜        |       |
| 13. Creative Mode             | ⬜        |       |

---

## Testing Priority

**High Priority** (Core Functionality):

- Test Case 1: Player Collision
- Test Case 2: Hostile Mob Pass-Through
- Test Case 4: Item Drop Pass-Through
- Test Case 5: Fluid Flow

**Medium Priority** (Important Edge Cases):

- Test Case 7: Player-Controlled Entities
- Test Case 9: Multiplayer Behavior
- Test Case 13: Creative Mode

**Low Priority** (Nice to Have):

- Test Case 8: Redstone
- Test Case 10: Dimensions
- Test Case 12: Performance

---

**Test Completion**: Date: ****\_\_\_****  
**Tester**: ****\_\_\_****  
**Version**: ****\_\_\_****  
**Result**: ⬜ All Pass | ⬜ Some Fail | ⬜ Major Issues
