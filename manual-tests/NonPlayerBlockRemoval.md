# Non-Player Block Removal - Manual Test Cases

## Overview

Test that barriers are properly maintained when blocks are removed by non-player causes (leaf decay, explosions, etc.).

## Prerequisites

- Minecraft server running with Chunk Locked mod
- Creative or OP permissions for setup
- At least one locked chunk with barriers adjacent to unlocked chunks

---

## Test Case 1: Leaf Decay

**Purpose**: Verify barriers are replaced when leaves decay naturally

**Setup**:

1. Find a chunk boundary between locked and unlocked chunks
2. Place oak leaves on the barrier wall (requires supporting blocks nearby)
3. Remove the supporting blocks to trigger leaf decay
4. Wait for leaves to decay naturally

**Steps**:

1. Locate a barrier wall between locked/unlocked chunks
2. Place oak or birch leaves against the barrier (at Y=70-100 for visibility)
3. Ensure leaves are close to logs or other leaves (within 6 blocks) so they don't immediately decay
4. Remove all nearby logs/leaves to trigger decay timer
5. Wait 20-40 seconds for leaves to decay

**Expected Result**:

- Leaves decay naturally
- If leaves were placed ON barrier blocks, barrier blocks should immediately reappear after decay
- No holes should remain in the barrier wall
- Server log should show: "Non-player block removal detected at barrier boundary [pos], replacing with barrier"

**Edge Cases**:

- Leaves at different Y levels (ground level, tree height, sky limit)
- Multiple leaf blocks decaying simultaneously
- Leaves on corners of chunk boundaries

---

## Test Case 2: TNT Explosions

**Purpose**: Verify barriers are replaced when blocks are destroyed by explosions

**Setup**:

1. Find a barrier wall between locked/unlocked chunks
2. Place TNT near the barrier (2-3 blocks away)
3. Ignite TNT and observe explosion

**Steps**:

1. Locate a barrier wall
2. Stand in the unlocked chunk
3. Place TNT 2-3 blocks from the barrier wall
4. Ignite TNT with flint and steel
5. Observe the explosion

**Expected Result**:

- TNT explodes and destroys surrounding terrain
- Barrier blocks destroyed by explosion are immediately replaced
- No holes remain in the barrier wall after explosion
- Normal terrain blocks are destroyed as expected
- Server log shows barrier replacement messages

**Edge Cases**:

- Multiple TNT explosions in quick succession
- TNT minecarts
- Creeper explosions
- End crystal explosions
- Bed explosions in Nether (if barrier walls exist there)

---

## Test Case 3: Fire Burning Blocks

**Purpose**: Verify barriers are replaced when flammable blocks burn

**Setup**:

1. Find a barrier wall
2. Place wooden planks or logs on/near barrier blocks
3. Set fire to the wood

**Steps**:

1. Locate barrier wall between locked/unlocked chunks
2. Place wooden planks on top of barrier blocks
3. Use flint and steel to ignite the wood
4. Wait for fire to spread and consume the wood
5. Check if barriers remain intact

**Expected Result**:

- Wood burns away
- If wood was placed on barrier blocks, barriers should reappear
- No holes in barrier wall
- Fire does not spread through barriers

**Edge Cases**:

- Lava flow onto barriers
- Lightning striking barriers
- Flint and steel used directly on barriers (should not place fire)

---

## Test Case 4: Water/Lava Flow

**Purpose**: Verify barriers prevent fluid flow and are maintained

**Setup**:

1. Find barrier wall
2. Place water/lava source blocks near barriers

**Steps**:

1. Locate barrier wall between locked/unlocked chunks
2. Place water source block 1 block away from barrier (in unlocked chunk)
3. Observe water flow attempting to reach barrier
4. Repeat with lava

**Expected Result**:

- Water/lava flows toward barrier but stops at barrier face
- Fluids do not pass through barriers
- Barriers remain intact
- No blocks are destroyed by fluid flow

**Edge Cases**:

- Water from above flowing down barrier wall
- Lava from above (should not destroy barriers)
- Kelp/seagrass placed near barriers (should not break barriers)

---

## Test Case 5: Piston Movement

**Purpose**: Verify barriers cannot be moved by pistons

**Setup**:

1. Find barrier wall
2. Place pistons facing the barrier

**Steps**:

1. Locate barrier wall
2. Place sticky piston facing barrier block (in unlocked chunk)
3. Power the piston with redstone
4. Observe piston behavior

**Expected Result**:

