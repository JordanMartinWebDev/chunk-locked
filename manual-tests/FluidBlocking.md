# Fluid Blocking - Manual Test Cases

## Overview

Tests to verify that barrier blocks properly resist destruction by flowing water and lava while maintaining selective collision properties.

---

## Test Case 1: Water Bucket Placement

**Prerequisites**:

- Load world with barrier walls
- Have water bucket in inventory
- Be in creative or survival mode

**Steps**:

1. Locate a barrier wall (vertical barrier at chunk boundary)
2. Stand next to the barrier
3. Place water bucket on the ground next to the barrier (within 1-2 blocks)
4. Observe water flow behavior for 5-10 seconds

**Expected Result**:

- Water flows horizontally and vertically
- Water does NOT destroy any barrier blocks
- Barrier wall remains intact
- Water stops at the barrier edge (doesn't flow through)

**Edge Cases**:

- Test at different Y levels (ground level, Y=100, Y=200)
- Test with water source placed above barriers (falling water)
- Test with water source placed directly adjacent to barrier
- Test in ocean biome where water naturally exists

---

## Test Case 2: Lava Bucket Placement

**Prerequisites**:

- Load world with barrier walls
- Have lava bucket in inventory
- Be in creative mode (safer)

**Steps**:

1. Locate a barrier wall
2. Stand next to the barrier
3. Place lava bucket on the ground next to the barrier (within 1-2 blocks)
4. Observe lava flow behavior for 10-15 seconds (lava flows slower)

**Expected Result**:

- Lava flows horizontally and slowly
- Lava does NOT destroy any barrier blocks
- Barrier wall remains intact
- Lava stops at the barrier edge

**Edge Cases**:

- Test at different Y levels
- Test with lava source placed above barriers (falling lava)
- Test in the Nether where lava naturally exists
- Test lava flowing into water near barriers (creates cobblestone/obsidian)

---

## Test Case 3: Mob Pathfinding Through Barriers

**Prerequisites**:

- Load world with barrier walls
- Have mob spawn eggs (chicken, cow, pig)
- Have mob attractant (wheat for cows, seeds for chickens)

**Steps**:

1. Spawn a mob on one side of a barrier wall
2. Stand on the opposite side of the barrier
3. Hold the mob's preferred food item to attract it
4. Observe mob behavior

**Expected Result**:

- Mob walks directly through the barrier (no collision)
- Mob pathfinds through barrier to reach player
- Mob does not get stuck or confused at barrier
- Mob treats barrier as if it doesn't exist

**Edge Cases**:

- Test with different mob types (passive, neutral, hostile)
- Test with multiple barriers in a row
- Test with barriers at different angles
- Test mob AI pathfinding around corners with barriers

---

## Test Case 4: Player Collision With Barriers

**Prerequisites**:

- Load world with barrier walls
- Be in survival or creative mode

**Steps**:

1. Locate a barrier wall
2. Walk directly toward the barrier
3. Attempt to walk through the barrier
4. Try jumping while walking into barrier
5. Try sprinting into barrier

**Expected Result**:

- Player is completely blocked by barrier
- Player cannot walk through barrier
- Player cannot jump over barrier (if barrier is tall enough)
- Player collision is smooth (no glitching)

**Edge Cases**:

- Test collision at chunk boundaries
- Test collision while swimming
- Test collision while flying in creative mode
- Test collision while riding a horse/boat

---

## Test Case 5: Ender Pearl Blocking

**Prerequisites**:

- Load world with barrier walls
- Have ender pearls in inventory
- Be in survival or creative mode

**Steps**:

1. Stand 10 blocks away from a barrier wall
2. Aim ender pearl directly at the barrier
3. Throw ender pearl
4. Observe ender pearl behavior

**Expected Result**:

- Ender pearl hits barrier and falls to ground
- Player does NOT teleport through barrier
- Ender pearl is blocked (same as player collision)

**Edge Cases**:

- Test throwing pearl at different angles
- Test throwing pearl from above barrier
- Test throwing pearl from below barrier

---

## Test Case 6: Ocean/River Generation

**Prerequisites**:

- Generate new world or find ocean biome
- Ensure barrier walls cross water bodies

**Steps**:

1. Locate a barrier wall that intersects with ocean or river
2. Observe barrier integrity in water
3. Check for any missing barrier blocks underwater
4. Swim around the barrier checking all Y levels

**Expected Result**:

- All barrier blocks present underwater
- No gaps in barrier wall due to water
- Water exists on both sides but doesn't flow through
- Barrier extends from bedrock to world height

**Edge Cases**:

- Test in different water depths
- Test in flowing rivers
- Test in waterfalls
- Test near water source blocks

---

## Test Case 7: World Generation with Lava

**Prerequisites**:

- Find or generate world with lava lakes
- Ensure barrier walls cross lava bodies

**Steps**:

1. Locate a barrier wall that intersects with lava lake
2. Observe barrier integrity in/near lava
3. Check for any missing barrier blocks near lava
4. Check both surface lava and underground lava

**Expected Result**:

- All barrier blocks present near/in lava
- No gaps in barrier wall due to lava
- Lava doesn't flow through barriers
- Barrier blocks don't combust or break

**Edge Cases**:

- Test in Nether lava oceans
- Test with lava falls
- Test at Y=11 (common lava level)
- Test in lava lakes at various Y levels

---

## Test Case 8: Chunk Boundary Water/Lava

**Prerequisites**:

- Load world with barriers at chunk boundaries
- Have F3+G enabled to see chunk boundaries

**Steps**:

1. Press F3+G to show chunk boundaries
2. Locate barrier exactly at chunk edge
3. Place water/lava bucket at chunk boundary near barrier
4. Observe fluid behavior across chunk boundary

**Expected Result**:

- Barrier blocks at chunk boundaries resist water/lava
- No difference in behavior compared to mid-chunk barriers
- Chunk loading doesn't affect barrier integrity
- Fluids don't destroy barriers during chunk load/unload

**Edge Cases**:

- Test while standing in one chunk, placing fluid in another
- Test with barriers at corner where 4 chunks meet
- Test rapidly loading/unloading chunks with fluid present
- Test in multiplayer with multiple players in different chunks

---

## Test Case 9: Waterlogging Prevention

**Prerequisites**:

- Have water bucket
- Have barrier wall

**Steps**:

1. Right-click on barrier block with water bucket
2. Attempt to waterlog the barrier
3. Observe if water appears inside barrier block

**Expected Result**:

- Water bucket cannot waterlog barrier
- Barrier block remains unchanged
- Water is not placed (bucket remains full)

**Edge Cases**:

- Test with various waterloggable blocks nearby
- Test placing water source adjacent then removing it

---

## Test Case 10: Fluid Flow After Barrier Placement

**Prerequisites**:

- Have water source already flowing
- Have admin access to place barriers

**Steps**:

1. Create a flowing water source
2. Use `/chunklocked unlock` or admin command to place barriers in water's path
3. Observe water behavior after barrier placement
4. Check if water recalculates flow around new barriers

**Expected Result**:

- Water flow stops at newly placed barriers
- Water recalculates path around barriers
- Newly placed barriers are not destroyed by existing water
- Water does not glitch through barriers

**Edge Cases**:

- Place barriers in middle of waterfall
- Place barriers while water is mid-flow
- Place multiple barrier walls rapidly in flowing water

---

## Test Results Summary

| Test Case                | Pass/Fail | Notes |
| ------------------------ | --------- | ----- |
| 1. Water Bucket          | ⬜        |       |
| 2. Lava Bucket           | ⬜        |       |
| 3. Mob Pathfinding       | ⬜        |       |
| 4. Player Collision      | ⬜        |       |
| 5. Ender Pearl           | ⬜        |       |
| 6. Ocean Generation      | ⬜        |       |
| 7. Lava Generation       | ⬜        |       |
| 8. Chunk Boundary        | ⬜        |       |
| 9. Waterlogging          | ⬜        |       |
| 10. Flow After Placement | ⬜        |       |

---

## Known Issues

**None currently identified**

---

## Regression Testing

When making changes to barrier properties or placement logic, re-run:

- Test Case 1 (Water Bucket) - Critical
- Test Case 3 (Mob Pathfinding) - Critical
- Test Case 4 (Player Collision) - Critical

These three tests cover the core functionality that must not break.
