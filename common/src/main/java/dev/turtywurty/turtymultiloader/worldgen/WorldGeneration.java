package dev.turtywurty.turtymultiloader.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Common entry point for datapack registries, built-in datapacks, and code-based biome modifications.
 */
public final class WorldGeneration {
    private WorldGeneration() {
    }

    public static <T> void registerDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec
    ) {
        WorldGenerationService.get().registerDatapackRegistry(
            Objects.requireNonNull(registryKey, "registryKey"),
            Objects.requireNonNull(datapackCodec, "datapackCodec"),
            null
        );
    }

    public static <T> void registerSyncedDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec
    ) {
        registerSyncedDatapackRegistry(registryKey, datapackCodec, datapackCodec);
    }

    public static <T> void registerSyncedDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec,
        Codec<T> networkCodec
    ) {
        WorldGenerationService.get().registerDatapackRegistry(
            Objects.requireNonNull(registryKey, "registryKey"),
            Objects.requireNonNull(datapackCodec, "datapackCodec"),
            Objects.requireNonNull(networkCodec, "networkCodec")
        );
    }

    /**
     * Declares a bootstrap used by data generators. Call {@link #addBootstraps(RegistrySetBuilder)} from the loader's
     * registry-data-generator callback.
     */
    public static <T> void registerBootstrap(
        ResourceKey<? extends Registry<T>> registryKey,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
        WorldGenerationService.get().registerBootstrap(
            Objects.requireNonNull(registryKey, "registryKey"),
            Objects.requireNonNull(bootstrap, "bootstrap")
        );
    }

    public static void addBootstraps(RegistrySetBuilder builder) {
        WorldGenerationService.get().addBootstraps(Objects.requireNonNull(builder, "builder"));
    }

    /**
     * Registers a pack stored at {@code resourcepacks/<id path>} in the owning mod's resources.
     */
    public static void registerBuiltInDatapack(
        Identifier id,
        Component displayName,
        BuiltInDatapackActivation activation
    ) {
        WorldGenerationService.get().registerBuiltInDatapack(
            Objects.requireNonNull(id, "id"),
            Objects.requireNonNull(displayName, "displayName"),
            Objects.requireNonNull(activation, "activation")
        );
    }

    public static void addFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        WorldGenerationService.get().addFeature(
            Objects.requireNonNull(modificationId, "modificationId"),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(step, "step"),
            Objects.requireNonNull(feature, "feature")
        );
    }

    public static void removeFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        WorldGenerationService.get().removeFeature(
            Objects.requireNonNull(modificationId, "modificationId"),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(step, "step"),
            Objects.requireNonNull(feature, "feature")
        );
    }

    public static void addSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        MobCategory category,
        Supplier<? extends EntityType<?>> entityType,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize
    ) {
        WorldGenerationService.get().addSpawn(
            Objects.requireNonNull(modificationId, "modificationId"),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(category, "category"),
            Objects.requireNonNull(entityType, "entityType"),
            weight,
            minimumGroupSize,
            maximumGroupSize
        );
    }

    public static void removeSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        Supplier<? extends EntityType<?>> entityType
    ) {
        WorldGenerationService.get().removeSpawn(
            Objects.requireNonNull(modificationId, "modificationId"),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(entityType, "entityType")
        );
    }
}
