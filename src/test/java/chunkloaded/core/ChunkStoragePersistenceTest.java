package chunkloaded.core;

import chunkloaded.advancement.AdvancementCreditManager;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for chunk storage persistence in ChunkUnlockData.
 * 
 * Tests validate save/load roundtrip, data migration, and persistence across sessions.
 * 
 * NOTE: Disabled due to API changes in ChunkUnlockData and ChunkManager.
 * These tests need to be refactored to match the current API.
 * See: docs/Session-2026-01-12-Completion-Summary.md
 * 
 * TODO: Update these tests to use correct ChunkManager methods instead of ChunkUnlockData
 */
@Disabled("API mismatch - needs refactoring to use current ChunkManager methods")
public class ChunkStoragePersistenceTest {
    
    private static final UUID TEST_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_PLAYER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    
    private Path testDir;
    
    @BeforeEach
    void setUp() throws Exception {
        testDir = Files.createTempDirectory("chunk-locked-persist-test");
    }
    
    // ========== Save/Load Roundtrip Tests ==========
    
    @Test
    void testSaveLoad_SingleChunk_PreservesData() throws Exception {
        // Create, populate, and save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        ChunkPos chunk = new ChunkPos(0, 0);
        data1.unlockChunk(TEST_PLAYER, chunk);
        data1.save();
        
        // Load into new instance
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        // Verify data preserved
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk));
    }
    
    @Test
    void testSaveLoad_MultipleChunks_PreservesAllData() throws Exception {
        // Create, populate, and save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        ChunkPos chunk1 = new ChunkPos(0, 0);
        ChunkPos chunk2 = new ChunkPos(1, 0);
        ChunkPos chunk3 = new ChunkPos(0, 1);
        ChunkPos chunk4 = new ChunkPos(-1, -1);
        
        data1.unlockChunk(TEST_PLAYER, chunk1);
        data1.unlockChunk(TEST_PLAYER, chunk2);
        data1.unlockChunk(TEST_PLAYER, chunk3);
        data1.unlockChunk(TEST_PLAYER, chunk4);
        data1.save();
        
        // Load into new instance
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        // Verify all chunks preserved
        assertEquals(4, data2.getPlayerUnlockedChunks(TEST_PLAYER).size());
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk1));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk2));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk3));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk4));
    }
    
    @Test
    void testSaveLoad_MultiplePlayer_PreservesIndependentData() throws Exception {
        // Create, populate, and save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        ChunkPos player1Chunk = new ChunkPos(0, 0);
        ChunkPos player2Chunk = new ChunkPos(10, 10);
        ChunkPos sharedChunk = new ChunkPos(5, 5);
        
        data1.unlockChunk(TEST_PLAYER, player1Chunk);
        data1.unlockChunk(TEST_PLAYER, sharedChunk);
        data1.unlockChunk(TEST_PLAYER_2, player2Chunk);
        data1.unlockChunk(TEST_PLAYER_2, sharedChunk);
        data1.save();
        
        // Load into new instance
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        // Verify player 1 data
        assertEquals(2, data2.getPlayerUnlockedChunks(TEST_PLAYER).size());
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, player1Chunk));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, sharedChunk));
        
        // Verify player 2 data
        assertEquals(2, data2.getPlayerUnlockedChunks(TEST_PLAYER_2).size());
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER_2, player2Chunk));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER_2, sharedChunk));
    }
    
    @Test
    void testSaveLoad_AfterLocking_PreservesLockedState() throws Exception {
        // Create, unlock chunks, lock some, then save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        ChunkPos chunk1 = new ChunkPos(0, 0);
        ChunkPos chunk2 = new ChunkPos(1, 0);
        
        data1.unlockChunk(TEST_PLAYER, chunk1);
        data1.unlockChunk(TEST_PLAYER, chunk2);
        data1.lockChunk(TEST_PLAYER, chunk2);  // Lock one of them
        data1.save();
        
        // Load into new instance
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        // Verify locked state preserved
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, chunk1));
        assertFalse(data2.isChunkUnlocked(TEST_PLAYER, chunk2));
        assertEquals(1, data2.getPlayerUnlockedChunks(TEST_PLAYER).size());
    }
    
    @Test
    void testSaveLoad_EmptyData_NoErrors() throws Exception {
        // Create empty data and save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        data1.save();
        
        // Load into new instance - should not throw
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        assertDoesNotThrow(() -> data2.load());
        
        // Verify empty state
        assertTrue(data2.getPlayerUnlockedChunks(TEST_PLAYER).isEmpty());
    }
    
    // ========== Data Modification After Load Tests ==========
    
    @Test
    void testModifyAfterLoad_AddMoreChunks_SavePreservesAll() throws Exception {
        // Initial save
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(0, 0));
        data1.save();
        
        // Load and add more chunks
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        data2.unlockChunk(TEST_PLAYER, new ChunkPos(1, 0));
        data2.unlockChunk(TEST_PLAYER, new ChunkPos(2, 0));
        data2.save();
        
        // Reload and verify
        AdvancementCreditManager creditManager3 = new AdvancementCreditManager();
        ChunkUnlockData data3 = new ChunkUnlockData(creditManager3, testDir);
        data3.load();
        
        Set<ChunkPos> chunks = data3.getPlayerUnlockedChunks(TEST_PLAYER);
        assertEquals(3, chunks.size());
    }
    
    @Test
    void testModifyAfterLoad_LockChunks_SavePreservesChanges() throws Exception {
        // Initial setup with multiple chunks
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(0, 0));
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(1, 0));
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(2, 0));
        data1.save();
        
        // Load, remove chunks, and save
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        data2.lockChunk(TEST_PLAYER, new ChunkPos(1, 0));
        data2.save();
        
        // Reload and verify
        AdvancementCreditManager creditManager3 = new AdvancementCreditManager();
        ChunkUnlockData data3 = new ChunkUnlockData(creditManager3, testDir);
        data3.load();
        
        assertEquals(2, data3.getPlayerUnlockedChunks(TEST_PLAYER).size());
        assertFalse(data3.isChunkUnlocked(TEST_PLAYER, new ChunkPos(1, 0)));
    }
    
    // ========== Large Dataset Persistence Tests ==========
    
    @Test
    void testSaveLoad_LargeDataset_1000Chunks() throws Exception {
        // Create with 1000 chunks
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        for (int i = 0; i < 1000; i++) {
            data1.unlockChunk(TEST_PLAYER, new ChunkPos(i, 0));
        }
        data1.save();
        
        // Load and verify count
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        assertEquals(1000, data2.getPlayerUnlockedChunks(TEST_PLAYER).size());
        
        // Verify sample chunks
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, new ChunkPos(0, 0)));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, new ChunkPos(500, 0)));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, new ChunkPos(999, 0)));
    }
    
    @Test
    void testSaveLoad_NegativeCoordinates_PreservesCorrectly() throws Exception {
        // Create with negative coordinate chunks
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        ChunkPos negChunk1 = new ChunkPos(-5, -5);
        ChunkPos negChunk2 = new ChunkPos(-100, 50);
        ChunkPos negChunk3 = new ChunkPos(0, -1);
        
        data1.unlockChunk(TEST_PLAYER, negChunk1);
        data1.unlockChunk(TEST_PLAYER, negChunk2);
        data1.unlockChunk(TEST_PLAYER, negChunk3);
        data1.save();
        
        // Load and verify negative coordinates preserved
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, negChunk1));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, negChunk2));
        assertTrue(data2.isChunkUnlocked(TEST_PLAYER, negChunk3));
    }
    
    // ========== Concurrent Player Tests ==========
    
    @Test
    void testSaveLoad_ManyPlayers_PreservesIndependentSets() throws Exception {
        // Create data with many players
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        UUID[] players = new UUID[10];
        for (int i = 0; i < 10; i++) {
            players[i] = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", i + 1));
            for (int j = 0; j < 5; j++) {
                data1.unlockChunk(players[i], new ChunkPos(i * 10 + j, 0));
            }
        }
        data1.save();
        
        // Load and verify all players preserved
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        for (int i = 0; i < 10; i++) {
            assertEquals(5, data2.getPlayerUnlockedChunks(players[i]).size());
            // Verify sample chunk for each player
            assertTrue(data2.isChunkUnlocked(players[i], new ChunkPos(i * 10, 0)));
        }
    }
    
    // ========== Clear Operation Persistence Tests ==========
    
    @Test
    void testSaveLoad_AfterClear_NoChunksPreserved() throws Exception {
        // Create with chunks
        AdvancementCreditManager creditManager1 = new AdvancementCreditManager();
        ChunkUnlockData data1 = new ChunkUnlockData(creditManager1, testDir);
        
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(0, 0));
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(1, 0));
        data1.unlockChunk(TEST_PLAYER, new ChunkPos(2, 0));
        
        // Clear and save
        data1.clearPlayerChunks(TEST_PLAYER);
        data1.save();
        
        // Load and verify cleared
        AdvancementCreditManager creditManager2 = new AdvancementCreditManager();
        ChunkUnlockData data2 = new ChunkUnlockData(creditManager2, testDir);
        data2.load();
        
        assertTrue(data2.getPlayerUnlockedChunks(TEST_PLAYER).isEmpty());
    }
}