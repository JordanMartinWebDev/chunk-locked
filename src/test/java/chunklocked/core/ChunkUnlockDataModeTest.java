package chunklocked.core;

import chunklocked.advancement.AdvancementCreditManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunklockedMode functionality in {@link ChunkUnlockData}.
 * <p>
 * Tests mode persistence, immutability, and NBT serialization/deserialization.
 */
class ChunkUnlockDataModeTest {

  @TempDir
  Path tempDir;

  private AdvancementCreditManager creditManager;
  private ChunkUnlockData data;

  @BeforeEach
  void setUp() {
    creditManager = new AdvancementCreditManager();
    data = new ChunkUnlockData(creditManager, tempDir);
  }

  @AfterEach
  void tearDown() {
    // Clean up any created files
    try {
      Files.deleteIfExists(tempDir.resolve("chunklocked_data.nbt"));
    } catch (IOException e) {
      // Ignore cleanup errors
    }
  }

  // ========== MODE GETTER/SETTER TESTS ==========

  @Test
  void getMode_DefaultValue_ReturnsDisabled() {
    assertEquals(ChunklockedMode.DISABLED, data.getMode(),
        "Default mode should be DISABLED");
  }

  @Test
  void setMode_ValidMode_SetsSuccessfully() {
    data.setMode(ChunklockedMode.EASY);
    assertEquals(ChunklockedMode.EASY, data.getMode(),
        "Mode should be set to EASY");
  }

  @Test
  void setMode_NullMode_ThrowsException() {
    assertThrows(IllegalArgumentException.class,
        () -> data.setMode(null),
        "Setting null mode should throw IllegalArgumentException");
  }

  @Test
  void setMode_SecondCall_ThrowsException() {
    data.setMode(ChunklockedMode.EASY);

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> data.setMode(ChunklockedMode.EXTREME),
        "Second call to setMode should throw IllegalStateException");

