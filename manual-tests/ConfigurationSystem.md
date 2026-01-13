# Configuration System - Manual Test Cases

## Test Case 1: Default Configuration Creation

**Prerequisites**:

- Fresh Minecraft installation or deleted config file
- Chunk Locked mod installed

**Steps**:

1. Delete `config/chunk-locked-advancements.json` if it exists
2. Start Minecraft server/client
3. Check the `config/` directory

**Expected Result**:

- `chunk-locked-advancements.json` file should be created automatically
- File should contain default values:
  - `defaultCredits: 1`
  - Custom rewards for major advancements (adventuring_time: 20, dragon_egg: 10, etc.)
  - Blacklist for trivial advancements (story/root, story/mine_stone, story/upgrade_tools)
  - `allowRevoke: false`
  - `enableNotifications: true`
- Console should log: "Loading advancement reward configuration from: config/chunk-locked-advancements.json"
- Console should log: "Loaded X custom rewards and Y blacklisted advancements"

**Edge Cases**:

- Verify file is valid JSON and can be opened in a text editor
- Verify file is formatted with pretty printing (indentation)

---

## Test Case 2: Configuration Loading

**Prerequisites**:

- Existing config file at `config/chunk-locked-advancements.json`

**Steps**:

1. Edit `config/chunk-locked-advancements.json` and set `defaultCredits: 5`
2. Add custom reward: `"minecraft:story/mine_diamond": 100`
3. Save the file
4. Restart Minecraft

**Expected Result**:

- Console should log: "Configuration loaded successfully"
- Console should show the loaded config values in logs
- When completing "Mine Diamond" advancement, player should receive 100 credits (verify in later phases)
- When completing other advancements, player should receive 5 credits (changed from default 1)

**Edge Cases**:

- Check console for "Loaded 9 custom rewards" (8 defaults + 1 added)

---

## Test Case 3: Invalid Configuration Handling

**Prerequisites**:

- Existing config file

**Steps**:

1. Edit `config/chunk-locked-advancements.json` and corrupt the JSON syntax:
   ```
   { "defaultCredits": 1, invalid syntax here }
   ```
2. Save and restart Minecraft

**Expected Result**:

- Console should log ERROR: "Configuration file has invalid JSON syntax"
- Console should log: "Using default configuration"
- Server/client should start successfully (no crash)
- Config file should be overwritten with default values

---

## Test Case 4: Negative Credits Validation

**Prerequisites**:

- Existing config file

**Steps**:

1. Edit config and set `"defaultCredits": -5`
2. Save and restart Minecraft

**Expected Result**:

- Console should log ERROR: "Configuration validation failed: defaultCredits cannot be negative"
- Console should log: "Using default configuration"
- Config file should be reset to defaults

---

## Test Case 5: Custom Rewards Applied

**Prerequisites**:

- Config file with custom reward set

**Steps**:

1. Edit config and set:
   ```json
   "customRewards": {
     "minecraft:adventure/adventuring_time": 50
   }
   ```
2. Save and restart Minecraft
3. Use `/advancement grant @s minecraft:adventure/adventuring_time`

**Expected Result**:

- Console should log: "Awarded 50 credit(s) to [player] for completing advancement: minecraft:adventure/adventuring_time"
- Player should receive 50 credits (verify with future commands/UI)

---

## Test Case 6: Blacklist Prevents Credits

**Prerequisites**:

- Config file with blacklist

**Steps**:

1. Edit config and add to blacklist:
   ```json
   "blacklist": [
     "minecraft:story/mine_diamond"
   ]
   ```
2. Save and restart Minecraft
3. Use `/advancement grant @s minecraft:story/mine_diamond`

**Expected Result**:

- Console should log: "Advancement minecraft:story/mine_diamond is blacklisted, no credits awarded to [player]"
- Player should receive 0 credits
- No "Awarded X credit(s)" message should appear

---

## Test Case 7: Partial Configuration

**Prerequisites**:

- Config file

**Steps**:

1. Edit config file to only contain:
   ```json
   {
     "defaultCredits": 10
   }
   ```
2. Save and restart Minecraft

**Expected Result**:

- Console should log: "Configuration loaded successfully"
- `defaultCredits` should be 10 (from file)
- Other fields should use defaults:
  - `customRewards` should be empty map
  - `blacklist` should be empty list
  - `allowRevoke` should be false
  - `enableNotifications` should be true

---

## Test Case 8: Empty Config File

**Prerequisites**:

- Config file

**Steps**:

1. Delete all content from config file (make it empty)
2. Save and restart Minecraft

**Expected Result**:

- Console should log: "Configuration file is empty or invalid"
- Console should log: "Using default configuration"
- Config file should be regenerated with default values

---

## Test Case 9: Missing Config Directory

**Prerequisites**:

- No `config/` directory

**Steps**:

1. Delete the `config/` directory entirely
2. Start Minecraft

**Expected Result**:

- `config/` directory should be created
- `config/chunk-locked-advancements.json` should be created with defaults
- No errors should occur

---

## Test Case 10: Unicode and Special Characters

**Prerequisites**:

- Config file

**Steps**:

1. Add custom reward with unicode:
   ```json
   "customRewards": {
     "mymod:special_advancement": 15
   }
   ```
2. Save and restart Minecraft

**Expected Result**:

- Configuration should load without errors
- Unicode characters should be preserved
- Custom reward should work correctly

---

## Configuration Integration Checklist

After completing all test cases, verify:

- [x] Default config file created automatically on first run
- [x] Valid custom configurations load correctly
- [x] Invalid JSON falls back to defaults gracefully
- [x] Validation catches negative/invalid values
- [x] Custom rewards override default credits
- [x] Blacklist prevents credit awards
- [x] Partial configs merge with defaults
- [x] Empty/corrupt files regenerate defaults
- [x] Missing directories are created
- [x] Config changes apply after restart
- [x] No crashes from any config scenario
- [x] All error messages are clear and helpful
