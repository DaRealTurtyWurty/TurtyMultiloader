package dev.turtywurty.turtymultiloader.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.turtywurty.turtymultiloader.worldgen.BiomeSelectionContext;
import dev.turtywurty.turtymultiloader.worldgen.BiomeSelector;
import dev.turtywurty.turtymultiloader.worldgen.BuiltInDatapackActivation;
import dev.turtywurty.turtymultiloader.worldgen.WorldGenerationService;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public final class NeoForgeWorldGenerationService implements WorldGenerationService {
    private static volatile NeoForgeWorldGenerationService instance;
    private static volatile boolean bound;

    private final List<DatapackRegistryDeclaration<?>> datapackRegistries = new ArrayList<>();
    private final List<BootstrapDeclaration<?>> bootstraps = new ArrayList<>();
    private final List<BuiltInDatapackDeclaration> builtInDatapacks = new ArrayList<>();
    private final List<BiomeModificationDeclaration> biomeModifications = new ArrayList<>();
    private final Set<ResourceKey<? extends Registry<?>>> datapackRegistryKeys = new HashSet<>();
    private final Set<Identifier> builtInDatapackIds = new HashSet<>();
    private final Set<Identifier> biomeModificationIds = new HashSet<>();
    private boolean datapackRegistryEventFired;

    public NeoForgeWorldGenerationService() {
        synchronized (NeoForgeWorldGenerationService.class) {
            if (instance != null)
                throw new IllegalStateException("NeoForge world-generation service was instantiated twice");
            instance = this;
        }
    }

    public static synchronized void bind(IEventBus modBus) {
        Objects.requireNonNull(modBus, "modBus");
        if (bound)
            throw new IllegalStateException("NeoForge world-generation service is already bound to a mod event bus");
        bound = true;
        modBus.addListener(NeoForgeWorldGenerationService::handleDatapackRegistries);
        modBus.addListener(NeoForgeWorldGenerationService::handlePackFinders);
    }

    @Override
    public synchronized <T> void registerDatapackRegistry(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec,
        @Nullable Codec<T> networkCodec
    ) {
        if (datapackRegistryEventFired)
            throw new IllegalStateException("Datapack registries must be declared before DataPackRegistryEvent.NewRegistry");
        requireNew(datapackRegistryKeys, registryKey, "datapack registry");
        datapackRegistries.add(new DatapackRegistryDeclaration<>(registryKey, datapackCodec, networkCodec));
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
        requireNew(builtInDatapackIds, id, "built-in datapack");
        builtInDatapacks.add(new BuiltInDatapackDeclaration(id, displayName, activation));
    }

    @Override
    public synchronized void addFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        addModification(new FeatureDeclaration(
            requireModificationId(modificationId),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(step, "step"),
            Objects.requireNonNull(feature, "feature"),
            false
        ));
    }

    @Override
    public synchronized void removeFeature(
        Identifier modificationId,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature
    ) {
        addModification(new FeatureDeclaration(
            requireModificationId(modificationId),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(step, "step"),
            Objects.requireNonNull(feature, "feature"),
            true
        ));
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
        addModification(new SpawnDeclaration(
            requireModificationId(modificationId),
            Objects.requireNonNull(selector, "selector"),
            Objects.requireNonNull(category, "category"),
            Objects.requireNonNull(entityType, "entityType"),
            weight,
            minimumGroupSize,
            maximumGroupSize,
            false
        ));
    }

    @Override
    public synchronized void removeSpawn(
        Identifier modificationId,
        BiomeSelector selector,
        Supplier<? extends EntityType<?>> entityType
    ) {
        addModification(new SpawnDeclaration(
            requireModificationId(modificationId),
            Objects.requireNonNull(selector, "selector"),
            null,
            Objects.requireNonNull(entityType, "entityType"),
            0,
            0,
            0,
            true
        ));
    }

    /**
     * Called by the NeoForge mixin immediately before native datapack biome modifiers are applied.
     */
    public static List<BiomeModifier> appendCodeModifiers(
        List<BiomeModifier> datapackModifiers,
        RegistryAccess registryAccess
    ) {
        NeoForgeWorldGenerationService service = instance;
        if (service == null)
            return datapackModifiers;
        return service.createModifierList(datapackModifiers, registryAccess);
    }

    private synchronized List<BiomeModifier> createModifierList(
        List<BiomeModifier> datapackModifiers,
        RegistryAccess registryAccess
    ) {
        if (biomeModifications.isEmpty())
            return datapackModifiers;
        List<BiomeModifier> combined = new ArrayList<>(datapackModifiers.size() + biomeModifications.size());
        combined.addAll(datapackModifiers);
        biomeModifications.stream()
            .map(declaration -> declaration.create(registryAccess))
            .forEach(combined::add);
        return List.copyOf(combined);
    }

    private void addModification(BiomeModificationDeclaration declaration) {
        biomeModifications.add(declaration);
    }

    private Identifier requireModificationId(Identifier id) {
        requireNew(biomeModificationIds, id, "biome modification");
        return id;
    }

    private static void handleDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        NeoForgeWorldGenerationService service = instance;
        if (service == null)
            return;
        synchronized (service) {
            service.datapackRegistryEventFired = true;
            service.datapackRegistries.forEach(declaration -> registerDatapackRegistry(event, declaration));
        }
    }

    private static void handlePackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA)
            return;
        NeoForgeWorldGenerationService service = instance;
        if (service == null)
            return;
        synchronized (service) {
            service.builtInDatapacks.forEach(declaration -> {
                BuiltInDatapackActivation activation = declaration.activation();
                Identifier location = Identifier.fromNamespaceAndPath(
                    declaration.id().getNamespace(),
                    "resourcepacks/" + declaration.id().getPath()
                );
                event.addPackFinders(
                    location,
                    PackType.SERVER_DATA,
                    declaration.displayName(),
                    activation == BuiltInDatapackActivation.NORMAL ? PackSource.FEATURE : PackSource.BUILT_IN,
                    activation == BuiltInDatapackActivation.ALWAYS_ENABLED,
                    Pack.Position.TOP
                );
            });
        }
    }

    private static <T> void registerDatapackRegistry(
        DataPackRegistryEvent.NewRegistry event,
        DatapackRegistryDeclaration<T> declaration
    ) {
        @SuppressWarnings("unchecked")
        ResourceKey<Registry<T>> key = (ResourceKey<Registry<T>>) declaration.registryKey();
        event.dataPackRegistry(key, declaration.datapackCodec(), declaration.networkCodec());
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

    private static BiomeSelectionContext selectionContext(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrapKey()
            .orElseThrow(() -> new IllegalStateException("Cannot apply a code modifier to an unregistered biome"));
        return new BiomeSelectionContext(key, biome, biome.value());
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

    private sealed interface BiomeModificationDeclaration permits FeatureDeclaration, SpawnDeclaration {
        BiomeModifier create(RegistryAccess registryAccess);
    }

    private record FeatureDeclaration(
        Identifier id,
        BiomeSelector selector,
        GenerationStep.Decoration step,
        ResourceKey<PlacedFeature> feature,
        boolean remove
    ) implements BiomeModificationDeclaration {
        @Override
        public BiomeModifier create(RegistryAccess registryAccess) {
            Holder<PlacedFeature> featureHolder = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(feature);
            return new CodeFeatureModifier(selector, step, featureHolder, remove);
        }
    }

    private record SpawnDeclaration(
        Identifier id,
        BiomeSelector selector,
        @Nullable MobCategory category,
        Supplier<? extends EntityType<?>> entityType,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize,
        boolean remove
    ) implements BiomeModificationDeclaration {
        @Override
        public BiomeModifier create(RegistryAccess registryAccess) {
            EntityType<?> type = Objects.requireNonNull(entityType.get(), "entityType supplier result");
            if (!remove && type.getCategory() != category) {
                throw new IllegalArgumentException(
                    "Entity category " + type.getCategory() + " does not match requested category " + category
                );
            }
            return new CodeSpawnModifier(selector, category, type, weight, minimumGroupSize, maximumGroupSize, remove);
        }
    }

    private record CodeFeatureModifier(
        BiomeSelector selector,
        GenerationStep.Decoration step,
        Holder<PlacedFeature> feature,
        boolean remove
    ) implements BiomeModifier {
        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            Phase expectedPhase = remove ? Phase.REMOVE : Phase.ADD;
            if (phase != expectedPhase || !selector.test(selectionContext(biome)))
                return;
            if (remove)
                builder.getGenerationSettings().getFeatures(step).removeIf(feature::equals);
            else
                builder.getGenerationSettings().addFeature(step, feature);
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return MapCodec.unit(this);
        }
    }

    private record CodeSpawnModifier(
        BiomeSelector selector,
        @Nullable MobCategory category,
        EntityType<?> entityType,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize,
        boolean remove
    ) implements BiomeModifier {
        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            Phase expectedPhase = remove ? Phase.REMOVE : Phase.ADD;
            if (phase != expectedPhase || !selector.test(selectionContext(biome)))
                return;
            if (remove) {
                for (MobCategory mobCategory : MobCategory.values()) {
                    builder.getMobSpawnSettings().getSpawner(mobCategory)
                        .removeIf(spawner -> spawner.value().type() == entityType);
                }
            } else {
                builder.getMobSpawnSettings().addSpawn(
                    Objects.requireNonNull(category),
                    weight,
                    new MobSpawnSettings.SpawnerData(entityType, minimumGroupSize, maximumGroupSize)
                );
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return MapCodec.unit(this);
        }
    }

    private record DatapackRegistryDeclaration<T>(
        ResourceKey<? extends Registry<T>> registryKey,
        Codec<T> datapackCodec,
        @Nullable Codec<T> networkCodec
    ) {
    }

    private record BootstrapDeclaration<T>(
        ResourceKey<? extends Registry<T>> registryKey,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
    }

    private record BuiltInDatapackDeclaration(
        Identifier id,
        Component displayName,
        BuiltInDatapackActivation activation
    ) {
    }
}
