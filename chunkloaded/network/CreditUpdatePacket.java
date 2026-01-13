package chunkloaded.network;

import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9139;

/**
 * Network packet for synchronizing player credit data from server to client.
 * <p>
 * This packet is sent by the server to update the client's view of available
 * credits and total advancements. Used when:
 * - A player earns credits (immediate sync)
 * - A player joins the server (initial sync)
 * - A player changes dimensions (resync)
 * <p>
 * Thread Safety: This packet is designed to be decoded on the render thread
 * (client side) and should update client data holders that are read by
 * rendering
 * and UI code. Server-side handling happens on the network thread.
 */
public record CreditUpdatePacket(int credits, int total) implements class_8710 {

  /**
   * Packet ID for the credit update payload.
   * Namespace: chunklocked, Name: credit_update
   */
  public static final class_8710.class_9154<CreditUpdatePacket> ID = new class_8710.class_9154<>(
      class_2960.method_60655("chunklocked", "credit_update"));

  /**
   * Packet codec for encoding/decoding credit update packets.
   * <p>
   * Format:
   * - credits (int, 4 bytes): Available credits to unlock chunks
   * - total (int, 4 bytes): Total advancements completed by player
   */
  public static final class_9139<class_2540, CreditUpdatePacket> CODEC = class_9139.method_56438(CreditUpdatePacket::encode,
      CreditUpdatePacket::decode);

  /**
   * Encodes this packet to a byte buffer.
   *
   * @param buf The buffer to write to
   */
  private void encode(class_2540 buf) {
    buf.method_53002(this.credits);
    buf.method_53002(this.total);
  }

  /**
   * Decodes a packet from a byte buffer.
   *
   * @param buf The buffer to read from
   * @return The decoded packet
   */
  private static CreditUpdatePacket decode(class_2540 buf) {
    int credits = buf.readInt();
    int total = buf.readInt();
    return new CreditUpdatePacket(credits, total);
  }

  /**
   * Gets the packet ID for this payload.
   *
   * @return The CustomPayload.Id for this packet type
   */
  @Override
  public class_8710.class_9154<? extends class_8710> method_56479() {
    return ID;
  }

  /**
   * Validates packet content.
   * <p>
   * Ensures credits and total are non-negative values that fit within
   * reasonable bounds (assuming 32-bit signed integers).
   *
   * @throws IllegalArgumentException if credits or total are negative
   */
  public void validate() {
    if (credits < 0) {
      throw new IllegalArgumentException(
          "Credits cannot be negative: " + credits);
    }
    if (total < 0) {
      throw new IllegalArgumentException(
          "Total advancements cannot be negative: " + total);
    }
  }

  /**
   * Creates a new credit update packet with validated values.
   *
   * @param credits The available credits (must be >= 0)
   * @param total   The total advancements (must be >= 0)
   * @return A new CreditUpdatePacket
   * @throws IllegalArgumentException if values are invalid
   */
  public static CreditUpdatePacket create(int credits, int total) {
    if (credits < 0 || total < 0) {
      throw new IllegalArgumentException(
          "Credits and total must be non-negative. Got: credits=" + credits + ", total=" + total);
    }
    return new CreditUpdatePacket(credits, total);
  }
}
