# Phase 2: Optimization & Edge Cases - Manual Test Cases

## Overview

These tests verify that Phase 2 optimizations work correctly:

- Smart barrier placement (only between locked/unlocked chunks)
- Lazy loading (barriers placed when chunks load)
- Batch operations (multiple barriers placed efficiently)
- Visual feedback (particles, sounds, action bar messages)

---

## Test Case 1: Smart Barrier Placement - Locked/Unlocked Boundaries

**Goal**: Verify barriers only appear at locked/unlocked chunk boundaries, not between two locked or two unlocked chunks.

**Prerequisites**:

- Create a new world with Chunk Locked mod
- Start at coordinates where initial 2x2 chunk area is unlocked
- Have admin access (/op)

**Setup**:

1. Note your initial spawn chunk position (should be a 2x2 area)
2. Run `/chunklocked debug info` to see unlocked chunks

**Steps**:

1. Stand in an unlocked chunk (part of starting 2x2)
   **Expected Result**: No barriers visible around you
2. Look in all 4 cardinal directions
   **Expected Result**: See invisible barriers at chunk boundary (Z or X coordinate at 0 or 15)
3. Try to walk in all 4 directions
   **Expected Result**: Can walk freely in unlocked chunks, hit barrier at locked chunk boundary
4. Check barrier between two LOCKED chunks
   - Move to a position where you're outside the boundary
   - Find a locked chunk that's adjacent to another locked chunk
   - Use `/fill` command or check collision
     **Expected Result**: NO barriers between two locked chunks (player could theoretically walk through if not for outer barrier)
5. Unlock an adjacent chunk using `/chunklocked unlock <x> <z>`
   **Expected Result**: Barriers removed from that side, new barriers appear on far side of newly unlocked chunk

**Edge Cases**:

