package chunklocked.core;

import chunklocked.core.ChunklockedMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers custom world presets for Chunklocked game modes.
 * <p>
 * This class defines two world types that appear in the world creation screen:
 * <ul>
 * <li><b>Chunklocked: Easy</b> - Generous credit rewards (TASK=1, GOAL=5,
 * CHALLENGE=10)</li>
 * <li><b>Chunklocked: Extreme</b> - Strict credit rewards (TASK=1, GOAL=2,
 * CHALLENGE=5)</li>
 * </ul>
 * <p>
 * These presets are identical to the default world generation but are used
 * to detect which mode the player selected during world creation.
 *
 * @since 1.0.0
 */
public class ChunklockedWorldPresets {
  private static final Logger LOGGER = LoggerFactory.getLogger("chunk-locked");

  /**
   * Resource key for the "Chunklocked: Easy" world preset.
   * <p>
   * Use this to detect if a world was created with Easy mode.
   */
  public static final ResourceKey<WorldPreset> CHUNKLOCKED_EASY = ResourceKey.create(
      Registries.WORLD_PRESET,
      Identifier.fromNamespaceAndPath("chunk-locked", "easy"));

  /**
   * Resource key for the "Chunklocked: Extreme" world preset.
   * <p>
   * Use this to detect if a world was created with Extreme mode.
   */
  public static final ResourceKey<WorldPreset> CHUNKLOCKED_EXTREME = ResourceKey.create(
      Registries.WORLD_PRESET,
      Identifier.fromNamespaceAndPath("chunk-locked", "extreme"));

  /**
   * Registers the custom world presets.
   * <p>
   * This should be called during mod initialization, but ONLY on the client side
   * since world presets are client-only for the world creation screen.
   * <p>
   * <b>Note</b>: In Minecraft 1.21+, world preset registration is complex and
   * requires data pack integration. This class defines the resource keys that
   * will be used to detect the selected preset on the server side.
   * <p>
   * The actual preset definitions should be placed in:
   * 
   * <pre>
   * src/main/resources/data/chunk-locked/worldgen/world_preset/easy.json
   * src/main/resources/data/chunk-locked/worldgen/world_preset/extreme.json
   * </pre>
   */
  public static void register() {
    LOGGER.info("Chunklocked world preset keys initialized:");
    LOGGER.info("  - Easy: chunk-locked:easy");
    LOGGER.info("  - Extreme: chunk-locked:extreme");
    LOGGER.info("World presets are defined via data packs in data/chunk-locked/worldgen/world_preset/");
  }

  /**
   * Detects the Chunklocked mode from a world preset resource key.
   * <p>
   * This is called on the server side when a world is loaded to determine
   * which mode the player selected during world creation.
   *
   * @param presetKey The world preset resource key, or null if default world
   * @return The detected ChunklockedMode (EASY, EXTREME, or DISABLED)
   */
  public static ChunklockedMode detectModeFromPreset(ResourceKey<WorldPreset> presetKey) {
    if (presetKey == null) {
      return ChunklockedMode.DISABLED;
    }

    if (presetKey.equals(CHUNKLOCKED_EASY)) {
      LOGGER.info("Detected Chunklocked: Easy world preset");
      return ChunklockedMode.EASY;
    } else if (presetKey.equals(CHUNKLOCKED_EXTREME)) {
      LOGGER.info("Detected Chunklocked: Extreme world preset");
      return ChunklockedMode.EXTREME;
    } else {
      LOGGER.debug("World created with non-Chunklocked preset (not Easy/Extreme)");
      return ChunklockedMode.DISABLED;
    }
  }
}
