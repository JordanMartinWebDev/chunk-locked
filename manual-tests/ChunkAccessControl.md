# Chunk Access Control - Manual Test Cases

## Overview

These manual tests verify the ChunkAccessManager functionality and world border enforcement. Since these features require Minecraft's runtime environment (ChunkPos, player entities, world state), they cannot be tested with standard unit tests.

**Tester Instructions:**

1. Load a test world in Minecraft
2. Use commands to set up test scenarios
3. Verify expected behavior occurs
4. Check for edge cases and error conditions

---

## Test Case 1: Basic Chunk Access Check

**Prerequisites:**

- New world created
- Player spawned at 0,0 (chunk 0,0)

**Steps:**

1. Use `/chunklocked forceunlock @s 0 0` to unlock spawn chunk
2. Walk around within chunk 0,0
3. Attempt to walk into adjacent chunk 1,0

**Expected Result:**

- Movement within chunk 0,0 works normally
- Movement into chunk 1,0 is blocked (hard border)
- Action bar message: "⚠ This chunk is locked. Unlock it to explore."

**Edge Cases:**

- Test walking along chunk boundary (parallel)
- Test jumping at boundary
- Test sprinting into boundary

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 2: Adjacency Detection

**Prerequisites:**

- Test world with single unlocked chunk at 0,0

**Steps:**

1. Verify initial state: only chunk 0,0 unlocked
2. Use `/chunklocked listchunks @s` to see unlocked chunks
3. Attempt to unlock chunk 1,0 (adjacent, east): `/chunklocked forceunlock @s 1 0`
4. Attempt to unlock chunk 5,5 (distant): `/chunklocked forceunlock @s 5 5`
5. Check which chunks are unlocked

**Expected Result:**

- Chunk 1,0 successfully unlocks (adjacent to 0,0)
- Chunk 5,5 successfully unlocks (admin command bypasses adjacency)
- Both chunks are now accessible
- Movement between 0,0 and 1,0 works smoothly

**Edge Cases:**

- Test diagonal chunk adjacency (should NOT count as adjacent)
- Test all 4 cardinal directions (N, S, E, W)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 3: Multi-Player Isolation

**Prerequisites:**

- Multiplayer server or LAN world
- Two players: PlayerA and PlayerB

**Steps:**

1. PlayerA: `/chunklocked forceunlock @s 0 0`
2. PlayerB: `/chunklocked forceunlock @s 10 10`
3. PlayerA attempts to enter chunk 10,10
4. PlayerB attempts to enter chunk 0,0
5. Each player attempts to enter their own unlocked chunk

**Expected Result:**

