package chunklocked;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Data generation for Chunklocked.
 * <p>
 * World presets are defined via static JSON files in:
 * - src/main/resources/data/chunk-locked/worldgen/world_preset/easy.json
 * - src/main/resources/data/chunk-locked/worldgen/world_preset/extreme.json
 * <p>
 * These files are automatically discovered by Minecraft's data pack system.
 * No dynamic generation is needed since our presets use vanilla dimensions.
 */
public class ChunklockedDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		// No providers needed - using static JSON files for world presets
		// This is the correct approach for presets that use vanilla world generation
	}
}
