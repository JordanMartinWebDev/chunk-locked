# Portal Mechanics & Locked Chunk Penalties - Manual Test Cases

## Test Setup

**Prerequisites**:

- Chunk Locked mod installed
- Single player world with cheats enabled
- Advancement tracking UI visible
- Action bar messages visible

**Initial Setup**:

```
/chunklocked givecredits 5      # Start with 5 credits
/chunklocked debugbarriers      # Show frontier chunks and barriers
```

---

## Test Case 1: Nether Portal - Auto-Unlock with Credits

**Scenario**: Player exits Nether into locked chunk while having available credits

**Steps**:

1. Start in Overworld, unlocked chunk area (barriers visible)
2. `/chunklocked givecredits 5` - Ensure you have 5 credits
3. Note current chunk coordinates
4. Build a Nether portal and enter it
5. Travel in Nether to be far from portal location
6. Return through portal, exit into a **locked chunk**
7. Observe action bar message immediately

**Expected Results**:

- ✅ Action bar message: "§a✓ Chunk auto-unlocked via Nether portal! (Credits remaining: 4)"
- ✅ Barriers around the newly unlocked chunk **disappear**
- ✅ New barriers appear around adjacent locked chunks
- ✅ Credit count decreases by 1
- ✅ Chunk is now marked as unlocked globally

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 2: Nether Portal - No Unlock (No Credits)

**Scenario**: Player exits Nether into locked chunk with zero credits, Wither penalty applies

**Steps**:

1. `/chunklocked givecredits -999` - Remove all credits
2. Build a Nether portal
3. Travel in Nether, return through portal
4. Exit into a locked chunk
5. Observe immediately
6. Wait 5 seconds and observe again

**Expected Results**:

- ✅ Action bar message: "§a✓ Chunk auto-unlocked..." does NOT appear
- ✅ Within 1 second: Wither II effect appears (darkening screen effect)
- ✅ After 5 seconds: Warning message appears:
  - "§c⚠ WARNING: You are in a locked chunk with no credits!"
  - "§c⚠ Return to an unlocked chunk to escape the Wither effect"
- ✅ Health bar shows Wither effect icon (gray skull)
- ✅ Wither effect reapplies after 3 seconds even if you drink milk

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 3: Escape Wither - Walk to Unlocked Chunk

**Scenario**: Player in locked chunk with Wither effect walks to adjacent unlocked chunk

**Steps**:

1. Set up: Zero credits, in locked chunk (Wither active)
2. Wait for Wither effect to be clearly visible
3. Walk toward nearest unlocked chunk (follow where barriers end)
4. Cross into unlocked chunk area
5. Observe immediately

**Expected Results**:

- ✅ Wither effect **immediately disappears** (gray skull icon vanishes)
- ✅ Health returns to normal color
- ✅ No more warning messages

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 4: Escape Wither - Enter Nether

**Scenario**: Player in locked chunk with Wither effect uses Nether portal to escape

**Steps**:

1. Set up: Zero credits, in locked chunk (Wither active)
2. Locate/create Nether portal
3. Enter Nether portal (trigger dimension change)
4. Observe in Nether

**Expected Results**:

- ✅ Wither effect **immediately disappears** upon entering Nether
- ✅ No Wither effect visible in Nether
- ✅ Can move freely in Nether without penalty

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 5: Milk Bucket Temporary Relief (Wither Reapply)

**Scenario**: Player uses milk bucket while in locked chunk with Wither, effect reapplies

**Steps**:

1. Set up: Zero credits, in locked chunk (Wither active)
2. Drink milk bucket
3. Observe immediately and at 1 second and 3 seconds

**Expected Results**:

- ✅ Wither effect **temporarily disappears** (instantly when milk consumed)
- ✅ At 1 second: Still no Wither (within reapply interval)
- ✅ At 3 seconds: Wither effect **reapplies** automatically
- ✅ Health shows damage again
- ✅ Gray skull icon returns
- ✅ Cannot permanently escape penalty with milk buckets

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 6: Return from Nether - Unlocked Chunk (No Penalty)

**Scenario**: Player returns from Nether to Overworld in unlocked chunk, no penalty should apply

**Steps**:

1. `/chunklocked givecredits 0` - Ensure zero credits
2. From unlocked chunk, enter Nether
3. Travel in Nether
4. Return to Overworld (via portal) - arrive in **unlocked chunk area**
5. Observe immediately

