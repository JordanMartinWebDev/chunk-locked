# Network Synchronization & Client Display - Manual Test Cases

## Overview

Phase 6 implements server-to-client synchronization of credit data via network packets. This document covers manual testing procedures to verify that credits are properly synchronized between server and client.

## Test Environment Setup

**Prerequisites**:

- Minecraft 1.21.11 with Chunk Locked mod loaded
- Single-player world or local multiplayer server
- Debug logging enabled to observe sync messages

**Setup Steps**:

1. Start a new world with the mod installed
2. Open chat (default: T key)
3. Optionally enable debug logging: `/say Debug mode enabled`

---

## Test Case 1: Credit Sync on First Login

**Description**: Verify that a player receives initial credit sync when joining the server

**Prerequisites**:

- Single-player world or active server
- First login to fresh world

**Steps**:

1. Join the world/server
2. Check client-side credit data is synchronized
3. Verify no errors in logs related to packet handling
4. Check chat for any sync confirmation messages (if logging enabled)

**Expected Result**:

- Client-side credit holder is updated with server data (0 credits for new player)
- No errors in console
- Subsequent UI elements can display credit count

**Edge Cases**:

- Test with player who has existing progression data
- Test on server reconnection
- Verify data persists after client-side cache

---

## Test Case 2: Credit Sync on Advancement Completion

**Description**: Verify that credits are immediately synced when player earns credits

**Prerequisites**:

- World loaded with player
- Player can access advancements

**Steps**:

1. Complete an advancement that awards credits
   - Example: Kill any mob (usually awards 1 credit)
2. Observe chat notification (if notifications enabled)
3. Check that client data is updated immediately
4. Verify no console errors during sync

**Expected Result**:

- Chat message shows credit award (if notifications enabled)
- Sound effect plays (experience orb sound)
- Client credit count updates to reflect new total
- No network errors in logs

**Edge Cases**:

- Complete advancement awarding 0 credits (blacklisted)
- Complete advancement awarding custom reward amount
- Complete advancement while offline (should sync on next login)
- Rapid advancement completions (test concurrent syncs)

---

## Test Case 3: Credit Sync Across Multiple Players

**Description**: Verify that credit sync is player-specific and doesn't leak data

**Prerequisites**:

- Local multiplayer server or online server with friends
- Multiple players connected

**Steps**:

1. Player A completes an advancement (earns 5 credits)
2. Player B observes their own credit count
3. Player A earns another advancement
4. Player B checks they still have their original credit count
5. Player B completes an advancement
6. Player A checks their credit count is unchanged

**Expected Result**:

- Each player's client shows only their own credits
- Player A's credits: 5 → updated amount after second advancement
- Player B's credits: unchanged until they earn advancement
- No cross-player credit data leakage

**Edge Cases**:

- One player joins while another earns credits (timing)
- Player disconnects mid-sync
- Multiple rapid completions by different players

---

## Test Case 4: Dimension Changes

**Description**: Verify credit sync maintains accuracy across dimension changes

**Prerequisites**:

- Player in Overworld
- Access to Nether or End

**Steps**:

1. Note current credit count in Overworld
2. Travel to Nether via portal
3. Check credit display still shows correct count
4. Complete an advancement in Nether
5. Verify credits update correctly
6. Return to Overworld
7. Verify credits are still correct

**Expected Result**:

- Credits display correctly in all dimensions
- Advancements completed in any dimension sync correctly
- No dimensional data isolation issues
- Credit count persists and updates consistently

**Edge Cases**:

- Travel between all three dimensions
- Complete advancement in Nether, return to Overworld
- Rapid dimension changes with advancement completion
- Disconnect in one dimension, reconnect in another

---

## Test Case 5: Disconnect and Reconnection

**Description**: Verify that credit data is properly restored on reconnection

**Prerequisites**:

- Player with earned credits
- Ability to disconnect and reconnect

**Steps**:

1. Earn some credits in the world
2. Note the credit count
3. Disconnect from server (quit game)
4. Close the game completely
5. Reopen game and rejoin the world
6. Check client credit data
7. Verify it matches the saved server state

**Expected Result**:

- Client credit data is reset on disconnect
- Client credit data is restored on reconnection
- All previously earned credits are still there
- No data loss or duplication

**Edge Cases**:

- Disconnect during advancement completion
- Rapid disconnect/reconnect cycles
- Rejoin different server (should have different credit data)
- Rejoin after server was shut down and restarted

---

## Test Case 6: Network Packet Integrity

**Description**: Verify that credit packets encode/decode correctly

**Prerequisites**:

- Game with packet logging enabled (optional)
- Network monitoring tools (optional, advanced)

**Steps**:

1. Enable debug logging with debug log level
2. Complete advancements and earn various credit amounts
3. Observe packet encode/decode messages in logs
4. Check that logged values match actual credits
5. Earn large credit amounts and verify no overflow
6. Earn zero credits and verify packet handling

**Expected Result**:

- Packet encode logs show correct values
- Packet decode logs show correct values
- No integer overflow on large values
- Zero credit amounts handled correctly
- Packet codec successfully round-trips all values

**Edge Cases**:

- Attempt to earn Integer.MAX_VALUE credits (system limit)
- Complete advancement awarding fractional credits (if supported)
- Corruption simulation (advanced, network packet inspection)

---

## Test Case 7: Error Handling

**Description**: Verify that the system gracefully handles errors

**Prerequisites**:

- Game with error logging enabled
- Ability to trigger edge cases

**Steps**:

1. Attempt to trigger network errors (advanced: kill network briefly)
2. Try invalid credit values (negative, overflow)
3. Disconnect during packet send
4. Spam advancement completions
5. Check logs for any unhandled exceptions

**Expected Result**:

- No uncaught exceptions in logs
- Negative credits rejected with warning
- Network disconnections handled gracefully
- High-frequency syncs don't crash system
- Error messages are clear and logged

**Edge Cases**:

- Network latency/lag scenarios
- Server thread contention
- Memory pressure while syncing data
- Malformed packet reception (security concern)

---

## Test Case 8: HUD/UI Display Integration

**Description**: Verify that synced credit data is available for UI rendering

**Prerequisites**:

- Mod with UI elements that display credits (future implementation)
- In-game UI visible

**Steps**:

1. Enable/open credit display HUD (when implemented)
2. Earn credits via advancements
3. Observe HUD updates in real-time
4. Complete advancement with custom reward
5. Verify HUD shows updated credit count
6. Switch to different UI views
7. Verify credit data persists in memory

**Expected Result**:

- HUD displays correct credit count
- HUD updates within 1 frame of sync packet
- Credit count visible in all HUD states
- No flickering or inconsistency in display
- UI accurately reflects server state

**Edge Cases**:

- UI hidden/shown during sync
- Quick succession of credit updates
- Rapid dimension changes with UI visible
- Long gameplay session (memory integrity)

---

## Test Case 9: Performance Impact

**Description**: Verify that network sync doesn't negatively impact performance

**Prerequisites**:

- Performance monitoring tools (F3 menu)
- Multiple players on server (if testing multiplayer)

**Steps**:

1. Establish baseline FPS without advancements
2. Complete advancements continuously and monitor FPS
3. Test with multiple players earning credits simultaneously
4. Run for extended period and check for memory leaks
5. Monitor network bandwidth usage
6. Check CPU usage on server with active syncing

**Expected Result**:

- FPS remains stable during syncs (no spikes >5ms)
- Multiplayer syncs don't cause lag for other players
- No memory accumulation over time
- Network bandwidth minimal (packets are small: 8 bytes)
- Server CPU impact negligible

**Edge Cases**:

- 10+ players earning credits simultaneously
- Long (1+ hour) gameplay session
- Server under other load (mob spawning, redstone)
- Resource-constrained environments

---

## Test Case 10: Localization and Messaging

**Description**: Verify that all user-facing messages are properly localized

**Prerequisites**:

- Game set to different language (if available)
- Debug messages enabled

**Steps**:

1. Complete advancement and check notification message
2. Check credit display formatting
3. Verify advancement names display correctly
4. Check all error messages are readable
5. Test with non-English locale if available

**Expected Result**:

- All notification messages display correctly
- No placeholder text visible to player
- Numbers format according to locale
- Advancement names visible and accurate
- Error messages are clear and helpful

**Edge Cases**:

- Very long advancement names
- Advancement names with special characters
- Multiple line messages
- RTL (right-to-left) languages (if supported)

---

## Known Limitations and Assumptions

1. **Network Reliability**: Assumes reasonably reliable network connection. Packet loss may cause temporary desync.
2. **Thread Safety**: Credit sync assumes server thread execution for all modifications.
3. **Packet Order**: Relies on Minecraft's packet delivery order guarantee.
4. **Multiplayer**: Tested with local multiplayer and online servers.

---

## Debug Commands (if implemented)

- `/chunklocked credits` - Display current player credits
- `/chunklocked info <player>` - Admin command to view player's credit data
- `/chunklocked reload` - Reload configuration

---

## Troubleshooting

**Credits not syncing after advancement**:

1. Check that advancement completion is detected (look for log messages)
2. Verify NotificationManager.syncCreditsToClient() is being called
3. Check that ServerPlayNetworking is properly configured
4. Verify client packet handler is registered

**Client shows zero credits after login**:

1. Check player data on server (if available)
2. Verify ChunkUnlockData is loaded correctly
3. Check that initial sync packet is sent on player join
4. Look for exceptions in packet sending

**Crashes during sync**:

1. Check for integer overflow in credit calculations
2. Verify packet encoding doesn't exceed buffer size
3. Look for null pointer exceptions in packet data
4. Check JVM memory isn't exhausted

**Performance degradation**:

1. Monitor packet frequency (should be 1-2 per advancement)
2. Check for packet send loops (unbounded iteration)
3. Verify client packet handler doesn't block main thread
4. Profile CPU usage on both sides

---

## Test Sign-Off

- [ ] All test cases passed
- [ ] No critical errors found
- [ ] Performance is acceptable
- [ ] Multiplayer functionality verified
- [ ] Ready for public release

**Tester**: **\*\*\*\***\_**\*\*\*\***  
**Date**: **\*\*\*\***\_**\*\*\*\***  
**Notes**: **\*\*\*\***\_**\*\*\*\***
