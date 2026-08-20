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
import org.jspecify.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Loader backend for world-generation declarations.
 */
public interface WorldGenerationService {
    static WorldGenerationService get() {
        return ServiceHolder.INSTANCE;
    }

    <T> void registerDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec,
        @Nullable Codec<T> networkCodec
    );

    <T> void registerBootstrap(
        ResourceKey<? extends Registry<T>> registryKey,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    );

    void addBootstraps(RegistrySetBuilder builder);

    void registerBuiltInDatapack(
        Identifier id,
        Component displayName,
        BuiltInDatapackActivation activation
    );

    void addFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    );

    void removeFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    );

    void addSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        MobCategory category,
        Supplier<? extends EntityType<?>> entityType,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize
    );

    void removeSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        Supplier<? extends EntityType<?>> entityType
    );

    final class ServiceHolder {
        private static final WorldGenerationService INSTANCE = ServiceLoader.load(
                WorldGenerationService.class,
                WorldGenerationService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No world-generation service is available"));

        private ServiceHolder() {
        }
    }
}
