package chunkloaded;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import chunkloaded.client.ClientChunkDataHolder;
import chunkloaded.network.CreditUpdatePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side mod initializer for Chunk Locked.
 * <p>
 * Sets up client-specific features including:
 * - Network packet handlers for credit synchronization
 * - Client-side data initialization
 * - HUD and UI setup (future)
 */
public class ChunklockedClient implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Chunk Locked client");
		registerPacketHandlers();
	}

	/**
	 * Registers packet handlers for client-side network communication.
	 * <p>
	 * Currently handles:
	 * - {@link CreditUpdatePacket} - Server→Client credit synchronization
	 */
	private static void registerPacketHandlers() {
		// Register handler for credit update packets from server
		ClientPlayNetworking.registerGlobalReceiver(CreditUpdatePacket.ID,
				(packet, context) -> {
					// Schedule on render thread to update client data
					context.client().execute(() -> {
						ClientChunkDataHolder.setAll(packet.credits(), packet.total());
						LOGGER.debug("Received credit update: credits={}, total={}",
								packet.credits(), packet.total());
					});
				});

		LOGGER.debug("Registered packet handlers");
	}
}
