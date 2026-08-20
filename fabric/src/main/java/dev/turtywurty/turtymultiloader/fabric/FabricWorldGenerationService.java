package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.serialization.Codec;
import dev.turtywurty.turtymultiloader.worldgen.BiomeSelectionContext;
import dev.turtywurty.turtymultiloader.worldgen.BiomeSelector;
import dev.turtywurty.turtymultiloader.worldgen.BuiltInDatapackActivation;
import dev.turtywurty.turtymultiloader.worldgen.WorldGenerationService;
import net.fabricmc.fabric.api.biome.v1.BiomeModification;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public final class FabricWorldGenerationService implements WorldGenerationService {
    private final List<BootstrapDeclaration<?>> bootstraps = new ArrayList<>();
    private final Set<ResourceKey<? extends Registry<?>>> datapackRegistries = new HashSet<>();
    private final Set<Identifier> builtInDatapacks = new HashSet<>();
    private final Set<Identifier> biomeModifications = new HashSet<>();

    @Override
    public synchronized <T> void registerDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec,
        @Nullable Codec<T> networkCodec
    ) {
        requireNew(datapackRegistries, registryKey, "datapack registry");
        if (networkCodec == null)
            DynamicRegistries.register(registryKey, datapackCodec);
        else
            DynamicRegistries.registerSynced(registryKey, datapackCodec, networkCodec);
    }

    @Override
    public synchronized <T> void registerBootstrap(
        ResourceKey<? extends Registry<T>> registryKey,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
        bootstraps.add(new BootstrapDeclaration<>(registryKey, bootstrap));
    }

    @Override
    public synchronized void addBootstraps(RegistrySetBuilder builder) {
        Map<ResourceKey<? extends Registry<?>>, List<BootstrapDeclaration<?>>> grouped = new LinkedHashMap<>();
        bootstraps.forEach(declaration -> grouped
            .computeIfAbsent(declaration.registryKey(), ignored -> new ArrayList<>())
            .add(declaration));
        grouped.forEach((registryKey, declarations) -> addBootstraps(builder, registryKey, declarations));
    }

    @Override
    public synchronized void registerBuiltInDatapack(
        Identifier id,
        Component displayName,
        BuiltInDatapackActivation activation
    ) {
        requireNew(builtInDatapacks, id, "built-in datapack");
        var container = FabricLoader.getInstance().getModContainer(id.getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "No Fabric mod owns the built-in datapack namespace " + id.getNamespace()
            ));
        boolean registered = ResourceLoader.registerBuiltinPack(
            id,
            container,
            displayName,
            switch (activation) {
                case NORMAL -> PackActivationType.NORMAL;
                case DEFAULT_ENABLED -> PackActivationType.DEFAULT_ENABLED;
                case ALWAYS_ENABLED -> PackActivationType.ALWAYS_ENABLED;
            }
        );
        if (!registered) {
            builtInDatapacks.remove(id);
            throw new IllegalArgumentException(
                "Could not find built-in datapack resourcepacks/" + id.getPath() + " for " + id
            );
        }
    }

    @Override
    public synchronized void addFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        create(modificationId).add(
            ModificationPhase.ADDITIONS,
            context -> selector.test(selectionContext(context)),
            context -> context.getGenerationSettings().addFeature(step, feature)
        );
    }

    @Override
    public synchronized void removeFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        create(modificationId).add(
            ModificationPhase.REMOVALS,
            context -> selector.test(selectionContext(context)),
            context -> context.getGenerationSettings().removeFeature(step, feature)
        );
    }

    @Override
    public synchronized void addSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        MobCategory category,
        Supplier<? extends EntityType<?>> entityType,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize
    ) {
        validateSpawn(weight, minimumGroupSize, maximumGroupSize);
        create(modificationId).add(
            ModificationPhase.ADDITIONS,
            context -> selector.test(selectionContext(context)),
            context -> {
                EntityType<?> type = Objects.requireNonNull(entityType.get(), "entityType supplier result");
                if (type.getCategory() != category) {
                    throw new IllegalArgumentException(
                        "Entity category " + type.getCategory() + " does not match requested category " + category
                    );
                }
                context.getMobSpawnSettings().addSpawn(
                    category,
                    new MobSpawnSettings.SpawnerData(type, minimumGroupSize, maximumGroupSize),
                    weight
                );
            }
        );
    }

    @Override
    public synchronized void removeSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        Supplier<? extends EntityType<?>> entityType
    ) {
        create(modificationId).add(
            ModificationPhase.REMOVALS,
            context -> selector.test(selectionContext(context)),
            context -> context.getMobSpawnSettings().removeSpawnsOfEntityType(
                Objects.requireNonNull(entityType.get(), "entityType supplier result")
            )
        );
    }

    private BiomeModification create(Identifier id) {
        requireNew(biomeModifications, Objects.requireNonNull(id, "modificationId"), "biome modification");
        return BiomeModifications.create(id);
    }

    private static BiomeSelectionContext selectionContext(
        net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext context
    ) {
        return new BiomeSelectionContext(context.getBiomeKey(), context.getBiomeHolder(), context.getBiome());
    }

    private static void validateSpawn(int weight, int minimumGroupSize, int maximumGroupSize) {
        if (weight <= 0)
            throw new IllegalArgumentException("Spawn weight must be positive");
        if (minimumGroupSize <= 0 || maximumGroupSize < minimumGroupSize) {
            throw new IllegalArgumentException(
                "Spawn group sizes must satisfy 0 < minimumGroupSize <= maximumGroupSize"
            );
        }
    }

    private static <T> void requireNew(Set<T> values, T value, String kind) {
        if (!values.add(Objects.requireNonNull(value, kind)))
            throw new IllegalArgumentException("Duplicate " + kind + " declaration: " + value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addBootstraps(
        RegistrySetBuilder builder,
        ResourceKey<? extends Registry<?>> registryKey,
        List<BootstrapDeclaration<?>> declarations
    ) {
        builder.add((ResourceKey) registryKey, context -> declarations.forEach(declaration ->
            ((RegistrySetBuilder.RegistryBootstrap) declaration.bootstrap()).run(context)
        ));
    }

    private record BootstrapDeclaration<T>(
        ResourceKey<? extends Registry<T>> registryKey,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
    }
}
