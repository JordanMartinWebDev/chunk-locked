package chunkloaded.test;

import net.minecraft.nbt.CompoundTag;

/**
 * Test probe to verify NBT operations in Yarn mappings for Minecraft 1.21.11
 */
public class ApiProbe {

  // Test NBT compound creation and serialization

  public ApiProbe() {
  }

  // Test NBT compound creation

  public CompoundTag testSerialize() {
    return new CompoundTag();
  }
}
