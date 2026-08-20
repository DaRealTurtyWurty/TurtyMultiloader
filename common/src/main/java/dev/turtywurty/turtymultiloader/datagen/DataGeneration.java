package dev.turtywurty.turtymultiloader.datagen;

import net.minecraft.core.RegistrySetBuilder;

import java.util.Objects;

/**
 * Common data-generation entry points used from loader data-generator hooks.
 */
public final class DataGeneration {
    private DataGeneration() {
    }

    public static DataGenerationSpec.Builder spec(String modId) {
        return DataGenerationSpec.builder(modId);
    }

    /**
     * Called from Fabric's {@code DataGeneratorEntrypoint.buildRegistry}. NeoForge runners call this automatically.
     */
    public static void addRegistryBootstraps(DataGenerationSpec spec, RegistrySetBuilder builder) {
        Objects.requireNonNull(spec, "spec").addRegistryBootstraps(Objects.requireNonNull(builder, "builder"));
    }
}
