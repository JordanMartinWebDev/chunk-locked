# Game Mode Variants - Manual Test Cases

**Feature**: Easy & Extreme difficulty modes with frame-based credit rewards  
**Test Date**: \***\*\_\_\_\*\***  
**Tester**: \***\*\_\_\_\*\***

---

## Test Case 1: World Creation with EASY Mode

**Prerequisites**: Clean Minecraft installation with mod installed

**Steps**:

1. Launch Minecraft
2. Click "Singleplayer" → "Create New World"
3. Click "World Type" dropdown
4. Select "Chunklocked: Easy"
5. Create and enter the world
6. Wait for world to load
7. Check server console for mode auto-detection
8. Check inventory for starter items

**Expected Result**:

- "Chunklocked: Easy" appears in world type dropdown
- World generates normally
- **NO COMMAND NEEDED** - mode is auto-detected
- Console logs: "User selected Chunklocked: Easy preset"
- Console logs: "Detected Easy mode from marker file"
- Console logs: "Chunklocked mode: EASY"
- Player spawns in 2x2 chunk area with barriers visible
- Starter items AUTOMATICALLY received (3 Oak Saplings + 10 Bone Meal)
- Chat message: "You received starter items!"

**Edge Cases**:

- Verify world type persists if you cancel and reopen creation screen
- Verify difficulty setting doesn't affect Chunklocked mode

---

## Test Case 2: World Creation with EXTREME Mode

**Prerequisites**: Clean Minecraft installation with mod installed

**Steps**:

1. Launch Minecraft
2. Click "Singleplayer" → "Create New World"
3. Click "World Type" dropdown
4. Select "Chunklocked: Extreme"
5. Create and enter the world
6. Check server console for auto-detection
7. Check inventory for starter items

**Expected Result**:

- "Chunklocked: Extreme" appears in world type dropdown
- World generates normally
- **NO COMMAND NEEDED** - mode is auto-detected
- Console logs: "User selected Chunklocked: Extreme preset"
- Console logs: "Detected Extreme mode from marker file"
- Console logs: "Chunklocked mode: EXTREME"
- Player spawns in 2x2 chunk area with barriers
- Starter items AUTOMATICALLY received (3 Oak Saplings + 10 Bone Meal)
- Chat message: "You received starter items!"

**Edge Cases**:

- Compare with EASY mode - world generation should be identical
- Only difference is credit rewards

---

## Test Case 3: World Creation with Default Type (DISABLED Mode)

**Prerequisites**: Clean Minecraft installation with mod installed

**Steps**:

1. Create new world with "Default" world type
2. Enter the world
3. Check server console
4. Try to place barriers manually (should not be blocked)
5. Complete an advancement (e.g., get wood)

**Expected Result**:

- World generates normally without mod interference
- Console logs: "Chunklocked mode is DISABLED. Use /chunklocked set_mode to enable..."
- No barriers placed anywhere
- Player can move freely in all directions
- Advancement completion does NOT grant credits
- NO starter items received

**Edge Cases**:

- Verify mod is completely inactive (no performance impact)
- Verify you can enable mode later with `/chunklocked set_mode easy`

---

## Test Case 4: Credit Rewards in EASY Mode (TASK/GOAL/CHALLENGE)

**Prerequisites**: World created with "Chunklocked: Easy" world type (auto-detects mode)

**Steps**:

1. Complete a TASK advancement (e.g., "Stone Age" - pick up cobblestone)
   - Run `/chunklocked credits` - check credit count
2. Complete a GOAL advancement (e.g., "Hot Stuff" - fill bucket with lava)
   - Run `/chunklocked credits` - check credit count
3. Complete a CHALLENGE advancement (e.g., "Postmortal" - use totem of undying)
   - Run `/chunklocked credits` - check credit count

**Expected Result**:

- TASK advancement grants +1 credit (e.g., "Stone Age")
- GOAL advancement grants +5 credits (e.g., "Hot Stuff")
- CHALLENGE advancement grants +10 credits (e.g., "Postmortal")
- Console logs each credit reward
- `/chunklocked credits` shows updated count

**Edge Cases**:

- Recipe advancements grant 0 credits (filtered out)
- Blacklisted advancements grant 0 credits
- Completing same advancement twice grants 0 credits (already rewarded)

---

## Test Case 5: Credit Rewards in EXTREME Mode (Reduced Rewards)

**Prerequisites**: World created with "Chunklocked: Extreme" world type (auto-detects mode)

**Steps**:

1. Complete a TASK advancement (e.g., "Stone Age")
   - Check credit count
