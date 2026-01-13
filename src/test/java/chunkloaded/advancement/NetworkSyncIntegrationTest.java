package chunkloaded.advancement;

import chunkloaded.network.CreditUpdatePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for network synchronization of credit data.
 * <p>
 * Tests the flow from server-side credit awards to client-side data updates
 * via network packets.
 */
@DisplayName("Credit Sync Network Integration")
public class NetworkSyncIntegrationTest {

  /**
   * Test packet encoding for credit update.
   */
  @Test
  @DisplayName("Packet correctly encodes credit values")
  void testPacketEncoding_CreditData_EncodesCorrectly() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

    CreditUpdatePacket.CODEC.encode(buf, packet);

    buf.resetReaderIndex();
    assertEquals(5, buf.readInt());
    assertEquals(10, buf.readInt());
  }

  /**
   * Test packet decoding for credit update.
   */
  @Test
  @DisplayName("Packet correctly decodes credit values")
  void testPacketDecoding_EncodedBuffer_DecodesCorrectly() {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    buf.writeInt(5);
    buf.writeInt(10);
    buf.resetReaderIndex();

    CreditUpdatePacket packet = CreditUpdatePacket.CODEC.decode(buf);

    assertEquals(5, packet.credits());
    assertEquals(10, packet.total());
  }

  /**
   * Test round-trip encoding and decoding preserves data.
   */
  @Test
  @DisplayName("Round-trip encode/decode preserves packet data")
  void testRoundTrip_EncodeAndDecode_DataPreserved() {
    CreditUpdatePacket original = new CreditUpdatePacket(42, 99);
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

    CreditUpdatePacket.CODEC.encode(buf, original);
    buf.resetReaderIndex();
    CreditUpdatePacket decoded = CreditUpdatePacket.CODEC.decode(buf);

    assertEquals(original.credits(), decoded.credits());
    assertEquals(original.total(), decoded.total());
  }

  /**
   * Test create factory validates negative values.
   */
  @Test
  @DisplayName("Packet factory rejects negative credit values")
  void testCreate_NegativeCredits_ThrowsException() {
    assertThrows(IllegalArgumentException.class,
        () -> CreditUpdatePacket.create(-1, 10));
  }

  /**
   * Test create factory validates negative total.
   */
  @Test
  @DisplayName("Packet factory rejects negative total advancement values")
  void testCreate_NegativeTotal_ThrowsException() {
    assertThrows(IllegalArgumentException.class,
        () -> CreditUpdatePacket.create(5, -1));
  }

  /**
   * Test validate method accepts valid values.
   */
  @Test
  @DisplayName("Validate accepts valid packet values")
  void testValidate_ValidValues_NoException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    assertDoesNotThrow(packet::validate);
  }

  /**
   * Test validate method with zero values.
   */
  @Test
  @DisplayName("Validate accepts zero values")
  void testValidate_ZeroValues_NoException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(0, 0);
    assertDoesNotThrow(packet::validate);
  }

  /**
   * Test validate method rejects negative credits.
   */
  @Test
  @DisplayName("Validate rejects negative credit values")
  void testValidate_NegativeCredits_ThrowsException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(-1, 10);
    assertThrows(IllegalArgumentException.class, packet::validate);
  }

  /**
   * Test validate method rejects negative total.
   */
  @Test
  @DisplayName("Validate rejects negative total advancement values")
  void testValidate_NegativeTotal_ThrowsException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, -1);
    assertThrows(IllegalArgumentException.class, packet::validate);
  }

  /**
   * Test multiple packets with different values maintain independence.
   */
  @Test
  @DisplayName("Multiple packets encode/decode independently")
  void testMultiplePackets_DifferentValues_EncodedDecodedCorrectly() {
    int[][] testCases = {
        { 0, 0 },
        { 1, 1 },
        { 10, 20 },
        { 100, 200 },
        { 1000, 5000 }
    };

    for (int[] testCase : testCases) {
      CreditUpdatePacket original = new CreditUpdatePacket(testCase[0], testCase[1]);
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

      CreditUpdatePacket.CODEC.encode(buf, original);
      buf.resetReaderIndex();
      CreditUpdatePacket decoded = CreditUpdatePacket.CODEC.decode(buf);

      assertEquals(original.credits(), decoded.credits(),
          "Credits mismatch for test case [" + testCase[0] + ", " + testCase[1] + "]");
      assertEquals(original.total(), decoded.total(),
          "Total mismatch for test case [" + testCase[0] + ", " + testCase[1] + "]");
    }
  }

  /**
   * Test packet type consistency.
   */
  @Test
  @DisplayName("Packet type is consistent and not null")
  void testPacketType_Always_ReturnsConsistentType() {
    CreditUpdatePacket packet1 = new CreditUpdatePacket(5, 10);
    CreditUpdatePacket packet2 = new CreditUpdatePacket(3, 7);

    assertNotNull(packet1.type());
    assertNotNull(packet2.type());
    assertEquals(packet1.type(), packet2.type());
  }

  /**
   * Test sync method static access.
   */
  @Test
  @DisplayName("Sync method is accessible as static")
  void testSyncMethod_StaticAccess_NotThrows() {
    // This test verifies the method exists and is static
    // Actual network sending requires mocking ServerPlayNetworking
    assertDoesNotThrow(() -> {
      // Verify we can access the method without an instance
      var method = NotificationManager.class.getMethod("syncCreditsToClient",
          ServerPlayer.class, int.class, int.class);
      assertNotNull(method);
    });
  }
}
