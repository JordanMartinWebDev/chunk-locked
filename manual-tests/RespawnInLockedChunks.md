# Player Respawn in Locked Chunks - Manual Test Cases

## Overview

Tests to verify that players who die and respawn in locked chunks are teleported to the nearest unlocked chunk. The mod should NOT auto-unlock chunks on respawn, but instead relocate the player to a safe area while preserving the penalty system (wither effect).

---

## Test Case 1: Respawn at Bed in Locked Chunk

**Prerequisites**:

- Chunk Locked world with some unlocked chunks
- Bed placed and set as spawn point

**Steps**:

1. Place a bed in an unlocked chunk and sleep in it
2. Note the bed's chunk coordinates
3. Unlock several adjacent chunks around the bed
4. Use `/chunklocked forcelock <chunkX> <chunkZ>` to lock the bed's chunk
5. Go somewhere else and die
6. Respawn at the bed

**Expected Result**:

- Player attempts to respawn at bed location
- Mod detects bed is in locked chunk
- Player is **teleported to nearest unlocked chunk** (likely adjacent)
- Player receives message: "§eYour respawn point was in a locked area. Teleported to the nearest unlocked chunk!"
- **No chunks are auto-unlocked**
- Bed's chunk remains locked
- Player can move freely in the unlocked chunk they landed in
- If player walks back to locked bed chunk, wither effect applies normally

**Edge Cases**:

- Test with bed surrounded by locked chunks on multiple sides
- Test with only one adjacent unlocked chunk
- Verify teleportation lands on safe ground (not in lava/void)

---

## Test Case 2: Respawn at World Spawn in Locked Chunk

**Prerequisites**:

- Chunk Locked world
- World spawn location is known

**Steps**:

1. Identify world spawn chunk coordinates
2. Unlock chunks away from world spawn
3. Use `/chunklocked forcelock` to lock the world spawn chunk
4. Die without setting a bed spawn
5. Respawn at world spawn

**Expected Result**:

- Player attempts to respawn at world spawn
- Mod detects spawn is in locked chunk
- Player is **teleported to nearest unlocked chunk**
- Message appears: "§eYour respawn point was in a locked area. Teleported to the nearest unlocked chunk!"
- World spawn chunk remains locked (not auto-unlocked)
- Player lands safely in unlocked area
- Wither effect applies if player returns to locked spawn area

---

## Test Case 3: Respawn with Distant Unlocked Chunks

**Prerequisites**:

- Chunk Locked world
- Unlocked chunks far from spawn point

**Steps**:

1. Set bed spawn in an unlocked chunk
2. Unlock another area 10+ chunks away
3. Lock all chunks around the bed (create isolated locked zone)
4. Die and respawn

**Expected Result**:

- Player respawns at bed location initially
- Mod detects locked chunk
- Spiral search finds nearest unlocked chunk (may be 10+ chunks away)
- Player is teleported to that distant location
- Message confirms teleportation
- No auto-unlocking occurs

**Edge Cases**:

- Test with maximum search radius (16 chunks)
- Test performance with many unlocked chunks scattered around

---

## Test Case 4: Normal Respawn in Already Unlocked Chunk

**Prerequisites**:

- Chunk Locked world
- Spawn point (bed/world spawn) in an unlocked chunk

**Steps**:

1. Set spawn point in a chunk that's already unlocked
2. Die
3. Respawn

**Expected Result**:

- Player respawns normally at spawn point
- **No teleportation occurs**
- **No message about locked area**
- No chunks unlocked or modified
- Gameplay continues normally

---

## Test Case 5: Nearest Chunk Finding - Adjacent Chunks

**Prerequisites**:

- Chunk Locked world
- Controlled unlock pattern

**Steps**:

1. Create a situation where bed is in a locked chunk at position (X, Z)
2. Unlock only the chunk at (X+1, Z) - east adjacent
3. Die and respawn

**Expected Result**:

- Player teleports to (X+1, Z) - the only adjacent unlocked chunk
- Teleportation is precise (lands in correct chunk)
- Message confirms nearest chunk found
- Player can see the locked bed chunk with barriers

---

## Test Case 6: Nearest Chunk Finding - Multiple Options

**Prerequisites**:

- Chunk Locked world
- Multiple unlocked chunks equidistant from spawn

**Steps**:

1. Set bed at locked chunk (0, 0)
2. Unlock chunks at (2, 0), (0, 2), (-2, 0), (0, -2) - all same distance
3. Die and respawn

**Expected Result**:

- Player teleports to ONE of the equidistant chunks
- Choice is deterministic based on spiral search order
- Player lands safely in whichever chunk was chosen
- All equidistant chunks are valid, algorithm picks first found

---

## Test Case 7: Penalty System Still Active

**Prerequisites**:

- Chunk Locked world
- Bed in locked chunk with nearby unlocked area