2. Complete a GOAL advancement (e.g., "Hot Stuff")
   - Check credit count
3. Complete a CHALLENGE advancement (e.g., "Postmortal")
   - Check credit count

**Expected Result**:

- TASK advancement grants +1 credit (same as EASY)
- GOAL advancement grants +2 credits (reduced from 5)
- CHALLENGE advancement grants +5 credits (reduced from 10)
- Credit accumulation is much slower than EASY mode

**Edge Cases**:

- Compare with EASY mode - EXTREME should feel significantly harder
- Verify all filtering (recipes, blacklist) still works

---

## Test Case 6: Starter Items on First Join (EASY/EXTREME)

**Prerequisites**: New world with "Chunklocked: Easy" or "Chunklocked: Extreme" preset

**Steps**:

1. Create world with Easy or Extreme preset
2. Join world for first time (mode auto-detects)
3. Open inventory (E key)
4. Check for Oak Saplings and Bone Meal
5. Leave world and rejoin
6. Check inventory again

**Expected Result**:

- **First Join**: Inventory contains 3 Oak Saplings + 10 Bone Meal
- Chat message: "You received starter items!"
- **Rejoin**: NO additional items given
- NBT data persists across saves

**Edge Cases**:

- Full inventory: Items drop at player's feet
- Multiple players: Each receives items once
- Items can be used/dropped/crafted normally

---

## Test Case 7: Starter Items NOT Given in DISABLED Mode

**Prerequisites**: World with Default world type (DISABLED mode)

**Steps**:

1. Create world without Chunklocked world type
2. Join world
3. Check inventory

**Expected Result**:

- NO starter items received
- No chat message about starter items
- Inventory is empty except for default spawn items

**Edge Cases**:

- Enabling mode later with command does NOT give retroactive starter items

---

## Test Case 8: Mode Persistence Across Save/Load

**Prerequisites**: World with EASY mode set

**Steps**:

1. Create world, set mode to EASY
2. Complete 2-3 advancements to earn credits
3. Unlock 1-2 chunks using credits
4. Save and quit to title
5. Reload the world
6. Check mode with console logs
7. Check credits with `/chunklocked credits`
8. Check unlocked chunks still accessible

**Expected Result**:

- Mode remains EASY after reload
- Credits persist correctly
- Unlocked chunks remain unlocked
- Barriers remain in correct positions
- No data loss

**Edge Cases**:

- Restart entire Minecraft client
- Copy world folder to different location
- Verify NBT file contains correct mode

---

## Test Case 9: Mode Immutability (Cannot Change After Set)

**Prerequisites**: World with mode already set to EASY

**Steps**:

1. Create world, set mode to EASY
2. Try to change mode to EXTREME: `/chunklocked set_mode extreme`
3. Check command response
4. Try to change mode to DISABLED: `/chunklocked set_mode disabled`
5. Check command response
6. Verify mode is still EASY

**Expected Result**:

- Command fails with error message
- Error: "Cannot change mode from EASY to EXTREME. Mode can only be set once per world."
- Explanation: "(This prevents players from switching modes to exploit the system)"
- Mode remains EASY
- No data corruption

**Edge Cases**:

- Setting same mode again (EASY → EASY) should succeed (no-op)
- Verify this prevents exploit where player switches to EASY to farm credits

---

## Test Case 10: Barrier Toggle Command - Disable

**Prerequisites**: World with barriers visible (locked chunks)

**Steps**:

1. Create world with barriers
2. Note down barrier locations (F3 + G to see chunk boundaries)
3. Run `/chunklocked toggle_barriers`
4. Check command feedback
5. Walk to where barriers were
6. Verify barriers are gone
7. Check console logs

**Expected Result**:

- Command succeeds with message: "Barriers disabled! (Removed X barriers)"
- Warning: "Progression still active. Use /chunklocked toggle_barriers again to restore."
- All barrier blocks removed from world
- Player can walk through previous barrier locations
- Progression still tracks (credits, advancements)
- Console logs: "Barriers disabled by <player>. Removed X barriers. Progression still tracked."

**Edge Cases**:

- Verify removing barriers doesn't break other systems
- Verify player can still unlock chunks normally
- Verify credits still accumulate

---

## Test Case 11: Barrier Toggle Command - Re-Enable

**Prerequisites**: World with barriers disabled (from Test Case 10)

**Steps**:

1. Continue from previous test with barriers disabled
2. Run `/chunklocked toggle_barriers` again
3. Check command feedback
4. Walk to chunk boundaries
5. Verify barriers are back
6. Try to walk through barrier
7. Check console logs

