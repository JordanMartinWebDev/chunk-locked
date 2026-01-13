# Advancement Integration - Manual Test Cases

## Test Case 1: Basic Advancement Completion Detection

**Prerequisites**:

- Mod installed and loaded in a new world
- Player in survival mode

**Steps**:

1. Start a new Minecraft world with the mod installed
2. Open the console/logs to view debug output
3. Complete a simple advancement (e.g., "Getting Wood" - collect wood from a tree)
4. Observe the console output

**Expected Result**:
Console should display a log message:

```
[chunk-locked] Player <PlayerName> completed advancement: minecraft:story/mine_wood (criterion: has_log)
```

**Edge Cases**:

- Complete the same advancement again (shouldn't be possible without /advancement revoke)
- Complete advancement with multiple criteria (only fires when ALL criteria met)

---

## Test Case 2: Multiple Advancement Completions

**Prerequisites**:

- Mod installed and loaded
- Player in survival mode with some resources

**Steps**:

1. Start a new world
2. Complete "Getting Wood" advancement
3. Craft a crafting table to complete "Benchmarking" advancement
4. Craft a wooden pickaxe to complete "Time to Mine!" advancement
5. Check console logs after each completion

**Expected Result**:
Three separate log messages should appear:

```
[chunk-locked] Player <PlayerName> completed advancement: minecraft:story/mine_wood (criterion: has_log)
[chunk-locked] Player <PlayerName> completed advancement: minecraft:story/craft_crafting_table (criterion: has_crafting_table)
[chunk-locked] Player <PlayerName> completed advancement: minecraft:story/craft_pickaxe (criterion: has_wooden_pickaxe)
```

**Edge Cases**:

- Complete advancements rapidly in succession
- Complete advancements in different order than designed progression

---

## Test Case 3: Multi-Criteria Advancement

**Prerequisites**:

- Mod installed and loaded
- Player has basic tools and resources

**Steps**:

1. Start working toward "Two Birds, One Arrow" advancement
2. Partially complete criteria (e.g., have a bow but not yet killed two entities)
3. Observe console - should see NO message yet
4. Complete the final criterion (kill two phantoms with one arrow)
5. Check console after full completion

**Expected Result**:

- No log message appears during partial progress
- Log message appears ONLY when final criterion is met:

```
[chunk-locked] Player <PlayerName> completed advancement: minecraft:adventure/two_birds_one_arrow (criterion: <final_criterion_name>)
```

**Edge Cases**:

- Complete criteria out of order
- Complete multiple criteria in the same game tick

---

## Test Case 4: Command-Based Advancement Grant

**Prerequisites**:

- Mod installed and loaded
- Player has OP permissions or in single player

**Steps**:

1. Start a world and open chat
2. Use command: `/advancement grant @s only minecraft:story/mine_wood`
3. Observe console output
4. Use command: `/advancement revoke @s only minecraft:story/mine_wood`
5. Use command: `/advancement grant @s only minecraft:story/mine_wood` again
6. Observe console output

**Expected Result**:

- First grant triggers log message
- Revoke does NOT trigger any message
- Second grant (after revoke) triggers log message again

---

## Test Case 5: Advancement Completion in Multiplayer

**Prerequisites**:

- Mod installed on server
- Two players connected to server

**Steps**:

1. Start a multiplayer server with mod installed
2. Player 1 completes "Getting Wood" advancement
3. Check server console
4. Player 2 completes "Getting Wood" advancement
5. Check server console again

**Expected Result**:
Two separate log messages:

```
[chunk-locked] Player Player1 completed advancement: minecraft:story/mine_wood (criterion: has_log)
[chunk-locked] Player Player2 completed advancement: minecraft:story/mine_wood (criterion: has_log)
```

**Edge Cases**:

- Both players complete same advancement simultaneously
- One player completes multiple advancements while other is online
- Player completes advancement then disconnects immediately

---

## Test Case 6: Advancement Progress Persistence

**Prerequisites**:

- Mod installed and loaded
- Partial progress on a multi-criteria advancement

**Steps**:

1. Start working on "The Parrots and the Bats" (breed all animals)
2. Breed several but not all required animals
3. Save and quit the world
4. Restart the world
5. Breed the remaining required animals to complete the advancement
6. Check console

**Expected Result**:

- No messages during partial progress before or after restart
- Log message appears only when final animal is bred:

```
[chunk-locked] Player <PlayerName> completed advancement: minecraft:husbandry/bred_all_animals (criterion: <final_animal>)
```

---

## Test Case 7: No Fake Player Detection

**Prerequisites**:

- Mod installed
- Access to a fake player mod/system (e.g., Carpet mod with fake players)

**Steps**:

1. Install both this mod and Carpet mod (or equivalent)
2. Spawn a fake player using `/player <name> spawn`
3. Grant advancement to fake player: `/advancement grant <fake_player> only minecraft:story/mine_wood`
4. Check console

**Expected Result**:
No log message should appear for fake player advancement completion
(Fake players are filtered by the mixin injection point)

**Note**: This test requires additional mods and may be optional

---

## Test Case 8: Hidden Advancement Completion

**Prerequisites**:

- Mod installed
- Ability to complete a hidden advancement

**Steps**:

1. Prepare resources to complete a hidden advancement (e.g., "How Did We Get Here?" effect)
2. Complete the hidden advancement
3. Check console

**Expected Result**:
Log message appears even for hidden advancements:

```
[chunk-locked] Player <PlayerName> completed advancement: minecraft:nether/all_effects (criterion: <criterion>)
```

---

## Test Case 9: Data Pack Custom Advancements

**Prerequisites**:

- Mod installed
- Custom advancement data pack installed

**Steps**:

1. Install a data pack with custom advancements
2. Reload data packs: `/reload`
3. Complete one of the custom advancements
4. Check console

**Expected Result**:
Custom advancement completion is detected and logged:

```
[chunk-locked] Player <PlayerName> completed advancement: <datapack_namespace>:<advancement_path> (criterion: <criterion>)
```

**Edge Cases**:

- Custom advancement with multiple criteria
- Custom advancement that grants other advancements
- Malformed custom advancement (should not crash)

---

## Test Case 10: Rapid Advancement Spam

**Prerequisites**:

- Mod installed
- OP permissions

**Steps**:

1. Prepare a command block or command sequence
2. Grant and revoke the same advancement rapidly:
   ```
   /advancement grant @s only minecraft:story/mine_wood
   /advancement revoke @s only minecraft:story/mine_wood
   /advancement grant @s only minecraft:story/mine_wood
   /advancement revoke @s only minecraft:story/mine_wood
   /advancement grant @s only minecraft:story/mine_wood
   ```
3. Check console and game performance

**Expected Result**:

- Each grant produces a log message
- No performance degradation
- No duplicate/missing messages
- No crashes or errors

---

## Verification Checklist

After completing all test cases, verify:

- [ ] Event fires for single-criterion advancements
- [ ] Event fires for multi-criterion advancements (only when complete)
- [ ] Event does NOT fire for partial progress
- [ ] Event includes correct player name in logs
- [ ] Event includes correct advancement identifier
- [ ] Event includes correct criterion name
- [ ] Multiple players tracked independently
- [ ] Works with command-granted advancements
- [ ] Works with data pack custom advancements
- [ ] No fake player advancements logged
- [ ] No performance issues with rapid completions
- [ ] No memory leaks over extended play sessions
- [ ] Advancement progress persists across restarts

---

## Known Limitations

- Fake player filtering relies on Minecraft's `isEffectiveAi()` check in `PlayerAdvancementTracker.grantCriterion()`
- Custom advancement data packs must be properly formatted to be detected
- Advancement revocation does NOT trigger any event (by design)

---

## Debugging Tips

If events are not firing:

1. Check that mixin is registered in `chunk-locked.mixins.json`
2. Verify mixin is loading at startup (check logs for mixin messages)
3. Confirm event listener is registered in `Chunklocked.onInitialize()`
4. Check that advancement is actually completing (use `/advancement` command)
5. Verify mod is loaded in the correct environment (server-side)

---

**Test Document Version**: 1.0  
**Last Updated**: January 10, 2026  
**Phase**: 1 - Custom Event Infrastructure
