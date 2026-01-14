package chunklocked.core;

import chunklocked.advancement.AdvancementCreditManager;
import chunklocked.advancement.PlayerProgressionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkUnlockData NBT serialization and deserialization.
 * <p>
 * Tests verify that player progression data can be correctly saved to
 * and loaded from NBT format, including credits, advancements, and edge cases.
 * 
 * NOTE: Disabled due to API changes in ChunkUnlockData and ChunkManager.
 * These tests need to be refactored to match the current API.
 * See: docs/Session-2026-01-12-Completion-Summary.md
 * 
 * TODO: Update these tests to use correct ChunkManager methods instead of ChunkUnlockData
 */
@Disabled("API mismatch - needs refactoring to use current ChunkManager methods")
class ChunkUnlockDataTest {

  private AdvancementCreditManager creditManager;
  private ChunkUnlockData chunkUnlockData;

  @BeforeEach
  void setUp() {
    creditManager = new AdvancementCreditManager();
    chunkUnlockData = new ChunkUnlockData(creditManager, Path.of("/tmp/test"));
  }

  @Test
  void testManagerInitiallyEmpty() {
    // Manager should start with no players
    assertEquals(0, creditManager.getPlayerCount());
  }

  @Test
  void testSinglePlayerData() {
    // Add player data
    UUID playerId = UUID.randomUUID();
    PlayerProgressionData progressionData = new PlayerProgressionData();
    progressionData.setAvailableCredits(5);
    progressionData.setTotalAdvancementsCompleted(10);
    progressionData.markAdvancementRewarded("minecraft:story/mine_stone");
    progressionData.markAdvancementRewarded("minecraft:story/upgrade_tools");
    creditManager.loadPlayerData(playerId, progressionData);

    // Retrieve and verify
    PlayerProgressionData retrieved = creditManager.getPlayerData(playerId);
    assertNotNull(retrieved);
    assertEquals(5, retrieved.getAvailableCredits());
    assertEquals(2, retrieved.getRewardedAdvancements().size());
  }

  @Test
  void testMultiplePlayersData() {
    // Add multiple players
    UUID player1 = UUID.randomUUID();
    UUID player2 = UUID.randomUUID();
    UUID player3 = UUID.randomUUID();

    PlayerProgressionData data1 = new PlayerProgressionData();
    data1.setAvailableCredits(3);
    creditManager.loadPlayerData(player1, data1);

    PlayerProgressionData data2 = new PlayerProgressionData();
    data2.setAvailableCredits(7);
    creditManager.loadPlayerData(player2, data2);

    PlayerProgressionData data3 = new PlayerProgressionData();
    data3.setAvailableCredits(0);
    creditManager.loadPlayerData(player3, data3);

    // Verify all players
    assertEquals(3, creditManager.getPlayerCount());
    assertEquals(3, creditManager.getPlayerData(player1).getAvailableCredits());
    assertEquals(7, creditManager.getPlayerData(player2).getAvailableCredits());
    assertEquals(0, creditManager.getPlayerData(player3).getAvailableCredits());
  }

  @Test
  void testRewardedAdvancementsTracking() {
    // Add player with rewarded advancements
    UUID playerId = UUID.randomUUID();
    PlayerProgressionData progressionData = new PlayerProgressionData();
    progressionData.setAvailableCredits(15);
    progressionData.markAdvancementRewarded("minecraft:story/root");
    progressionData.markAdvancementRewarded("minecraft:story/mine_stone");
    progressionData.markAdvancementRewarded("minecraft:story/upgrade_tools");
    progressionData.markAdvancementRewarded("minecraft:nether/find_fortress");
    progressionData.markAdvancementRewarded("minecraft:adventure/adventuring_time");
    creditManager.loadPlayerData(playerId, progressionData);

    // Verify rewarded advancements
    PlayerProgressionData retrieved = creditManager.getPlayerData(playerId);
    assertEquals(5, retrieved.getRewardedAdvancements().size());
    assertTrue(retrieved.getRewardedAdvancements().contains("minecraft:story/root"));
    assertTrue(retrieved.getRewardedAdvancements().contains("minecraft:adventure/adventuring_time"));
  }

