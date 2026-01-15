package chunklocked.mixin.client;

import chunklocked.Chunklocked;
import chunklocked.core.ChunklockedWorldPresets;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mixin to inject Chunklocked world presets into the world creation GUI.
 * <p>
 * This intercepts the normal preset list generation and adds our custom presets
 * so they appear in the "World Type" dropdown during world creation.
 * <p>
 * Also tracks when the user selects our presets and saves marker files for
 * server-side mode detection.
 */
@Mixin(WorldCreationUiState.class)
public abstract class WorldPresetsMixin {

  @Shadow
  @Final
  private net.minecraft.client.gui.screens.worldselection.WorldCreationContext settings;

  /**
   * Helper method to save preset selection to temp file.
   * This is called when the user actually selects our preset.
   */
  private void savePresetSelection(String presetId) {
    try {
      Path gameDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath();
      Path tempFile = gameDir.resolve("chunklocked_preset_" + presetId.replace(":", "_") + ".tmp");

      // Delete all other preset markers first
      try {
        java.nio.file.Files.deleteIfExists(gameDir.resolve("chunklocked_preset_chunk-locked_easy.tmp"));
        java.nio.file.Files.deleteIfExists(gameDir.resolve("chunklocked_preset_chunk-locked_extreme.tmp"));
      } catch (Exception ignored) {
      }

      // Save the selected preset
      java.nio.file.Files.writeString(tempFile, presetId);
      Chunklocked.LOGGER.info("Saved preset marker for world creation: {}", presetId);
    } catch (Exception e) {
      Chunklocked.LOGGER.error("Failed to save preset marker", e);
    }
  }

  /**
   * Intercept setWorldType to detect when user selects our custom presets.
   * This is where we save the marker file for server-side detection.
   */
  @Inject(method = "setWorldType", at = @At("HEAD"))
  private void onWorldTypeChanged(WorldCreationUiState.WorldTypeEntry worldType, CallbackInfo ci) {
    try {
      // Get the preset from the world type entry
      Holder<WorldPreset> selectedPreset = worldType.preset();

      // Check if it's one of our custom presets
      Optional<ResourceKey<WorldPreset>> keyOpt = selectedPreset.unwrapKey();
      if (keyOpt.isPresent()) {
        ResourceKey<WorldPreset> key = keyOpt.get();

        if (key.equals(ChunklockedWorldPresets.CHUNKLOCKED_EASY)) {
          savePresetSelection("chunk-locked:easy");
          Chunklocked.LOGGER.info("User selected Chunklocked: Easy preset");
        } else if (key.equals(ChunklockedWorldPresets.CHUNKLOCKED_EXTREME)) {
          savePresetSelection("chunk-locked:extreme");
          Chunklocked.LOGGER.info("User selected Chunklocked: Extreme preset");
        }
      }
    } catch (Exception e) {
      Chunklocked.LOGGER.error("Failed to track world type selection", e);
    }
  }

  /**
   * Inject our custom world presets into the normal preset list.
   * <p>
   * This runs when the world creation screen populates the World Type button's
   * preset list.
   */
  @Inject(method = "getNormalPresetList", at = @At("RETURN"), cancellable = true)
  private void addChunklockedPresetsToNormalList(
      CallbackInfoReturnable<List<WorldCreationUiState.WorldTypeEntry>> cir) {
    Chunklocked.LOGGER.info("WorldPresetsMixin: addChunklockedPresetsToNormalList called!");
    injectCustomPresets(cir, "Normal");
  }

  /**
   * Inject our custom world presets into the alternate preset list.
   * <p>
   * This runs when the world creation screen uses the alt preset list for the
   * World Type button.
   */
  @Inject(method = "getAltPresetList", at = @At("RETURN"), cancellable = true)
  private void addChunklockedPresetsToAltList(
      CallbackInfoReturnable<List<WorldCreationUiState.WorldTypeEntry>> cir) {
    Chunklocked.LOGGER.info("WorldPresetsMixin: addChunklockedPresetsToAltList called!");
    injectCustomPresets(cir, "Alt");
  }

  /**
   * Common logic for injecting custom presets into either preset list.
   */
  private void injectCustomPresets(
      CallbackInfoReturnable<List<WorldCreationUiState.WorldTypeEntry>> cir,
      String listType) {
    try {
      // Get the original preset list
      List<WorldCreationUiState.WorldTypeEntry> originalList = new ArrayList<>(cir.getReturnValue());

      // Get the registry access from the world gen context
      HolderLookup.Provider registries = settings.worldgenLoadContext();

      // Get the world preset registry
      HolderLookup.RegistryLookup<WorldPreset> presetRegistry = registries.lookupOrThrow(Registries.WORLD_PRESET);

      // Get the normal preset to use as a template
      Optional<Holder.Reference<WorldPreset>> normalPresetOpt = presetRegistry.get(WorldPresets.NORMAL);

      if (normalPresetOpt.isEmpty()) {
        Chunklocked.LOGGER.warn("Could not find NORMAL preset as template");
        return;
      }

      Holder<WorldPreset> normalPreset = normalPresetOpt.get();

      // Try to get our custom presets from the registry (if they were loaded via data
      // pack)
      Optional<Holder.Reference<WorldPreset>> easyPresetOpt = presetRegistry
          .get(ChunklockedWorldPresets.CHUNKLOCKED_EASY);
      Optional<Holder.Reference<WorldPreset>> extremePresetOpt = presetRegistry
          .get(ChunklockedWorldPresets.CHUNKLOCKED_EXTREME);

      // Add Easy preset (use data pack version if available, otherwise use normal as
      // fallback)
      Holder<WorldPreset> easyPreset = easyPresetOpt.map(h -> (Holder<WorldPreset>) h).orElse(normalPreset);
      var easyEntry = new WorldCreationUiState.WorldTypeEntry(easyPreset);
      originalList.add(easyEntry);

      // Add Extreme preset (use data pack version if available, otherwise use normal
      // as fallback)
      Holder<WorldPreset> extremePreset = extremePresetOpt.map(h -> (Holder<WorldPreset>) h).orElse(normalPreset);
      var extremeEntry = new WorldCreationUiState.WorldTypeEntry(extremePreset);
      originalList.add(extremeEntry);

      cir.setReturnValue(originalList);

      Chunklocked.LOGGER.info(
          "Successfully injected Chunklocked world presets into {} preset list (Easy: {}, Extreme: {})",
          listType,
          easyPresetOpt.isPresent() ? "data pack" : "fallback",
          extremePresetOpt.isPresent() ? "data pack" : "fallback");
    } catch (Exception e) {
      Chunklocked.LOGGER.error("Failed to inject Chunklocked world presets into {} preset list", listType, e);
      // Don't crash - just use original presets
    }
  }
}
