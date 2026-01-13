package chunkloaded.network;

import chunkloaded.network.CreditUpdatePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreditUpdatePacket}.
 */
public class CreditUpdatePacketTest {

  /**
   * Test packet creation with valid values.
   */
  @Test
  void testPacketCreation_ValidValues_Success() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    assertEquals(5, packet.credits());
    assertEquals(10, packet.total());
  }

  /**
   * Test packet creation with zero values.
   */
  @Test
  void testPacketCreation_ZeroValues_Success() {
    CreditUpdatePacket packet = new CreditUpdatePacket(0, 0);
    assertEquals(0, packet.credits());
    assertEquals(0, packet.total());
  }

  /**
   * Test packet encoding to byte buffer.
   */
  @Test
  void testPacketEncoding_ValidPacket_EncodesCorrectly() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

    // Encode using codec
    CreditUpdatePacket.CODEC.encode(buf, packet);

    // Verify buffer contains expected data
    buf.resetReaderIndex();
    assertEquals(5, buf.readInt());
    assertEquals(10, buf.readInt());
  }

  /**
   * Test packet decoding from byte buffer.
   */
  @Test
  void testPacketDecoding_ValidBuffer_DecodesCorrectly() {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    buf.writeInt(5);
    buf.writeInt(10);
    buf.resetReaderIndex();

    // Decode using codec
    CreditUpdatePacket packet = CreditUpdatePacket.CODEC.decode(buf);

    assertEquals(5, packet.credits());
    assertEquals(10, packet.total());
  }

  /**
   * Test round-trip encoding and decoding.
   */
  @Test
  void testRoundTrip_EncodeAndDecode_DataPreserved() {
    CreditUpdatePacket original = new CreditUpdatePacket(42, 99);
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

    // Encode
    CreditUpdatePacket.CODEC.encode(buf, original);

    // Decode
    buf.resetReaderIndex();
    CreditUpdatePacket decoded = CreditUpdatePacket.CODEC.decode(buf);

    assertEquals(original.credits(), decoded.credits());
    assertEquals(original.total(), decoded.total());
  }

  /**
   * Test packet creation with large values.
   */
  @ParameterizedTest
  @CsvSource({
      "1000, 5000",
      "100, 200"
  })
  void testPacketCreation_LargeValues_Success(int credits, int total) {
    CreditUpdatePacket packet = new CreditUpdatePacket(credits, total);
    assertEquals(credits, packet.credits());
    assertEquals(total, packet.total());
  }

  /**
   * Test validate() method with valid values.
   */
  @Test
  void testValidate_ValidValues_NoException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    assertDoesNotThrow(packet::validate);
  }

  /**
   * Test validate() method with zero values.
   */
  @Test
  void testValidate_ZeroValues_NoException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(0, 0);
    assertDoesNotThrow(packet::validate);
  }

  /**
   * Test validate() method with negative credits.
   */
  @Test
  void testValidate_NegativeCredits_ThrowsException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(-1, 10);
    assertThrows(IllegalArgumentException.class, packet::validate);
  }

  /**
   * Test validate() method with negative total.
   */
  @Test
  void testValidate_NegativeTotal_ThrowsException() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, -1);
    assertThrows(IllegalArgumentException.class, packet::validate);
  }

  /**
   * Test create() factory method with valid values.
   */
  @Test
  void testCreate_ValidValues_ReturnsPacket() {
    CreditUpdatePacket packet = CreditUpdatePacket.create(5, 10);
    assertNotNull(packet);
    assertEquals(5, packet.credits());
    assertEquals(10, packet.total());
  }

  /**
   * Test create() factory method with negative credits.
   */
  @Test
  void testCreate_NegativeCredits_ThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> CreditUpdatePacket.create(-1, 10));
  }

  /**
   * Test create() factory method with negative total.
   */
  @Test
  void testCreate_NegativeTotal_ThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> CreditUpdatePacket.create(5, -1));
  }

  /**
   * Test packet type is correctly set.
   */
  @Test
  void testPacketType_Always_ReturnsCorrectType() {
    CreditUpdatePacket packet = new CreditUpdatePacket(5, 10);
    assertEquals(CreditUpdatePacket.TYPE, packet.type());
  }

  /**
   * Test packet codec is not null.
   */
  @Test
  void testCodec_Always_IsNotNull() {
    assertNotNull(CreditUpdatePacket.CODEC);
  }

  /**
   * Test multiple packets with different values encode/decode independently.
   */
  @Test
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

      assertEquals(original.credits(), decoded.credits(), "Credits mismatch for: " + testCase[0]);
      assertEquals(original.total(), decoded.total(), "Total mismatch for: " + testCase[1]);
    }
  }
}
