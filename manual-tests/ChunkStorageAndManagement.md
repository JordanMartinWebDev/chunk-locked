# Chunk Storage and Management - Manual Test Cases

**Purpose**: Verify chunk unlock/lock operations, persistence, and the ChunkManager facade work correctly in a live Minecraft environment.

**Note**: These features require `ChunkPos` and cannot be unit tested due to Minecraft bootstrap requirements.

---

## Test Case 1: Basic Chunk Unlock/Lock Operations

**Prerequisites**:

- Start fresh world or clear test player's data
- Test player has at least 3 unlock credits

**Steps**:

1. Check player's unlocked chunks at spawn
2. Award player 3 credits: `/chunk-locked credits add <player> 3`
3. Unlock a chunk adjacent to spawn: `/chunk-locked unlock <x> <z>`
4. Verify chunk is marked as unlocked: `/chunk-locked info <x> <z>`
5. Lock the chunk: `/chunk-locked lock <x> <z>`
6. Verify chunk is marked as locked: `/chunk-locked info <x> <z>`

**Expected Result**:

- Unlock operation succeeds when credits available
- Lock operation succeeds on unlocked chunks
- Info command shows correct unlock status

**Edge Cases**:

- Try to unlock without credits (should fail)
- Try to lock already locked chunk (no-op)
- Try to unlock already unlocked chunk (no-op)

---

## Test Case 2: Multiple Chunks and Player Isolation

**Prerequisites**:

- Two test players
- Each player has 5 credits

**Steps**:

1. Player 1 unlocks chunks at (0,0), (1,0), (0,1)
2. Player 2 unlocks chunks at (10,10), (11,10)
3. Query Player 1's chunks: `/chunk-locked list <player1>`
4. Query Player 2's chunks: `/chunk-locked list <player2>`
5. Verify each player only sees their own unlocked chunks

**Expected Result**:

- Each player's unlocks are independent
- No cross-contamination between players
- List command shows correct chunks per player

**Edge Cases**:

- Multiple players unlocking same chunk coordinates
- Query non-existent player (should show 0 chunks)

---

## Test Case 3: Save/Load Persistence

**Prerequisites**:

- Fresh world
- Test player with 10 credits

**Steps**:

1. Unlock 5 random chunks in different quadrants (positive/negative coordinates)
2. Record the exact chunk coordinates unlocked
3. Save and quit the world
4. Reload the world
5. Query unlocked chunks: `/chunk-locked list <player>`
6. Verify all 5 chunks are still unlocked

**Expected Result**:

- All unlocked chunks persist across save/load
- Credits remaining persist
- No data corruption or loss

**Edge Cases**:

- Chunks with negative coordinates
- Large coordinate values (e.g., 100000, 100000)
- World save during chunk unlock operation

---

## Test Case 4: ChunkManager API Integration

**Prerequisites**:

- Test player with credits
- Access to debug commands or test mod

**Steps**:

1. Use ChunkManager.getAvailableCredits() - verify correct count
2. Use ChunkManager.tryUnlockChunk() - verify success with credits
3. Use ChunkManager.tryUnlockChunk() again on same chunk - verify failure
4. Use ChunkManager.isChunkUnlocked() - verify returns true
5. Use ChunkManager.getPlayerChunks() - verify returns correct set
6. Use ChunkManager.forceUnlockChunk() - verify unlocks without spending credit

**Expected Result**:

- All API methods work as documented
- Credit spending is atomic
- No race conditions or inconsistencies

**Edge Cases**:

- Rapid successive unlock calls
- Concurrent access from different threads
- Null parameter handling

---

## Test Case 5: Area Detection with Connected Chunks

**Prerequisites**:

- Test player with 10 credits
- Empty chunk unlock state

**Steps**:

1. Unlock a 2x2 grid of chunks: (0,0), (1,0), (0,1), (1,1)
2. Query areas: Check if detected as single connected area
3. Unlock disconnected chunk at (10,10)
4. Query areas again: Should show 2 separate areas
5. Bridge the gap by unlocking chunks between them
6. Query areas: Should now show single large area

**Expected Result**:

- BFS algorithm correctly identifies connected regions
- Multiple disconnected areas are tracked separately
- Area merging works when bridge is created

**Edge Cases**:

- L-shaped connected regions
- Single isolated chunks
- Very large contiguous areas (50+ chunks)
- Negative coordinate chunks

---

## Test Case 6: Negative Coordinates and Edge Cases

**Prerequisites**:

- Test player with 20 credits

**Steps**:

