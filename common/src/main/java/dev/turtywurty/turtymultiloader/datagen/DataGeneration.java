package dev.turtywurty.turtymultiloader.datagen;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

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

    /** Supplies only world-layer entries to Fabric's buildRegistry hook. */
    public static void addWorldRegistryBootstraps(DataGenerationSpec spec, RegistrySetBuilder builder) {
        addFilteredBootstraps(spec, builder, false);
    }

    /** Supplies only reloadable entries to Fabric's buildReloadableRegistry hook. */
    public static void addReloadableRegistryBootstraps(DataGenerationSpec spec, RegistrySetBuilder builder) {
        addFilteredBootstraps(spec, builder, true);
    }

    private static boolean isReloadable(ResourceKey<?> key) {
        return RegistryDataLoader.RELOADABLE_REGISTRIES.stream()
            .anyMatch(registry -> registry.key().equals(key));
    }

    private static void addFilteredBootstraps(DataGenerationSpec spec, RegistrySetBuilder builder, boolean reloadable) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(builder, "builder");
        spec.addRegistryBootstraps(new RegistrySetBuilder() {
            @Override
            public <T> RegistrySetBuilder add(
                ResourceKey<? extends Registry<T>> key,
                SingleRegistryBootstrap<T> bootstrap
            ) {
                if (isReloadable(key) == reloadable) {
                    builder.add(key, bootstrap);
                }
                return this;
            }
            @Override
            public RegistrySetBuilder add(MultiRegistryBootstrap bootstrap) {
                var layers = bootstrap.requestedRegistries().stream().map(DataGeneration::isReloadable).distinct()
                    .toList();
                if (layers.size() > 1)
                    throw new IllegalArgumentException("Bootstrap spans world and reloadable registries");
                if (!layers.isEmpty() && layers.getFirst() == reloadable) {
                    builder.add(bootstrap);
                }
                return this;
            }
        });
    }

    /**
     * Adds every layer to a builder; loader adapters select the appropriate registry layer.
     */
    public static void addRegistryBootstraps(DataGenerationSpec spec, RegistrySetBuilder builder) {
        Objects.requireNonNull(spec, "spec").addRegistryBootstraps(Objects.requireNonNull(builder, "builder"));
    }
}