- PlayerA CAN enter chunk 0,0 (their unlock)
- PlayerA CANNOT enter chunk 10,10 (PlayerB's unlock)
- PlayerB CAN enter chunk 10,10 (their unlock)
- PlayerB CANNOT enter chunk 0,0 (PlayerA's unlock)
- Each player sees their own set of unlocked chunks independently

**Edge Cases:**

- Test with 3+ players
- Test with players in same chunk but different unlock states

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 4: Performance Test - 1000 Chunks

**Prerequisites:**

- Test world with command block setup

**Steps:**

1. Use command blocks to unlock 1000 chunks in a 50x20 grid:
   ```
   /execute as @a run function chunklocked:test_unlock_1000
   ```
2. Walk through various chunks in the unlocked area
3. Monitor F3 debug screen for performance metrics
4. Attempt to exit the unlocked area into a locked chunk

**Expected Result:**

- No noticeable lag when moving between chunks
- Chunk access checks complete in <1ms (check debug if available)
- Hard border still blocks movement at edges
- Server TPS remains stable (20 tps)

**Edge Cases:**

- Test with multiple players in different chunks
- Test rapid movement (elytra, ender pearl)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 5: Negative Coordinates

**Prerequisites:**

- Test world, player at spawn

**Steps:**

1. Teleport to negative coordinates: `/tp @s -100 64 -100`
2. Note the chunk coordinates (F3 debug screen)
3. Unlock current chunk: `/chunklocked forceunlock @s <chunkX> <chunkZ>`
4. Verify movement within unlocked chunk
5. Unlock adjacent chunk to the west (more negative X)
6. Verify movement between the two chunks

**Expected Result:**

- Negative coordinate chunks work identically to positive
- Adjacency detection works across negative/positive boundaries
- Hard borders function correctly
- No coordinate overflow or wrapping issues

**Edge Cases:**

- Test at coordinate 0,0 boundary (chunk 0,0 vs -1,-1)
- Test with large negative coordinates (-1000000, -1000000)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 6: Chunk Unlock Validation

**Prerequisites:**

- Test world with player having 5 credits
- Only chunk 0,0 unlocked

**Steps:**

1. Check credits: `/chunklocked credits @s`
2. Attempt to unlock chunk 1,0 (adjacent): `/chunklocked unlock @s 1 0`
3. Attempt to unlock chunk 5,5 (not adjacent): `/chunklocked unlock @s 5 5`
4. Check credits again
5. Verify which chunks are unlocked

**Expected Result:**

- Initial credits: 5
- Chunk 1,0 unlocks successfully, credits: 4
- Chunk 5,5 fails to unlock (not adjacent), error message shown
- Credits remain at 4 (no credit spent on failed unlock)
- Only chunks 0,0 and 1,0 are unlocked

**Edge Cases:**

- Test unlocking already unlocked chunk (should fail, no credit spent)
- Test unlocking with 0 credits (should fail)
- Test unlocking diagonal chunk (should fail, not adjacent)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 7: Initial Spawn Setup (First Chunk)

**Prerequisites:**

- Brand new world, never played before
- Player has no unlocked chunks

**Steps:**

1. Check player's unlocked chunks: `/chunklocked listchunks @s`
2. Player should have 0 unlocked chunks initially
3. Admin grants 1 credit: `/chunklocked credits add @s 1`
4. Player attempts to unlock ANY chunk of their choice: `/chunklocked unlock @s <X> <Z>`

**Expected Result:**

- Initial unlock can be ANY chunk (no adjacency requirement)
- First chunk unlocks successfully
- Player can move within that chunk
- Subsequent unlocks require adjacency to this first chunk

**Edge Cases:**

- Test unlocking chunk far from spawn (10000, 10000)
- Test unlocking negative coordinate chunk as first unlock

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 8: Frontier Calculation

**Prerequisites:**

- Test world with 2x2 unlocked area (chunks 0,0 | 1,0 | 0,1 | 1,1)

**Steps:**

1. Use debug command to see adjacent locked chunks: `/chunklocked debug frontier @s`
2. Verify the output shows exactly 8 chunks (the perimeter)
3. Unlock one of the frontier chunks
4. Re-run frontier command, verify it now shows 10 chunks (new perimeter)

**Expected Result:**

- 2x2 area has 8 adjacent locked chunks (perimeter)
- None of the 4 unlocked chunks are in the frontier
- After unlocking a perimeter chunk, frontier expands correctly
- Frontier calculation is fast (<10ms for 100+ chunks)

**Edge Cases:**

- Test with disconnected areas (two separate unlocked regions)
- Test with irregular shape (L-shaped unlocked area)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 9: Save/Load Persistence

**Prerequisites:**

- Test world with several unlocked chunks
- Player with known credit count

**Steps:**

1. Unlock 5 chunks in a specific pattern
2. Note the exact chunk coordinates
3. Check credit count
4. Save and exit the world
5. Reload the world
6. Check unlocked chunks: `/chunklocked listchunks @s`
7. Check credit count: `/chunklocked credits @s`
8. Verify movement in previously unlocked chunks

**Expected Result:**

- All unlocked chunks persist after save/load
- Credit count persists correctly
- Movement within unlocked chunks still works
- Hard borders at locked chunks still function
- No data loss or corruption

**Edge Cases:**

- Test with world backup/restore
- Test with server restart (multiplayer)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Test Case 10: Rapid Movement Stress Test

**Prerequisites:**

- Test world with unlocked path of chunks (1x20 line)
- Player with elytra and firework rockets

**Steps:**

1. Fly rapidly through the unlocked chunks using elytra
2. Attempt to fly into locked chunks at high speed
3. Try to "clip through" the border by flying at it diagonally
4. Teleport repeatedly between chunks: `/tp @s <X> <Y> <Z>`

**Expected Result:**

- Elytra flight works normally in unlocked chunks
- Hard border stops elytra flight into locked chunks
- No clipping through borders at high speed
- Teleportation to locked chunks is blocked
- No server lag or crashes from rapid chunk checks

**Edge Cases:**

- Test with ender pearl throws during flight
- Test with player in vehicle (boat, minecart, horse)

**Pass/Fail:** ☐ PASS ☐ FAIL

**Notes:**
_Record any observations here_

---

## Summary Checklist

After completing all test cases, verify:

- [ ] All test cases passed
- [ ] No critical bugs found
- [ ] Performance targets met (<1ms chunk checks)
- [ ] Multi-player isolation working
- [ ] Save/load persistence working
- [ ] No coordinate overflow issues
- [ ] No way to bypass hard borders found

**Overall Result:** ☐ PASS ☐ FAIL

**Date Tested:** ******\_******

**Tester:** ******\_******

**Minecraft Version:** ******\_******

**Mod Version:** ******\_******
