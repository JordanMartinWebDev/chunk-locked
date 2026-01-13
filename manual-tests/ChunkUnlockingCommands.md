# Chunk Unlocking Commands - Manual Test Cases

## Overview

Test cases for chunk unlocking commands: `/chunklocked givecredits` and `/chunklocked unlockfacing`.

## Test Case 1: Give Credits to Self

**Objective**: Verify that `/chunklocked givecredits <amount>` correctly adds credits to the player.

**Prerequisites**:

- Fresh world with player joined
- Player has 0 credits initially

**Steps**:

1. Run `/chunklocked givecredits 5`
2. Check inventory or run `/chunklocked credits`
3. Verify credit count shows 5

**Expected Result**:

- Chat shows: "Given 5 credits (0 → 5)"
- Credits increase by 5
- Can unlock chunks with new credits

**Edge Cases**:

- Give 0 credits: Should be rejected (requires 1+)
- Give negative credits: Should be rejected
- Give huge amount (999): Should work without error

---

## Test Case 1b: Reset Credits (Self)

**Objective**: Verify `/chunklocked credits reset` sets credits to 0 for the executing player.

**Prerequisites**:

- Player has non-zero credits

**Steps**:

1. Run `/chunklocked credits reset`
2. Run `/chunklocked credits`

**Expected Result**:

- Chat shows: "Reset your credits to 0"
- Credits display shows 0

---

## Test Case 2: Give Credits to Another Player

**Objective**: Verify that `/chunklocked givecredits <player> <amount>` works for other players.

**Prerequisites**:

- 2 players online (Player A as executor, Player B as target)
- Player B has 0 credits

**Steps**:

1. As Player A, run `/chunklocked givecredits Player B 3`
2. Check Player B's credits
3. Verify both players see feedback messages

**Expected Result**:

- Player A sees: "Gave 3 credits to Player B (0 → 3)"
- Player B receives chat message: "You received 3 credits (0 → 3)"
- Player B can unlock chunks with new credits

**Edge Cases**:

- Non-existent player name: Should show error
- Offline player: Should show error
- Executor is not OP: Should be denied (admin command)

---

## Test Case 2b: Reset Credits (Other Player)

**Objective**: Verify `/chunklocked credits reset <player>` sets credits to 0 for the target player.

**Prerequisites**:

- Two players online; target has non-zero credits

**Steps**:

1. As admin, run `/chunklocked credits reset <player>`
2. Target runs `/chunklocked credits`

**Expected Result**:

- Admin sees: "Reset <player>'s credits to 0"
- Target sees: "Your credits were reset to 0"
- Target’s credits display shows 0

---

## Test Case 3: Unlock Chunk Facing - North Direction

**Objective**: Verify raycasting correctly identifies and unlocks chunk to the north.

**Prerequisites**:

- 2x2 spawn area unlocked (4 chunks)
- Player has at least 1 credit
- Chunks to north are locked

**Steps**:

1. Face directly north (yaw = 180°)
2. Run `/chunklocked unlockfacing`
3. Look at the world to verify
4. Check barriers are gone from that direction

**Expected Result**:

- Chat shows: "Unlocked chunk North [X, Z-1]!"
- Barrier blocks disappear from that direction
- Can move freely into the new chunk
- Adjacent locked chunks still have barriers

**Edge Cases**:

- Look slightly up (60° pitch): Should still detect north boundary
- Look slightly down (-60° pitch): Should still detect north boundary
- Look too steeply (90° pitch straight up): Should fail gracefully

---

## Test Case 4: Unlock Chunk Facing - South Direction

**Objective**: Verify raycasting correctly identifies and unlocks chunk to the south.

**Prerequisites**:

- 2x2 spawn area unlocked
- Player has at least 1 credit
- Chunks to south are locked

**Steps**:

1. Face directly south (yaw = 0°)
2. Run `/chunklocked unlockfacing`
3. Verify correct direction detected
4. Check barriers are gone

**Expected Result**:

- Chat shows: "Unlocked chunk South [X, Z+1]!"
- Barriers removed from south side
- New chunk accessible

---

## Test Case 5: Unlock Chunk Facing - East Direction

**Objective**: Verify raycasting correctly identifies and unlocks chunk to the east.

**Prerequisites**:

- 2x2 spawn area unlocked
- Player has at least 1 credit
- Chunks to east are locked

**Steps**:

1. Face directly east (yaw = 270°)
2. Run `/chunklocked unlockfacing`
3. Verify chunk east is unlocked

**Expected Result**:

- Chat shows: "Unlocked chunk East [X+1, Z]!"
- Barriers removed from east side

---

## Test Case 6: Unlock Chunk Facing - West Direction

**Objective**: Verify raycasting correctly identifies and unlocks chunk to the west.

**Prerequisites**:

- 2x2 spawn area unlocked
- Player has at least 1 credit
- Chunks to west are locked

**Steps**:

1. Face directly west (yaw = 90°)
2. Run `/chunklocked unlockfacing`
3. Verify chunk west is unlocked

**Expected Result**:

- Chat shows: "Unlocked chunk West [X-1, Z]!"
- Barriers removed from west side

---

## Test Case 7: Unlock Multiple Chunks in Same Direction

**Objective**: Verify raycasting correctly selects the FIRST locked chunk boundary, skipping internal unlocked boundaries.

**Prerequisites**:

- 2x2 spawn area unlocked: [7,-1], [8,-1], [7,0], [8,0]
- Player has 2+ credits
- Unlock one more chunk north: [7,-2]
- Now there are chunks at [7,-2] (locked), [7,-3] (still locked)

**Steps**:

1. Face north from the 2x2 area
2. Run `/chunklocked unlockfacing`
3. First unlock should target [7,-2]
4. Run again, should target [7,-3]

**Expected Result**:

- First unlock: "Unlocked chunk North [7, -2]!"
- Barriers between [7,-1] and [7,-2] disappear
- Second unlock: "Unlocked chunk North [7, -3]!"
- Barriers between [7,-2] and [7,-3] disappear
- No double-unlocking of already-unlocked chunks

**Edge Cases**:

- Skip internal boundaries: ✅ Raycasting should skip boundaries between [7,-1] and [7,-2] if both are unlocked
- Detect first locked boundary: ✅ Should stop at first locked chunk encountered

---

## Test Case 8: Cannot Unlock Already-Unlocked Chunk

**Objective**: Verify raycasting rejects already-unlocked chunks.

**Prerequisites**:

- Chunk [7,-2] is already unlocked
- Player facing that direction

**Steps**:

1. Face north toward [7,-2]
2. Run `/chunklocked unlockfacing`

**Expected Result**:

- Chat shows: "The chunk North of you [7, -2] is already unlocked!"
- No credit spent
- No barriers changed

---

## Test Case 9: Cannot Unlock Without Credits

**Objective**: Verify command fails gracefully when out of credits.

**Prerequisites**:

- Player has 0 credits
- Locked chunks available

**Steps**:

1. Face a locked chunk direction
2. Run `/chunklocked unlockfacing`

**Expected Result**:

- Chat shows: "Not enough credits! Complete more advancements."
- No chunk unlocked
- No barriers changed

---

## Test Case 10: Raycasting Range

**Objective**: Verify raycasting works up to 256 blocks away.

**Prerequisites**:

- Locked chunks far away (100+ blocks)
- Player has credits

**Steps**:

1. Build to 200 blocks away from locked chunk
2. Face the locked chunk
3. Run `/chunklocked unlockfacing`

**Expected Result**:

- Should successfully detect and unlock the distant chunk
- Works up to 256 blocks

**Edge Cases**:

- 257 blocks away: Should fail ("No locked chunk boundary found")
- Blocked line of sight (solid blocks): Ray should still work (not blocked by obstacles)

---

## Test Case 11: Barrier Persistence

**Objective**: Verify barriers remain after reload and chunks stay locked.

**Prerequisites**:

- 2x2 spawn area with frontier barriers
- Server restarted

**Steps**:

1. Check barriers are still present after reload
2. Try to move into locked chunk
3. Verify barriers block movement

**Expected Result**:

- Barriers are restored on server load
- Cannot move past barriers
- Chunks remain locked until explicitly unlocked

---

## Test Case 12: Barrier Update After Unlock

**Objective**: Verify barriers update correctly when adjacent chunk unlocks.

**Prerequisites**:

- Two adjacent locked chunks: [7,-2] (north) and [8,-1] (east of spawn)
- Barriers on all sides initially

**Steps**:

1. Unlock [7,-2] to the north
2. Check barrier walls update
3. North barrier from [7,-1] disappears
4. South barrier from [7,-2] appears (facing the 2x2)
5. Unlock [8,-1] to the east
6. Verify that chunk's barriers update

**Expected Result**:

- Barriers removed from unlocked chunks
- Barriers remain on locked chunks
- Frontier barrier configuration updates correctly
- No barriers between two unlocked chunks
- Barriers only on edges bordering locked areas

---

## Test Case 13: Unlock from Inside vs Outside

**Objective**: Verify unlocking works from any position within the unlocked area.

**Prerequisites**:

- 2x2 spawn area
- 3 credits available

**Steps**:

1. Stand in center of 2x2 area
2. Face east and unlock chunk east
3. Move into newly unlocked chunk
4. Face east again and unlock next chunk
5. Verify all directions work

**Expected Result**:

- Unlocking works regardless of position
- Barriers update correctly each time
- Can walk into newly unlocked areas

---

## Summary of Commands Tested

```
# Give self credits
/chunklocked givecredits 5

# Give other player credits
/chunklocked givecredits Player205 3

# Unlock chunk in facing direction
/chunklocked unlockfacing

# Check barrier configuration
/chunklocked debugbarriers

# Check own credits
/chunklocked credits
```

## Known Limitations

- Raycasting doesn't respect block line-of-sight (barriers are invisible, so this is correct)
- Maximum range is 256 blocks
- Must face surface of chunk boundary (not corner)
- Only works in Overworld (by design)
- Only works in Overworld (by design)

---

## Test Case 14: Credit Persistence After Admin Grants

**Objective**: Ensure credits granted via admin commands persist across server restarts.

**Prerequisites**:

- Server running and player online
- Player initially has 0 credits

**Steps**:

1. As admin, run `/chunklocked givecredits <player> 3`.
2. As player, run `/chunklocked credits` and note value (should be 3).
3. Save and exit to title; stop the server.
4. Restart the server and rejoin with the same player.
5. Run `/chunklocked credits` again.

**Expected Result**: The credited amount persists across restart (value remains 3).

**Edge Cases**:

- Use `/chunklocked addcredits <amount>` and `/chunklocked setcredits <amount>` and repeat steps; values should persist.
- If credits are granted before `SERVER_STARTED`, check logs for warning about `persistentData` being null; there should be no crash.
