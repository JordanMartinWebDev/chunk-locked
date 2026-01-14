package chunklocked.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ClientChunkDataHolder}.
 */
public class ClientChunkDataHolderTest {

  /**
   * Reset client data before each test.
   */
  @BeforeEach
  void setUp() {
    ClientChunkDataHolder.reset();
  }

  /**
   * Reset client data after each test.
   */
  @AfterEach
  void tearDown() {
    ClientChunkDataHolder.reset();
  }

  /**
   * Test initial state of client data holder.
   */
  @Test
  void testInitialState_AllValuesZero() {
    assertEquals(0, ClientChunkDataHolder.getCredits());
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
    assertFalse(ClientChunkDataHolder.isConnected());
  }

  /**
   * Test setting credits updates the value.
   */
  @Test
  void testSetCredits_ValidValue_UpdatesCorrectly() {
    ClientChunkDataHolder.setCredits(5);
    assertEquals(5, ClientChunkDataHolder.getCredits());
  }

  /**
   * Test setting credits to zero.
   */
  @Test
  void testSetCredits_ZeroValue_UpdatesToZero() {
    ClientChunkDataHolder.setCredits(10);
    ClientChunkDataHolder.setCredits(0);
    assertEquals(0, ClientChunkDataHolder.getCredits());
  }

  /**
   * Test setting credits multiple times uses latest value.
   */
  @Test
  void testSetCredits_MultipleUpdates_UsesLatestValue() {
    ClientChunkDataHolder.setCredits(5);
    ClientChunkDataHolder.setCredits(10);
    ClientChunkDataHolder.setCredits(3);
    assertEquals(3, ClientChunkDataHolder.getCredits());
  }

  /**
   * Test setting negative credits is rejected.
   */
  @Test
  void testSetCredits_NegativeValue_Rejected() {
    ClientChunkDataHolder.setCredits(5);
    ClientChunkDataHolder.setCredits(-1);
    // Should remain unchanged
    assertEquals(5, ClientChunkDataHolder.getCredits());
  }

  /**
   * Test setting total advancements updates the value.
   */
  @Test
  void testSetTotalAdvancements_ValidValue_UpdatesCorrectly() {
    ClientChunkDataHolder.setTotalAdvancements(10);
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setting total advancements to zero.
   */
  @Test
  void testSetTotalAdvancements_ZeroValue_UpdatesToZero() {
    ClientChunkDataHolder.setTotalAdvancements(20);
    ClientChunkDataHolder.setTotalAdvancements(0);
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setting total advancements multiple times uses latest value.
   */
  @Test
  void testSetTotalAdvancements_MultipleUpdates_UsesLatestValue() {
    ClientChunkDataHolder.setTotalAdvancements(5);
    ClientChunkDataHolder.setTotalAdvancements(15);
    ClientChunkDataHolder.setTotalAdvancements(7);
    assertEquals(7, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setting negative total advancements is rejected.
   */
  @Test
  void testSetTotalAdvancements_NegativeValue_Rejected() {
    ClientChunkDataHolder.setTotalAdvancements(10);
    ClientChunkDataHolder.setTotalAdvancements(-1);
    // Should remain unchanged
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setting both values independently.
   */
  @Test
  void testSetCreditsAndTotal_Independent_BothUpdate() {
    ClientChunkDataHolder.setCredits(5);
    ClientChunkDataHolder.setTotalAdvancements(10);
    assertEquals(5, ClientChunkDataHolder.getCredits());
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setting connected status to true.
   */
  @Test
  void testSetConnected_ToTrue_UpdatesStatus() {
    ClientChunkDataHolder.setConnected(true);
    assertTrue(ClientChunkDataHolder.isConnected());
  }

  /**
   * Test setting connected status to false resets data.
   */
  @Test
  void testSetConnected_ToFalse_ResetsData() {
    ClientChunkDataHolder.setCredits(5);
    ClientChunkDataHolder.setTotalAdvancements(10);
    ClientChunkDataHolder.setConnected(true);

    ClientChunkDataHolder.setConnected(false);

    assertFalse(ClientChunkDataHolder.isConnected());
    assertEquals(0, ClientChunkDataHolder.getCredits());
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test disconnection clears data properly.
   */
  @Test
  void testDisconnect_AnyConnectedState_ClearsData() {
    ClientChunkDataHolder.setConnected(true);
    ClientChunkDataHolder.setCredits(100);
    ClientChunkDataHolder.setTotalAdvancements(50);

    ClientChunkDataHolder.setConnected(false);

    assertEquals(0, ClientChunkDataHolder.getCredits());
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
    assertFalse(ClientChunkDataHolder.isConnected());
  }

  /**
   * Test reset clears all data.
   */
  @Test
  void testReset_AnyState_ClearsEverything() {
    ClientChunkDataHolder.setConnected(true);
    ClientChunkDataHolder.setCredits(50);
    ClientChunkDataHolder.setTotalAdvancements(25);

    ClientChunkDataHolder.reset();

    assertEquals(0, ClientChunkDataHolder.getCredits());
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
    assertFalse(ClientChunkDataHolder.isConnected());
  }

  /**
   * Test setAll() updates both values atomically.
   */
  @Test
  void testSetAll_ValidValues_UpdatesBoth() {
    ClientChunkDataHolder.setAll(5, 10);
    assertEquals(5, ClientChunkDataHolder.getCredits());
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setAll() with zero values.
   */
  @Test
  void testSetAll_ZeroValues_UpdatesBothToZero() {
    ClientChunkDataHolder.setAll(5, 10);
    ClientChunkDataHolder.setAll(0, 0);
    assertEquals(0, ClientChunkDataHolder.getCredits());
    assertEquals(0, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setAll() rejects negative credits.
   */
  @Test
  void testSetAll_NegativeCredits_Rejected() {
    ClientChunkDataHolder.setAll(5, 10);
    ClientChunkDataHolder.setAll(-1, 15);
    // Should remain unchanged
    assertEquals(5, ClientChunkDataHolder.getCredits());
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setAll() rejects negative total.
   */
  @Test
  void testSetAll_NegativeTotal_Rejected() {
    ClientChunkDataHolder.setAll(5, 10);
    ClientChunkDataHolder.setAll(8, -1);
    // Should remain unchanged
    assertEquals(5, ClientChunkDataHolder.getCredits());
    assertEquals(10, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test setAll() with large values.
   */
  @Test
  void testSetAll_LargeValues_UpdatesCorrectly() {
    int largeCredit = 1_000_000;
    int largeTotal = 2_000_000;
    ClientChunkDataHolder.setAll(largeCredit, largeTotal);
    assertEquals(largeCredit, ClientChunkDataHolder.getCredits());
    assertEquals(largeTotal, ClientChunkDataHolder.getTotalAdvancements());
  }

  /**
   * Test multiple independent updates maintain consistency.
   */
  @Test
  void testMultipleUpdates_Independent_AllCorrect() {
    ClientChunkDataHolder.setCredits(1);
    ClientChunkDataHolder.setTotalAdvancements(2);
    ClientChunkDataHolder.setCredits(3);
    ClientChunkDataHolder.setTotalAdvancements(4);
    ClientChunkDataHolder.setCredits(5);

    assertEquals(5, ClientChunkDataHolder.getCredits());
    assertEquals(4, ClientChunkDataHolder.getTotalAdvancements());
  }
}
