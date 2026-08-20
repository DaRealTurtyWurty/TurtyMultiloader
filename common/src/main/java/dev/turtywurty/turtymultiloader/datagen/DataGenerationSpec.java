package dev.turtywurty.turtymultiloader.datagen;

import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalLanguageProvider;
import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalModelProvider;
import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalRecipeProvider;
import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalTagsProvider;
import dev.turtywurty.turtymultiloader.worldgen.WorldGeneration;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Immutable, loader-neutral description of a mod's generated resource pack.
 */
public final class DataGenerationSpec {
    private final String modId;
    private final List<ProviderDeclaration> providers;
    private final List<FunctionalRecipeProvider.RecipeGenerator> recipeGenerators;
    private final List<RegistryBootstrapDeclaration<?>> registryBootstraps;
    private final List<Consumer<RegistrySetBuilder>> registryContributors;
    private final boolean generateAllDynamicRegistries;

    private DataGenerationSpec(Builder builder) {
        this.modId = builder.modId;
        List<ProviderDeclaration> composedProviders = new ArrayList<>(builder.providers);
        builder.tagGenerators.forEach((registry, generators) ->
            composedProviders.add(tagProvider(registry, generators))
        );
        builder.languageGenerators.forEach((locale, generators) -> {
            List<FunctionalLanguageProvider.LanguageGenerator> generatorCopy = List.copyOf(generators);
            composedProviders.add(new ProviderDeclaration(
                DataGenerationSide.CLIENT,
                context -> new FunctionalLanguageProvider(context.output(), context.modId(), locale, translations ->
                    generatorCopy.forEach(generator -> generator.generate(translations))
                )
            ));
        });
        if (!builder.modelGenerators.isEmpty()) {
            List<FunctionalModelProvider.ModelGenerator> modelGenerators = List.copyOf(builder.modelGenerators);
            composedProviders.add(new ProviderDeclaration(
                DataGenerationSide.CLIENT,
                context -> new FunctionalModelProvider(context.output(), models ->
                    modelGenerators.forEach(generator -> generator.generate(models))
                )
            ));
        }
        if (!builder.lootRequiredTables.isEmpty() || !builder.lootSubProviders.isEmpty()) {
            Set<ResourceKey<LootTable>> requiredTables = Set.copyOf(builder.lootRequiredTables);
            List<SubProviderEntry> lootProviders = List.copyOf(builder.lootSubProviders);
            composedProviders.add(new ProviderDeclaration(
                DataGenerationSide.SERVER,
                context -> new LootTableProvider(
                    context.output(),
                    requiredTables,
                    lootProviders,
                    context.registries()
                )
            ));
        }
        this.providers = List.copyOf(composedProviders);
        this.recipeGenerators = List.copyOf(builder.recipeGenerators);
        this.registryBootstraps = List.copyOf(builder.registryBootstraps);
        this.registryContributors = List.copyOf(builder.registryContributors);
        this.generateAllDynamicRegistries = builder.generateAllDynamicRegistries;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ProviderDeclaration tagProvider(
        ResourceKey<? extends Registry<?>> registry,
        List<FunctionalTagsProvider.TagGenerator<?>> generators
    ) {
        List<FunctionalTagsProvider.TagGenerator<?>> generatorCopy = List.copyOf(generators);
        return new ProviderDeclaration(DataGenerationSide.SERVER, context -> new FunctionalTagsProvider(
            context.output(),
            registry,
            context.registries(),
            (lookup, tags) -> generatorCopy.forEach(generator ->
                ((FunctionalTagsProvider.TagGenerator) generator).generate(lookup, tags)
            )
        ));
    }

    public static Builder builder(String modId) {
        return new Builder(modId);
    }

    public String modId() {
        return modId;
    }

    public List<ProviderDeclaration> providers() {
        return providers;
    }

    public List<FunctionalRecipeProvider.RecipeGenerator> recipeGenerators() {
        return recipeGenerators;
    }

    public Set<ResourceKey<? extends Registry<?>>> dynamicRegistries() {
        if (generateAllDynamicRegistries)
            return Set.of();

        var registries = new java.util.LinkedHashSet<ResourceKey<? extends Registry<?>>>();
        registryBootstraps.forEach(declaration -> registries.add(declaration.registry()));
        return Collections.unmodifiableSet(registries);
    }

    public boolean generatesDynamicRegistries() {
        return generateAllDynamicRegistries || !registryBootstraps.isEmpty() || !registryContributors.isEmpty();
    }

    public boolean generatesAllDynamicRegistries() {
        return generateAllDynamicRegistries;
    }

    /**
     * Adds the declared dynamic-registry bootstraps to a loader-owned builder.
     */
    public void addRegistryBootstraps(RegistrySetBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        Map<ResourceKey<? extends Registry<?>>, List<RegistryBootstrapDeclaration<?>>> grouped = new LinkedHashMap<>();
        for (RegistryBootstrapDeclaration<?> declaration : registryBootstraps)
            grouped.computeIfAbsent(declaration.registry(), ignored -> new ArrayList<>()).add(declaration);
        grouped.forEach((registry, declarations) -> addComposedBootstrap(builder, registry, declarations));
        registryContributors.forEach(contributor -> contributor.accept(builder));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addComposedBootstrap(
        RegistrySetBuilder builder,
        ResourceKey<? extends Registry<?>> registry,
        List<RegistryBootstrapDeclaration<?>> declarations
    ) {
        builder.add((ResourceKey) registry, context -> declarations.forEach(declaration ->
            ((RegistrySetBuilder.RegistryBootstrap) declaration.bootstrap()).run(context)
        ));
    }

    public record ProviderDeclaration(DataGenerationSide side, DataProviderFactory factory) {
        public ProviderDeclaration {
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(factory, "factory");
        }
    }

    private record RegistryBootstrapDeclaration<T>(
        ResourceKey<? extends Registry<T>> registry,
        RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
        private RegistryBootstrapDeclaration {
            Objects.requireNonNull(registry, "registry");
            Objects.requireNonNull(bootstrap, "bootstrap");
        }
    }

    public static final class Builder {
        private final String modId;
        private final List<ProviderDeclaration> providers = new ArrayList<>();
        private final List<FunctionalRecipeProvider.RecipeGenerator> recipeGenerators = new ArrayList<>();
        private final Map<ResourceKey<? extends Registry<?>>, List<FunctionalTagsProvider.TagGenerator<?>>> tagGenerators =
            new LinkedHashMap<>();
        private final Map<String, List<FunctionalLanguageProvider.LanguageGenerator>> languageGenerators =
            new LinkedHashMap<>();
        private final List<FunctionalModelProvider.ModelGenerator> modelGenerators = new ArrayList<>();
        private final Set<ResourceKey<LootTable>> lootRequiredTables = new LinkedHashSet<>();
        private final List<SubProviderEntry> lootSubProviders = new ArrayList<>();
        private final List<RegistryBootstrapDeclaration<?>> registryBootstraps = new ArrayList<>();
        private final List<Consumer<RegistrySetBuilder>> registryContributors = new ArrayList<>();
        private boolean generateAllDynamicRegistries;

        private Builder(String modId) {
            this.modId = Objects.requireNonNull(modId, "modId");
            Identifier.fromNamespaceAndPath(modId, "data_generation_validation");
        }

        public Builder provider(DataGenerationSide side, DataProviderFactory factory) {
            providers.add(new ProviderDeclaration(side, factory));
            return this;
        }

        public Builder recipes(FunctionalRecipeProvider.RecipeGenerator generator) {
            recipeGenerators.add(Objects.requireNonNull(generator, "generator"));
            return this;
        }

        public Builder lootTables(Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> providers) {
            lootRequiredTables.addAll(Objects.requireNonNull(requiredTables, "requiredTables"));
            lootSubProviders.addAll(Objects.requireNonNull(providers, "providers"));
            return this;
        }

        public <T> Builder tags(
            ResourceKey<? extends Registry<T>> registry,
            FunctionalTagsProvider.TagGenerator<T> generator
        ) {
            Objects.requireNonNull(registry, "registry");
            Objects.requireNonNull(generator, "generator");
            tagGenerators.computeIfAbsent(registry, ignored -> new ArrayList<>()).add(generator);
            return this;
        }

        public Builder blockTags(FunctionalTagsProvider.TagGenerator<Block> generator) {
            return tags(Registries.BLOCK, generator);
        }

        public Builder itemTags(FunctionalTagsProvider.TagGenerator<Item> generator) {
            return tags(Registries.ITEM, generator);
        }

        public Builder fluidTags(FunctionalTagsProvider.TagGenerator<Fluid> generator) {
            return tags(Registries.FLUID, generator);
        }

        public Builder entityTypeTags(FunctionalTagsProvider.TagGenerator<EntityType<?>> generator) {
            return tags(Registries.ENTITY_TYPE, generator);
        }

        public Builder language(String locale, FunctionalLanguageProvider.LanguageGenerator generator) {
            Objects.requireNonNull(locale, "locale");
            Objects.requireNonNull(generator, "generator");
            languageGenerators.computeIfAbsent(locale, ignored -> new ArrayList<>()).add(generator);
            return this;
        }

        public Builder models(FunctionalModelProvider.ModelGenerator generator) {
            Objects.requireNonNull(generator, "generator");
            modelGenerators.add(generator);
            return this;
        }

        public <T> Builder dynamicRegistry(
            ResourceKey<? extends Registry<T>> registry,
            RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            registryBootstraps.add(new RegistryBootstrapDeclaration<>(registry, bootstrap));
            return this;
        }

        public Builder damageTypes(RegistrySetBuilder.RegistryBootstrap<DamageType> bootstrap) {
            return dynamicRegistry(Registries.DAMAGE_TYPE, bootstrap);
        }

        /**
         * Alias for world-generation registries such as configured features, placed features, biomes, and structures.
         */
        public <T> Builder worldGeneration(
            ResourceKey<? extends Registry<T>> registry,
            RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            return dynamicRegistry(registry, bootstrap);
        }

        /**
         * Includes bootstraps previously declared through {@link WorldGeneration#registerBootstrap}.
         */
        public Builder registeredWorldGeneration() {
            registryContributors.add(WorldGeneration::addBootstraps);
            generateAllDynamicRegistries = true;
            return this;
        }

        /**
         * Adds an advanced registry contributor. All entries in the mod namespace are generated.
         */
        public Builder registryContributor(Consumer<RegistrySetBuilder> contributor) {
            registryContributors.add(Objects.requireNonNull(contributor, "contributor"));
            generateAllDynamicRegistries = true;
            return this;
        }

        public DataGenerationSpec build() {
            return new DataGenerationSpec(this);
        }
    }
}
