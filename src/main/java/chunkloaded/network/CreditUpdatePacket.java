package chunkloaded.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
public record CreditUpdatePacket(int credits, int total) implements CustomPacketPayload {

  /**
   * Packet ID for the credit update payload.
   * Namespace: chunklocked, Name: credit_update
   */
  public static final CustomPacketPayload.Id<CreditUpdatePacket> ID = new CustomPacketPayload.Id<>(
      Identifier.of("chunklocked", "credit_update"));

  /**
   * Packet codec for encoding/decoding credit update packets.
   * <p>
   * Format:
   * - credits (int, 4 bytes): Available credits to unlock chunks
   * - total (int, 4 bytes): Total advancements completed by player
   */
  public static final StreamCodec<FriendlyByteBuf, CreditUpdatePacket> CODEC = StreamCodec.of(CreditUpdatePacket::encode,
      CreditUpdatePacket::decode);

  /**
   * Encodes this packet to a byte buffer.
   *
   * @param buf The buffer to write to
   */
  private void encode(FriendlyByteBuf buf) {
    buf.writeInt(this.credits);
    buf.writeInt(this.total);
  }

  /**
   * Decodes a packet from a byte buffer.
   *
   * @param buf The buffer to read from
   * @return The decoded packet
   */
  private static CreditUpdatePacket decode(FriendlyByteBuf buf) {
    int credits = buf.readInt();
    int total = buf.readInt();
    return new CreditUpdatePacket(credits, total);
  }

  /**
   * Gets the packet ID for this payload.
   *
   * @return The CustomPacketPayload.Id for this packet type
   */
  @Override
  public CustomPacketPayload.Id<? extends CustomPacketPayload> getId() {
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