**Steps**:

1. Set bed in locked chunk
2. Die and respawn (player gets teleported to unlocked chunk)
3. Walk back toward the locked bed chunk
4. Enter the locked chunk with barriers

**Expected Result**:

- After respawn teleportation, player is safe in unlocked chunk
- Walking back to locked chunk **still triggers wither effect**
- Barriers still block movement in locked areas
- **Penalty system is fully intact**
- Respawning does NOT give immunity to locked areas
- Player cannot exploit death to bypass penalties

---

## Test Case 8: Multiplayer - Multiple Players Respawning

**Prerequisites**:

- Multiplayer Chunk Locked world
- 2+ players with different locked spawn points

**Steps**:

1. Player 1 sets bed in locked chunk A
2. Player 2 sets bed in locked chunk B
3. Both players die simultaneously
4. Both players respawn

**Expected Result**:

- Each player independently teleported to their nearest unlocked chunk
- No conflicts or race conditions
- Each gets appropriate message
- No chunks auto-unlocked for either player
- Both can continue playing in their safe zones

---

## Test Case 9: Respawn Anchor in Nether (Unrestricted)

**Prerequisites**:

- Chunk Locked world with Nether access
- Respawn Anchor placed in Nether

**Steps**:

1. Enter the Nether
2. Place and charge a Respawn Anchor
3. Set spawn point
4. Die in the Nether

**Expected Result**:

- Player respawns at anchor normally
- **No teleportation** (Nether is unrestricted)
- No locked chunk mechanics apply
- No messages about locked areas

---

## Test Case 10: Fallback to Any Unlocked Chunk

**Prerequisites**:

- Chunk Locked world
- Unlocked chunks beyond 16 chunk search radius

**Steps**:

1. Set bed at world origin (0, 0) and lock it
2. Unlock only chunks very far away (20+ chunks)
3. Die and respawn

**Expected Result**:

- Spiral search reaches max radius (16 chunks)
- Algorithm falls back to "any unlocked chunk"
- Player is teleported to one of the distant unlocked chunks
- Message confirms teleportation
- Player may be far from spawn but in safe location

---

## Regression Tests

### Previous Bug Verification

**Bug**: Player died and respawned in locked chunk, getting trapped with wither effect

**Verification Steps**:

1. Create a Chunk Locked world
2. Set bed in a locked chunk
3. Die and respawn

**Expected Result**:

- Player does NOT get trapped
- Player is teleported to nearest unlocked chunk
- **No auto-unlocking of chunks**
- Penalty system still works if they go back to locked areas
- Bug is FIXED ✅

---

## Performance Tests

### Test Case P1: Rapid Respawns

**Steps**:

1. Die and respawn repeatedly (10+ times in 1 minute)
2. Monitor server/client performance

**Expected Result**:

- No memory leaks
- No performance degradation
- Nearest chunk search is fast (<50ms typically)
- Teleportation is smooth

### Test Case P2: Complex Unlock Patterns

**Steps**:

1. Create checkerboard pattern of locked/unlocked chunks (100+ chunks)
2. Die and respawn in various locked locations

**Expected Result**:

- Spiral search efficiently finds adjacent unlocked chunks
- Performance remains good even with complex patterns
- No lag spikes during respawn

---

## Known Limitations

- **Max search radius**: 16 chunks (configurable in code)
- **Teleportation height**: Always 1 block above ground (may be suboptimal in some cases)
- **No preference for original spawn**: Always teleports to nearest, not "best" location

---

## Test Checklist

- [ ] Test Case 1: Bed in Locked Chunk → Teleport to Adjacent
- [ ] Test Case 2: World Spawn Locked → Teleport to Nearest
- [ ] Test Case 3: Distant Unlocked Chunks → Long Range Teleport
- [ ] Test Case 4: Normal Respawn in Unlocked → No Teleport
- [ ] Test Case 5: Nearest Chunk - Adjacent Only
- [ ] Test Case 6: Nearest Chunk - Multiple Options
- [ ] Test Case 7: Penalty System Still Active After Respawn
- [ ] Test Case 8: Multiplayer Simultaneous Respawn
- [ ] Test Case 9: Nether Respawn Anchor (Unrestricted)
- [ ] Test Case 10: Fallback to Distant Chunk
- [ ] Regression: Original Bug Fixed (No Auto-Unlock)
- [ ] Performance: Rapid Respawns
- [ ] Performance: Complex Unlock Patterns

---

## Notes

- The respawn handler is implemented in `ServerPlayerMixin.java`
- It injects into the `respawn()` method at the RETURN point
- The handler calls `Chunklocked.handlePlayerRespawn(player, world)`
- **Key difference from initial fix**: Teleports instead of auto-unlocking
- Preserves game balance and penalty system integrity
- Nearest chunk algorithm uses spiral search (fast for adjacent, scales to 16 chunks)