**Expected Result**:

- Command succeeds: "Barriers restored! (X chunks protected)"
- Barriers reappear at all chunk boundaries
- Player cannot pass through barriers
- Barriers block player but not mobs/water/items
- Console logs: "Barriers re-enabled by <player>. Restored barriers for X locked chunks."

**Edge Cases**:

- Toggle multiple times rapidly - should work consistently
- Verify barrier count matches expected (4 sides per locked chunk adjacent to unlocked)

---

## Test Case 12: Barrier State Persistence

**Prerequisites**: World with barriers disabled

**Steps**:

1. Disable barriers with `/chunklocked toggle_barriers`
2. Verify barriers are gone
3. Save and quit to title
4. Reload world
5. Check if barriers are still gone or restored
6. Check console logs

**Expected Result**:

- Barrier state persists: barriers remain DISABLED after reload
- Console logs: "Barriers enabled: false"
- Player can still move freely
- No auto-restore (3.2 was deferred)

**Edge Cases**:

- Verify NBT contains barriersEnabled flag
- Verify flag defaults to true for old saves

---

## Test Case 13: Recipe Advancement Filtering (No Credits)

**Prerequisites**: World with EASY mode

**Steps**:

1. Create world, set EASY mode
2. Craft a wooden pickaxe (triggers recipe advancement)
3. Check credits with `/chunklocked credits`
4. Craft several more items (planks, sticks, etc.)
5. Check credits again

**Expected Result**:

- Recipe advancements do NOT grant credits
- `/chunklocked credits` count unchanged
- Console logs show advancements but no credit rewards
- Only actual achievements (TASK/GOAL/CHALLENGE) grant credits

**Edge Cases**:

- Verify all recipe advancements filtered (100+ recipes)
- Verify recipe unlocking still works normally (recipe book)

---

## Test Case 14: Blacklist Honoring in All Modes

**Prerequisites**: World with EASY or EXTREME mode, config with blacklisted advancement

**Steps**:

1. Edit config to blacklist an advancement (e.g., "story/mine_stone")
2. Restart world
3. Complete the blacklisted advancement (mine stone)
4. Check credits

**Expected Result**:

- Blacklisted advancement completion detected
- Console logs advancement but no credit reward
- Credits remain unchanged
- Progression tracking unaffected

**Edge Cases**:

- Verify blacklist works for all frame types (TASK/GOAL/CHALLENGE)
- Verify blacklist works in both EASY and EXTREME modes

---

## Test Case 15: Multiplayer Credit Synchronization

**Prerequisites**: Server with EASY mode, 2+ players

**Steps**:

1. Player 1 joins server
2. Player 2 joins server
3. Player 1 completes advancement
4. Player 1 checks credits: `/chunklocked credits`
5. Player 2 checks Player 1's credits: `/chunklocked credits <player1>`
6. Player 2 completes different advancement
7. Both players check credits

**Expected Result**:

- Each player has independent credit count
- Credits sync correctly to both clients
- `/chunklocked credits` shows own credits
- `/chunklocked credits <player>` shows other player's credits (admin)
- No credit duplication or loss

**Edge Cases**:

- Player joins after advancements completed - credits still accurate
- Player disconnects during advancement - credits sync on rejoin

---

## Test Case 16: Starter Items with Full Inventory

**Prerequisites**: New world with EASY mode

**Steps**:

1. Use creative mode or commands to fill inventory
2. Set mode to EASY (if not already)
3. Relog to trigger starter items
4. Check ground around player spawn
5. Pick up items

**Expected Result**:

- Starter items drop at player's feet (entity items)
- Items are visible on ground
- Items can be picked up normally
- No items lost/deleted

**Edge Cases**:

- Player in midair when items drop - items fall down
- Player in water - items float/drop
- Player in lava - items may burn (expected)

---

## Regression Tests

### Test Case 17: Existing Features Still Work

**Prerequisites**: World with mode enabled

**Steps**:

1. Unlock chunks with credits
2. Use portal mechanics
3. Test penalty system
4. Test barrier block collision (player blocked, mobs pass)
5. Test command permissions

**Expected Result**:

- All existing features work as before
- No regressions introduced by mode system
- Integration is seamless

**Edge Cases**:

- Mode DISABLED should completely disable mod
- Mode EASY/EXTREME should enable all features

---

## Summary Checklist

- [ ] All 17 test cases executed
- [ ] No critical bugs found
- [ ] Performance acceptable
- [ ] Multiplayer tested
- [ ] Documentation updated with findings
- [ ] Known issues documented