- Check diagonal corners (where north and west barriers meet)
- Check when unlocked area forms an L-shape (some edges have locked neighbors, some don't)
- Check when unlocked area is only 1x1 (all 4 sides surrounded by locked chunks)

---

## Test Case 2: Lazy Barrier Loading

**Goal**: Verify barriers are placed correctly even when chunks load asynchronously.

**Prerequisites**:

- Restart the server/world to test fresh chunk loading
- Have several chunks unlocked by advancement completion
- Monitor server logs for timing

**Steps**:

1. Close the world
2. Delete chunks from the region folder (in run/saves/[world]/region/) to force reload
   - Delete specific .mca files that contain locked chunks
3. Reopen the world
4. Monitor as chunks load
   **Expected Result**: Barriers appear around locked chunks without errors, even if chunks load in non-obvious order

5. Check that barriers are in correct positions
   **Expected Result**: All barriers properly positioned, no gaps in walls

6. Run `/chunklocked debug barriers` to verify barrier count
   **Expected Result**: Matches expected number (16 blocks per side _ # of locked/unlocked boundaries _ 384 world height)

**Edge Cases**:

- World with 100+ chunks unlocked (test at massive scale)
- Unlock chunk right as nearby chunk is loading (timing edge case)

---

## Test Case 3: Batch Barrier Operations Performance

**Goal**: Verify that batch operations don't cause server lag or client-side issues.

**Prerequisites**:

- World with server-side profiling enabled (optional: use `/profiler`)
- Multiple locked chunks adjacent to a single unlocked chunk

**Steps**:

1. Monitor server TPS with `/tps` command (if available) or watch for lag
2. Unlock a single chunk adjacent to 5+ locked chunks
   **Expected Result**:

   - No server lag spike (TPS remains >15)
   - Barriers appear immediately without stuttering
   - No client-side FPS drop

3. Unlock multiple chunks in rapid succession
   **Expected Result**: Server handles gracefully, no crashes

4. Check `/chunklocked debug performance` if available
   **Expected Result**: Barrier placement time <100ms per chunk

**Edge Cases**:

- Unlock chunk with 100+ barrier blocks to place
- Unlock while other chunks are being loaded

---

## Test Case 4: Particle Feedback at Barriers

**Goal**: Verify particles appear when player hits barriers.

**Prerequisites**:

- Client particle settings NOT set to "Minimal"
- Standing at barrier edge

**Steps**:

1. Walk toward a barrier wall (locked chunk edge)
2. Hit the barrier (attempt to move into it)
   **Expected Result**:

   - Smoke particles appear at barrier location
   - Particles spawn only once per second (throttled)
   - No particle spam even if repeatedly hitting barrier

3. Wait 1 second
4. Hit barrier again
   **Expected Result**: Particles appear again (throttle has reset)

5. Change direction and repeat
   **Expected Result**: Particles appear in different locations as you hit different parts of the wall

**Edge Cases**:

- With particle settings at "All": see abundant particles
- With particle settings at "Low": see fewer particles
- Rapidly clicking (spam) barrier: particles should still throttle

---

## Test Case 5: Sound Feedback at Barriers

**Goal**: Verify sound plays when player interacts with barriers.

**Prerequisites**:

- Master volume ON
- Sound settings at reasonable level (not muted)

**Steps**:

1. Walk toward barrier
2. Attempt to enter locked chunk
   **Expected Result**:

   - Stone hit sound plays (solid "thunk" sound)
   - Sound plays at barrier location
   - Sound throttled to 1 per second

3. Wait 1 second and try again
   **Expected Result**: Sound plays again

4. Try different directions
   **Expected Result**: Sound plays from different locations

**Edge Cases**:

- Very close to barrier: sound still plays
- Far from barrier: sound volume decreases appropriately
- Rapidly spamming movement: sound throttled (not spam)

---

## Test Case 6: Action Bar Message Feedback

**Goal**: Verify action bar message appears at locked chunk boundaries.

**Prerequisites**:

- Playing in single-player or multiplayer
- Can see action bar (text above hotbar)

**Steps**:

1. Approach a locked chunk boundary from an unlocked chunk
   **Expected Result**:

   - Action bar message appears: "⚠ This chunk is locked. Requires 3 more credits." (or similar)
   - Message appears in red with warning symbol
   - Message throttled to 1 every 2 seconds

2. Stand at boundary facing locked chunk for 5 seconds
   **Expected Result**: Message appears only once every 2 seconds (not spammy)

3. Move away from boundary
   **Expected Result**: Message stops appearing after you're fully in unlocked chunk

4. Return to boundary
   **Expected Result**: Message appears again

**Edge Cases**:

- At exact chunk boundary (X=0, Z=0): message still appears
- Multiple locked chunks adjacent: message matches closest/most relevant chunk
- After unlocking: message shouldn't appear at that boundary anymore

---

## Test Case 7: Unlock Particle Feedback

**Goal**: Verify success particles appear when chunk is unlocked.

**Prerequisites**:

- Have unlock credits available
- Particle settings enabled

**Steps**:

1. Complete an advancement to gain unlock credits
2. Open chunk map UI (if implemented) and unlock a chunk
   **Expected Result**:

   - Green sparkle particles burst from center of unlocked chunk
   - Particles visible from distance (can see from 2-3 chunks away)
   - Particles appear once for 3 seconds

3. Repeat with different chunks
   **Expected Result**: Particles appear at correct chunk centers

**Edge Cases**:

- Unlock chunk at world border: particles still visible
- Unlock chunk far from player: particles still spawn and visible
- Multiple simultaneous unlocks: particles for each chunk

---

## Test Case 8: Unlock Sound Feedback

**Goal**: Verify celebratory sound plays when chunk unlocks.

**Prerequisites**:

- Sound enabled
- Just completed an advancement or have credits to spend

**Steps**:

1. Unlock a chunk (via UI or command)
   **Expected Result**:

   - Bell sound plays (positive, celebratory sound)
   - Sound plays from chunk center
   - Sound audible from 2-3 chunks away
   - Higher pitch than barrier sound (celebratory)

2. Unlock another chunk
   **Expected Result**: Different bell sound plays for new chunk

**Edge Cases**:

- Underwater: sound still audible
- In caves: sound still plays
- Far from chunk: sound has appropriate falloff

---

## Test Case 9: Barrier Replacement (Creative Mode)

**Goal**: Verify barriers are replaced if broken in creative mode.

**Prerequisites**:

- In creative mode
- Holding barrier block in hand (or use creative menu)
- Standing next to a barrier wall

**Steps**:

1. Break a barrier block with left-click
   **Expected Result**: Block breaks visually
2. Wait 2 seconds
   **Expected Result**: Barrier block reappears at same location (replaced by system)

3. Break multiple barriers in sequence
   **Expected Result**: Each is replaced

4. Try to break barrier from locked chunk side
   **Expected Result**: Barrier still replaced

**Edge Cases**:

- Break dozens in a row: all replaced
- Break during chunk loading: handled correctly

---

## Test Case 10: Smart Barrier Removal on Unlock

**Goal**: Verify barriers are intelligently removed when chunks unlock.

**Prerequisites**:

- 2x2 unlocked starting area
- At least 1 locked chunk adjacent to starting area

**Steps**:

1. Note barrier positions at boundary
   - Visualize the wall between unlocked and locked chunk
2. Unlock the adjacent locked chunk
   **Expected Result**:

   - Barriers immediately disappear from that boundary
   - Particles and sound feedback (success)
   - New barriers appear on far side of newly unlocked chunk (if there are locked chunks beyond)

3. Unlock another chunk
   **Expected Result**: Same behavior - old barriers removed, new ones appear on new boundary

4. Unlock until you have an L-shape of unlocked chunks
   **Expected Result**: Barriers only at perimeter where locked chunks still exist

**Edge Cases**:

- Unlock in specific order to create complex shapes
- Check corners where multiple barrier walls meet

---

## Test Case 11: Multiplayer Barrier Synchronization

**Goal**: Verify barriers are synchronized correctly in multiplayer.

**Prerequisites**:

- 2-player multiplayer world
- Both players can see each other

**Steps**:

1. Player A approaches barrier from unlocked chunk
   **Expected Result**: Player A sees barrier, particles, sounds

2. Player B (in creative) removes barrier block while Player A watches
   **Expected Result**:

   - Barrier immediately reappears
   - Both players see replacement happen
   - No desync issues

3. Player A completes advancement and unlocks a chunk
   **Expected Result**:

   - Player B sees barriers removed/appear
   - Particle/sound effects visible to both
   - No lag or desync

4. Player B moves to the newly unlocked chunk
   **Expected Result**: Can move freely (barriers removed)

**Edge Cases**:

- Rapid unlock spam by both players
- One player logs out during barrier placement
- Unlock happens while second player is moving toward boundary

---

## Test Case 12: World Border vs Barriers (Overworld Only)

**Goal**: Verify barriers only exist in Overworld, not Nether/End.

**Prerequisites**:

- Access to Nether and End portals
- From Overworld (barriers should exist)

**Steps**:

1. In Overworld, confirm barriers exist at locked chunk boundaries
   **Expected Result**: Barriers block movement into locked chunks

2. Travel to Nether via portal
   **Expected Result**: No barriers visible, can walk freely (if chunk boundary was in Nether)

3. Return to Overworld
   **Expected Result**: Barriers present again

4. Travel to End
   **Expected Result**: No barriers in End, free exploration

5. Use `/execute in the_nether run` to check specific chunks in Nether
   **Expected Result**: No barrier blocks found in Nether

**Edge Cases**:

- Travel between Overworld and Nether multiple times
- Complete advancements in Nether (verify chunk unlock only affects Overworld barriers)

---

## Summary

**All Phase 2 tests passing** indicates:

- ✅ Smart barrier placement working (only at locked/unlocked boundaries)
- ✅ Lazy loading functional (handles async chunk loading)
- ✅ Batch operations optimized (no lag on large unlocks)
- ✅ Particle feedback working (throttled, visible)
- ✅ Sound feedback working (throttled, audible)
- ✅ Action bar messages functional (informative, throttled)
- ✅ Multiplayer synchronized correctly
- ✅ Overworld-only enforcement working
- ✅ Edge cases handled appropriately

**Next Steps**: Proceed to Phase 3 implementation (interaction blocking, ender pearl prevention).