**Expected Results**:

- ✅ No Wither effect applies
- ✅ No warning messages
- ✅ Movement unrestricted
- ✅ Can move between unlocked chunks freely

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 7: Return from Nether - Locked Chunk (Penalty Applies)

**Scenario**: Player returns from Nether to Overworld in locked chunk, penalty should apply

**Steps**:

1. `/chunklocked givecredits 0` - Zero credits
2. From Overworld, go to locked chunk adjacent to your spawn
3. Enter Nether (note that locked chunk restrictions don't apply in Nether)
4. Return through portal, arrive back in same **locked chunk**
5. Observe immediately
6. Wait 5 seconds

**Expected Results**:

- ✅ Wither effect applies within 1 second
- ✅ Warning message appears at 5 seconds
- ✅ Cannot stay in locked chunk without penalty

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 8: Warning Message Frequency (No Spam)

**Scenario**: Verify warning messages appear frequently enough but not annoyingly often

**Steps**:

1. Set up: Zero credits, in locked chunk (Wither active)
2. Wait and observe warning message frequency
3. Note approximate time between messages
4. Stay in locked chunk for 30 seconds, count messages

**Expected Results**:

- ✅ Warning message appears every ~5 seconds
- ✅ Messages are frequent enough to remind player to leave
- ✅ Not so frequent that chat is spammed
- ✅ Approximately 6 messages in 30 seconds

**Actual Result**: ******\_\_\_******

- Message count in 30 seconds: \_\_\_
- Approximate frequency: \_\_\_ seconds

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 9: Multiple Players (No Cross-Interference)

**Scenario**: One player in locked chunk with penalty, another in unlocked chunk, no interference

**Steps**:

1. Player 1: `/chunklocked givecredits 0` - Zero credits
2. Player 1: Walk into locked chunk (observe Wither applies)
3. Player 2: Walk into same locked chunk BUT `/chunklocked givecredits 5` - Has credits
4. Observe both players

**Expected Results**:

- ✅ Player 1: Wither effect visible, warning messages
- ✅ Player 2: No Wither effect, can move freely
- ✅ Each player's state is independent
- ✅ No cross-effect between players

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Test Case 10: Barrier Updates After Portal Unlock

**Scenario**: Verify barriers correctly update after auto-unlocking chunk via Nether portal

**Steps**:

1. `/chunklocked debugbarriers` - Note frontier chunks and barrier positions
2. Note a locked chunk adjacent to unlocked area
3. Use Nether portal to unlock that locked chunk
4. Return to world and check barrier status
5. `/chunklocked debugbarriers` - Compare to previous state

**Expected Results**:

- ✅ Barriers removed from newly unlocked chunk
- ✅ Barriers appear on NEW frontier (if any adjacent chunks are still locked)
- ✅ Barrier positions are physically accurate (Y=-64 to Y=319, edge of chunks)
- ✅ No orphaned barriers

**Actual Result**: ******\_\_\_******

**Pass/Fail**: ☐ PASS ☐ FAIL

---

## Summary

| Test Case | Title                                    | Result        |
| --------- | ---------------------------------------- | ------------- |
| 1         | Nether Portal - Auto-Unlock with Credits | ☐ PASS ☐ FAIL |
| 2         | Nether Portal - No Unlock (No Credits)   | ☐ PASS ☐ FAIL |
| 3         | Escape Wither - Walk to Unlocked Chunk   | ☐ PASS ☐ FAIL |
| 4         | Escape Wither - Enter Nether             | ☐ PASS ☐ FAIL |
| 5         | Milk Bucket Temporary Relief             | ☐ PASS ☐ FAIL |
| 6         | Return from Nether - Unlocked Chunk      | ☐ PASS ☐ FAIL |
| 7         | Return from Nether - Locked Chunk        | ☐ PASS ☐ FAIL |
| 8         | Warning Message Frequency                | ☐ PASS ☐ FAIL |
| 9         | Multiple Players (No Cross-Interference) | ☐ PASS ☐ FAIL |
| 10        | Barrier Updates After Portal Unlock      | ☐ PASS ☐ FAIL |

**Overall Status**: \_\_\_ / 10 passed

---

## Notes

- List any bugs or issues discovered: ******\_\_\_******
- List any edge cases found: ******\_\_\_******
- Performance observations (lag, stuttering): ******\_\_\_******
- Suggestions for improvement: ******\_\_\_******