1. Unlock chunks in all quadrants:
   - Positive X, Positive Z: (5, 5)
   - Negative X, Positive Z: (-5, 5)
   - Positive X, Negative Z: (5, -5)
   - Negative X, Negative Z: (-5, -5)
2. Unlock chunk at origin: (0, 0)
3. Unlock chunks at extreme coordinates: (1000000, 1000000)
4. Query all unlocked chunks
5. Save and reload world
6. Verify all chunks still unlocked

**Expected Result**:

- All coordinate quadrants work correctly
- Origin (0,0) is handled properly
- Large coordinates don't cause overflow
- Persistence works with all coordinate types

**Edge Cases**:

- Integer.MAX_VALUE coordinates
- Integer.MIN_VALUE coordinates
- Rapid unlock/lock cycles

---

## Test Case 7: Credit Management Edge Cases

**Prerequisites**:

- Test player

**Steps**:

1. Set credits to 0: `/chunk-locked credits set <player> 0`
2. Try to unlock chunk (should fail)
3. Set credits to 1000: `/chunk-locked credits set <player> 1000`
4. Verify can unlock many chunks
5. Spend all credits by unlocking
6. Verify credit count is 0
7. Add negative credits (should fail/error)

**Expected Result**:

- Cannot unlock with 0 credits
- Credit operations are bounds-checked
- Negative credit values rejected
- Integer overflow handled gracefully

**Edge Cases**:

- Setting credits to Integer.MAX_VALUE
- Spending more credits than available
- Concurrent credit modifications

---

## Test Case 8: ChunkUnlockData NBT Serialization

**Prerequisites**:

- Fresh world
- Test player with credits

**Steps**:

1. Unlock 3 chunks with different coordinates
2. Check world save folder for chunk-locked data file
3. Open NBT file with NBT editor
4. Verify structure:
   - DataVersion = 2
   - Players section exists
   - Player UUID present
   - UnlockedChunks array contains 3 entries
   - Credits value is correct
5. Manually edit NBT to add a chunk
6. Reload world
7. Verify manual edit persisted

**Expected Result**:

- NBT structure matches v2 format
- All data serializes correctly
- Manual NBT edits work (data migration compatible)
- No data corruption

**Edge Cases**:

- Empty UnlockedChunks array
- Missing player data
- Corrupt NBT (should fallback gracefully)

---

## Test Case 9: Multi-Player Server Environment

**Prerequisites**:

- Multiplayer server
- 3 connected players

**Steps**:

1. Each player unlocks different chunks
2. Players check their own unlocked chunks
3. One player leaves and rejoins
4. Verify their chunks still unlocked
5. Admin unlocks chunk for offline player
6. Offline player rejoins
7. Verify admin unlock persisted

**Expected Result**:

- Each player's data is independent
- Server handles concurrent operations
- Data persists for offline players
- Admin operations work for any player

**Edge Cases**:

- Player leaves during unlock operation
- Server crashes mid-save
- Multiple admins modifying same player

---

## Test Case 10: Large Dataset Performance

**Prerequisites**:

- Test player
- Sufficient credits (10000+)

**Steps**:

1. Unlock 1000 chunks in a grid pattern
2. Measure time to save world (should be <100ms)
3. Reload world
4. Measure time to load chunk data (should be <100ms)
5. Query if specific chunk is unlocked (should be instant)
6. List all unlocked chunks (should be <50ms)

**Expected Result**:

- O(1) lookup performance maintained
- Save/load times remain reasonable
- No memory leaks
- UI remains responsive

**Performance Targets**:

- Chunk lookup: <1ms
- Save operation: <100ms for 1000 chunks
- Load operation: <100ms for 1000 chunks
- Memory usage: <10MB for 10,000 chunks

---

## Testing Checklist

- [ ] Test Case 1: Basic Operations
- [ ] Test Case 2: Player Isolation
- [ ] Test Case 3: Persistence
- [ ] Test Case 4: API Integration
- [ ] Test Case 5: Area Detection
- [ ] Test Case 6: Negative Coordinates
- [ ] Test Case 7: Credit Edge Cases
- [ ] Test Case 8: NBT Serialization
- [ ] Test Case 9: Multiplayer
- [ ] Test Case 10: Performance

---

## Known Limitations

1. ChunkPos requires Minecraft runtime - cannot unit test
2. Area detection requires actual BFS traversal in live environment
3. NBT serialization needs file system access
4. Multiplayer scenarios need actual server environment

## Future: Integration Test Framework

Consider implementing Fabric Game Tests for automated testing:

- https://fabricmc.net/wiki/tutorial:testing
- Requires Minecraft test harness setup
- Can run in-game automated test scenarios
- Provides reproducible test environment
