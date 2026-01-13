package chunkloaded;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunklockedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register barrier block with translucent render layer for transparency
		// This MUST be done on the client side, before world load
		BlockRenderLayerMap.putBlock(Chunklocked.BARRIER_BLOCK_V2, ChunkSectionLayer.TRANSLUCENT);

		Chunklocked.LOGGER.info("Registered barrier block with translucent render layer");
	}
}