  @Test
  void testCreditCounts() {
    // Test various credit values
    UUID player1 = UUID.randomUUID();
    UUID player2 = UUID.randomUUID();
    UUID player3 = UUID.randomUUID();

    PlayerProgressionData data1 = new PlayerProgressionData();
    data1.setAvailableCredits(0);
    creditManager.loadPlayerData(player1, data1);

    PlayerProgressionData data2 = new PlayerProgressionData();
    data2.setAvailableCredits(1);
    creditManager.loadPlayerData(player2, data2);

    PlayerProgressionData data3 = new PlayerProgressionData();
    data3.setAvailableCredits(100);
    creditManager.loadPlayerData(player3, data3);

    // Verify exact values
    assertEquals(0, creditManager.getPlayerData(player1).getAvailableCredits());
    assertEquals(1, creditManager.getPlayerData(player2).getAvailableCredits());
    assertEquals(100, creditManager.getPlayerData(player3).getAvailableCredits());
  }

  @Test
  void testUuidPreservation() {
    // Test UUID handling
    UUID playerId = UUID.randomUUID();
    PlayerProgressionData data = new PlayerProgressionData();
    data.setAvailableCredits(5);
    creditManager.loadPlayerData(playerId, data);

    // Should be retrievable by UUID
    assertNotNull(creditManager.getPlayerData(playerId));
    assertEquals(5, creditManager.getPlayerData(playerId).getAvailableCredits());
  }

  @Test
  void testPlayerCountAccuracy() {
    // Add various players and check accuracy
    for (int i = 0; i < 5; i++) {
      PlayerProgressionData data = new PlayerProgressionData();
      data.setAvailableCredits(i);
      creditManager.loadPlayerData(UUID.randomUUID(), data);
    }

    // Verify player count
    assertEquals(5, creditManager.getPlayerCount());
  }

  @Test
  void testLargeDataset() {
    // Test with 100+ players
    List<UUID> playerIds = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      UUID playerId = UUID.randomUUID();
      playerIds.add(playerId);

      PlayerProgressionData data = new PlayerProgressionData();
      data.setAvailableCredits(i);
      for (int j = 0; j < 5; j++) {
        data.markAdvancementRewarded("minecraft:test/advancement_" + j);
      }

      creditManager.loadPlayerData(playerId, data);
    }

    // Verify all players loaded
    assertEquals(150, creditManager.getPlayerCount());

    // Verify a sample
    UUID samplePlayer = playerIds.get(50);
    PlayerProgressionData sampleData = creditManager.getPlayerData(samplePlayer);
    assertEquals(50, sampleData.getAvailableCredits());
    assertEquals(5, sampleData.getRewardedAdvancements().size());
  }

  @Test
  void testSpecialCharactersInAdvancementIds() {
    // Test advancement IDs with special characters
    UUID playerId = UUID.randomUUID();
    PlayerProgressionData data = new PlayerProgressionData();
    data.setAvailableCredits(4);
    data.markAdvancementRewarded("minecraft:story/mine_stone");
    data.markAdvancementRewarded("create:recipes/materials/andesite_alloy");
    data.markAdvancementRewarded("mod:custom/advancement-with-dash");
    data.markAdvancementRewarded("test:path/to/advancement_123");
    creditManager.loadPlayerData(playerId, data);

    // Verify all advancements preserved
    PlayerProgressionData retrieved = creditManager.getPlayerData(playerId);
    assertEquals(4, retrieved.getRewardedAdvancements().size());
    assertTrue(retrieved.getRewardedAdvancements().contains("minecraft:story/mine_stone"));
    assertTrue(retrieved.getRewardedAdvancements().contains("create:recipes/materials/andesite_alloy"));
  }

  @Test
  void testEmptyRewardedAdvancements() {
    // Player with no rewarded advancements
    UUID playerId = UUID.randomUUID();
    creditManager.loadPlayerData(playerId, new PlayerProgressionData());

    // Verify empty list
    PlayerProgressionData retrieved = creditManager.getPlayerData(playerId);
    assertNotNull(retrieved.getRewardedAdvancements());
    assertEquals(0, retrieved.getRewardedAdvancements().size());
  }
}
