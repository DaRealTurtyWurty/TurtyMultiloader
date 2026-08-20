package dev.turtywurty.turtymultiloader.fabric.datagen;

import dev.turtywurty.turtymultiloader.datagen.DataGenerationSpec;
import dev.turtywurty.turtymultiloader.datagen.DataProviderContext;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Fabric adapter for a common {@link DataGenerationSpec}.
 */
public final class FabricDataGeneration {
    private FabricDataGeneration() {
    }

    /**
     * Registers all common providers. Call this from {@code DataGeneratorEntrypoint.onInitializeDataGenerator}.
     */
    public static void run(FabricDataGenerator generator, DataGenerationSpec spec) {
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(spec, "spec");
        if (!generator.getModId().equals(spec.modId())) {
            throw new IllegalArgumentException(
                "Data-generation spec for " + spec.modId() + " cannot run for Fabric mod " + generator.getModId()
            );
        }

        FabricDataGenerator.Pack pack = generator.createPack();
        for (DataGenerationSpec.ProviderDeclaration declaration : spec.providers()) {
            pack.addProvider((FabricDataGenerator.Pack.Factory<?>) output -> declaration.factory().create(
                new DataProviderContext(spec.modId(), output, generator.getRegistries())
            ));
        }

        if (!spec.recipeGenerators().isEmpty()) {
            pack.addProvider((output, registries) ->
                new FabricRecipeProviderAdapter(output, registries, spec.recipeGenerators())
            );
        }

        if (spec.generatesDynamicRegistries()) {
            pack.addProvider((output, registries) -> new FabricDynamicRegistryProvider(output, registries) {
                @Override
                protected void configure(HolderLookup.Provider lookup, Entries entries) {
                    if (spec.generatesAllDynamicRegistries()) {
                        DynamicRegistries.getDynamicRegistries().forEach(registry ->
                            addAll(entries, lookup, registry.key())
                        );
                    } else {
                        spec.dynamicRegistries().forEach(registry -> addAll(entries, lookup, registry));
                    }
                }

                @Override
                public String getName() {
                    return "Dynamic registries for " + spec.modId();
                }
            });
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addAll(
        FabricDynamicRegistryProvider.Entries entries,
        HolderLookup.Provider lookup,
        ResourceKey<? extends Registry<?>> registry
    ) {
        lookup.lookup((ResourceKey) registry).ifPresent(registryLookup ->
            addAllUnchecked(entries, (HolderLookup.RegistryLookup) registryLookup)
        );
    }

    private static <T> void addAllUnchecked(
        FabricDynamicRegistryProvider.Entries entries,
        HolderLookup.RegistryLookup<T> registry
    ) {
        entries.addAll(registry);
    }
}