- Piston extends but cannot move barrier block
- Barrier remains in place
- Piston retracts normally
- No holes created in barrier

**Edge Cases**:

- Normal piston (non-sticky)
- Piston from locked side (if accessible)
- Slime block contraptions pushing into barriers

---

## Test Case 6: Dragon Egg Teleportation

**Purpose**: Verify dragon egg doesn't create holes when teleporting

**Setup**:

1. Obtain dragon egg (creative mode: `/give @s dragon_egg`)
2. Find barrier wall
3. Place dragon egg on barrier block

**Steps**:

1. Place dragon egg on top of a barrier block at chunk boundary
2. Right-click the dragon egg to make it teleport
3. Check if barrier remains after egg teleports away

**Expected Result**:

- Dragon egg teleports to new location
- Barrier block remains at original position
- No hole created in barrier wall

---

## Test Case 7: Grass/Mycelium Spread

**Purpose**: Verify grass spreading doesn't affect barriers

**Setup**:

1. Find barrier wall at ground level
2. Place grass blocks adjacent to barriers

**Steps**:

1. Locate ground-level barrier wall
2. Place dirt blocks with grass on top adjacent to barriers
3. Wait for grass spread mechanics (may take several minutes with random ticks)
4. Observe if barriers are affected

**Expected Result**:

- Grass spreads to adjacent dirt blocks normally
- Barriers are not converted to grass
- Barriers remain intact
- No visual glitches

---

## Test Case 8: Bamboo/Sugarcane Growth

**Purpose**: Verify growing plants don't break barriers

**Setup**:

1. Find barrier wall
2. Plant bamboo or sugarcane near barriers

**Steps**:

1. Locate barrier wall at ground level
2. Plant bamboo or sugarcane on blocks adjacent to barriers
3. Use bonemeal to speed up growth
4. Observe if growth attempts to break through barriers

**Expected Result**:

- Plants grow upward normally
- Plants do not break or replace barrier blocks
- Plants do not grow into locked chunks
- Barriers remain intact

---

## Test Case 9: Snow/Ice Formation

**Purpose**: Verify weather effects don't affect barriers

**Setup**:

1. Find barrier wall in snowy biome (or use `/weather rain`)
2. Wait for snow accumulation

**Steps**:

1. Teleport to snowy biome with barrier walls
2. Use `/weather rain` or wait for natural snowfall
3. Observe if snow accumulates on barriers
4. Check if freeze mechanics affect barriers

**Expected Result**:

- Snow may accumulate on top of barriers (cosmetic)
- Water adjacent to barriers may freeze
- Barriers themselves are not destroyed or modified
- No holes created in barrier wall

---

## Test Case 10: Command-Based Block Changes

**Purpose**: Verify administrative commands respect barrier rules

**Setup**:

1. Find barrier wall
2. Use `/fill` or `/setblock` commands

**Steps**:

1. Locate barrier at chunk boundary
2. Try command: `/setblock <barrier_pos> air`
3. Observe if barrier is removed and replaced

**Expected Result**:

- Command initially removes barrier block
- Barrier is immediately replaced by BlockRemovalMixin
- No permanent hole is created
- Server log shows replacement message

**Edge Cases**:

- `/fill` command covering multiple barrier blocks
- `/clone` command involving barriers
- WorldEdit (if present) commands

---

## Regression Tests

After implementing the fix, re-run these existing tests to ensure no functionality broke:

1. **Player Block Breaking** (manual-tests/ChunkAccessControl.md)

   - Players should still be unable to break through barriers by hand
   - Barriers broken by players should still be replaced

2. **Barrier Placement** (manual-tests/ChunkStorageAndManagement.md)

   - Barriers should still be placed correctly when chunks are locked
   - Multiple areas should still work correctly

3. **Performance**
   - Check server TPS with `/tps` command (if available) or observe lag
   - The mixin should not cause noticeable performance impact
   - Large explosions should not cause excessive lag from barrier replacement

---

## Known Limitations

- Mixin fires for ALL block removals, which could have slight performance impact in very active worlds
- Command-based block removal will trigger barrier replacement (may interfere with admin building near barriers)
- Dragon egg teleportation is somewhat rare, may be difficult to test extensively

---

## Success Criteria

✅ All test cases pass
✅ No holes can be created in barrier walls by any non-player means
✅ Server performance remains acceptable
✅ Existing functionality still works (regression tests pass)
✅ Server logs show appropriate barrier replacement messages