    assertTrue(exception.getMessage().contains("Cannot change ChunklockedMode"),
        "Exception message should explain immutability");
  }

  @Test
  void setMode_SameModeTwice_ThrowsException() {
    data.setMode(ChunklockedMode.EASY);

    assertThrows(IllegalStateException.class,
        () -> data.setMode(ChunklockedMode.EASY),
        "Setting same mode twice should still throw exception");
  }

  @Test
  void isModeImmutable_BeforeSet_ReturnsFalse() {
    assertFalse(data.isModeImmutable(),
        "Mode should not be immutable before setting");
  }

  @Test
  void isModeImmutable_AfterSet_ReturnsTrue() {
    data.setMode(ChunklockedMode.EXTREME);
    assertTrue(data.isModeImmutable(),
        "Mode should be immutable after setting");
  }

  // ========== NBT SERIALIZATION TESTS ==========

  @Test
  void nbtSerialization_EasyMode_PersistsCorrectly() throws IOException {
    data.setMode(ChunklockedMode.EASY);
    data.save();

    // Load into new instance
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertEquals(ChunklockedMode.EASY, loadedData.getMode(),
        "EASY mode should persist through save/load");
    assertTrue(loadedData.isModeImmutable(),
        "Loaded mode should be immutable");
  }

  @Test
  void nbtSerialization_ExtremeMode_PersistsCorrectly() throws IOException {
    data.setMode(ChunklockedMode.EXTREME);
    data.save();

    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertEquals(ChunklockedMode.EXTREME, loadedData.getMode(),
        "EXTREME mode should persist through save/load");
  }

  @Test
  void nbtSerialization_DisabledMode_PersistsCorrectly() throws IOException {
    data.setMode(ChunklockedMode.DISABLED);
    data.save();

    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertEquals(ChunklockedMode.DISABLED, loadedData.getMode(),
        "DISABLED mode should persist through save/load");
  }

  @Test
  void nbtDeserialization_MissingMode_DefaultsToDisabled() throws IOException {
    // Create old save data without Mode field (pre-v4)
    CompoundTag nbt = new CompoundTag();
    nbt.putInt("DataVersion", 3); // Old version without mode

    Path saveFile = tempDir.resolve("chunklocked_data.nbt");
    Files.createDirectories(tempDir);
    NbtIo.writeCompressed(nbt, saveFile);

    // Load the data
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertEquals(ChunklockedMode.DISABLED, loadedData.getMode(),
        "Missing mode field should default to DISABLED");
    assertFalse(loadedData.isModeImmutable(),
        "Mode should not be marked immutable for migrated saves");
  }

  @Test
  void nbtDeserialization_InvalidMode_DefaultsToDisabled() throws IOException {
    // Create save data with invalid mode string
    CompoundTag nbt = new CompoundTag();
    nbt.putInt("DataVersion", 4);
    nbt.putString("Mode", "INVALID_MODE");

    Path saveFile = tempDir.resolve("chunklocked_data.nbt");
    Files.createDirectories(tempDir);
    NbtIo.writeCompressed(nbt, saveFile);

    // Load the data
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertEquals(ChunklockedMode.DISABLED, loadedData.getMode(),
        "Invalid mode string should default to DISABLED");
  }

  @Test
  void nbtSerialization_DataVersion_UpdatedToV4() {
    data.setMode(ChunklockedMode.EASY);
    data.save();

    // Verify data version is 5 (updated for starter items)
    try {
      Path saveFile = tempDir.resolve("chunklocked_data.nbt");
      CompoundTag nbt = NbtIo.readCompressed(saveFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());

      assertEquals(5, nbt.getInt("DataVersion").orElse(0),
          "Data version should be 5 for starter items support");
      assertEquals("EASY", nbt.getString("Mode").orElse(""),
          "Mode should be serialized as string");
    } catch (IOException e) {
      fail("Failed to read saved NBT: " + e.getMessage());
    }
  }

  // ========== IMMUTABILITY AFTER LOAD TESTS ==========

  @Test
  void immutability_AfterLoad_PreventsChanges() throws IOException {
    data.setMode(ChunklockedMode.EASY);
    data.save();

    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertThrows(IllegalStateException.class,
        () -> loadedData.setMode(ChunklockedMode.EXTREME),
        "Cannot change mode after loading from save");
  }

  @Test
  void immutability_AfterMigration_AllowsFirstSet() throws IOException {
    // Create old save without mode
    CompoundTag nbt = new CompoundTag();
    nbt.putInt("DataVersion", 3);

    Path saveFile = tempDir.resolve("chunklocked_data.nbt");
    Files.createDirectories(tempDir);
    NbtIo.writeCompressed(nbt, saveFile);

    // Load and verify we CAN set mode (migration path)
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertDoesNotThrow(() -> loadedData.setMode(ChunklockedMode.EASY),
        "Should allow setting mode on migrated saves");
    assertEquals(ChunklockedMode.EASY, loadedData.getMode(),
        "Migrated save should accept new mode");
  }

  // ========== DIRTY FLAG TESTS ==========

  @Test
  void setMode_MarksDirty() {
    assertFalse(data.isDirty(), "Should not be dirty initially");

    data.setMode(ChunklockedMode.EASY);

    assertTrue(data.isDirty(), "Setting mode should mark data as dirty");
  }

  // ========== STARTER ITEMS TRACKING TESTS ==========

  @Test
  void hasReceivedStarterItems_DefaultValue_ReturnsFalse() {
    UUID playerId = UUID.randomUUID();
    assertFalse(data.hasReceivedStarterItems(playerId),
        "Player should not have received starter items initially");
  }

  @Test
  void markStarterItemsGiven_MarksPlayer() {
    UUID playerId = UUID.randomUUID();

    data.markStarterItemsGiven(playerId);

    assertTrue(data.hasReceivedStarterItems(playerId),
        "Player should be marked as having received starter items");
  }

  @Test
  void markStarterItemsGiven_MarksDirty() {
    UUID playerId = UUID.randomUUID();
    assertFalse(data.isDirty(), "Should not be dirty initially");

    data.markStarterItemsGiven(playerId);

    assertTrue(data.isDirty(), "Marking starter items should set dirty flag");
  }

  @Test
  void markStarterItemsGiven_SamePlayerTwice_NoExtraMarking() {
    UUID playerId = UUID.randomUUID();

    data.markStarterItemsGiven(playerId);
    data.save(); // Clear dirty flag
    assertFalse(data.isDirty());

    data.markStarterItemsGiven(playerId);

    assertFalse(data.isDirty(), "Marking same player twice should not set dirty flag again");
  }

  @Test
  void starterItemsPersistence_SaveAndLoad() throws IOException {
    UUID player1 = UUID.randomUUID();
    UUID player2 = UUID.randomUUID();

    data.markStarterItemsGiven(player1);
    data.markStarterItemsGiven(player2);
    data.save();

    // Load into new instance
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    assertTrue(loadedData.hasReceivedStarterItems(player1),
        "Player 1 should have starter items after reload");
    assertTrue(loadedData.hasReceivedStarterItems(player2),
        "Player 2 should have starter items after reload");
  }

  @Test
  void starterItemsPersistence_MigrationFromOldSave() throws IOException {
    // Create old save without starter items tracking (pre-v5)
    CompoundTag nbt = new CompoundTag();
    nbt.putInt("DataVersion", 4);
    nbt.putString("Mode", "EASY");

    Path saveFile = tempDir.resolve("chunklocked_data.nbt");
    Files.createDirectories(tempDir);
    NbtIo.writeCompressed(nbt, saveFile);

    // Load the data
    ChunkUnlockData loadedData = new ChunkUnlockData(creditManager, tempDir);
    loadedData.load();

    UUID playerId = UUID.randomUUID();
    assertFalse(loadedData.hasReceivedStarterItems(playerId),
        "Players should not have starter items in migrated save");
    assertEquals(0, loadedData.getStarterItemsGivenCount(),
        "Starter items count should be 0 for migrated save");
  }

  @Test
  void getStarterItemsGivenCount_ReturnsCorrectCount() {
    assertEquals(0, data.getStarterItemsGivenCount(),
        "Initial count should be 0");

    data.markStarterItemsGiven(UUID.randomUUID());
    assertEquals(1, data.getStarterItemsGivenCount(),
        "Count should be 1 after marking one player");

    data.markStarterItemsGiven(UUID.randomUUID());
    assertEquals(2, data.getStarterItemsGivenCount(),
        "Count should be 2 after marking two players");
  }

  @Test
  void nbtSerialization_DataVersion_UpdatedToV5() {
    data.setMode(ChunklockedMode.EASY);
    data.markStarterItemsGiven(UUID.randomUUID());
    data.save();

    // Verify data version is 5
    try {
      Path saveFile = tempDir.resolve("chunklocked_data.nbt");
      CompoundTag nbt = NbtIo.readCompressed(saveFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());

      assertEquals(5, nbt.getInt("DataVersion").orElse(0),
          "Data version should be 5 for starter items support");
      assertTrue(nbt.contains("StarterItemsGiven"),
          "NBT should contain StarterItemsGiven field");
    } catch (IOException e) {
      fail("Failed to read saved NBT: " + e.getMessage());
    }
  }
}
