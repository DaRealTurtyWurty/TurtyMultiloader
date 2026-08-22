package dev.turtywurty.turtymultiloader.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loader-neutral selectors for code-based biome modifications.
 */
public final class BiomeSelectors {
    private BiomeSelectors() {
    }

    public static BiomeSelector all() {
        return context -> true;
    }

    /**
     * Selects biomes which can generate in the Overworld.
     */
    public static BiomeSelector foundInOverworld() {
        return tag(BiomeTags.IS_OVERWORLD);
    }

    /**
     * Selects biomes which can generate in the Nether.
     */
    public static BiomeSelector foundInTheNether() {
        return tag(BiomeTags.IS_NETHER);
    }

    /**
     * Selects biomes which can generate in the End.
     */
    public static BiomeSelector foundInTheEnd() {
        return tag(BiomeTags.IS_END);
    }

    @SafeVarargs
    public static BiomeSelector includeByKey(ResourceKey<Biome>... keys) {
        return includeByKey(Arrays.asList(keys));
    }

    public static BiomeSelector includeByKey(Collection<ResourceKey<Biome>> keys) {
        Set<ResourceKey<Biome>> copiedKeys = Set.copyOf(keys);
        return context -> copiedKeys.contains(context.key());
    }

    @SafeVarargs
    public static BiomeSelector excludeByKey(ResourceKey<Biome>... keys) {
        return includeByKey(keys).negate();
    }

    public static BiomeSelector excludeByKey(Collection<ResourceKey<Biome>> keys) {
        return includeByKey(keys).negate();
    }

    public static BiomeSelector tag(TagKey<Biome> tag) {
        Objects.requireNonNull(tag, "tag");
        return context -> context.is(tag);
    }

    public static BiomeSelector namespace(String... namespaces) {
        Set<String> copiedNamespaces = Arrays.stream(namespaces)
            .map(namespace -> Objects.requireNonNull(namespace, "namespace"))
            .collect(Collectors.toUnmodifiableSet());
        return context -> copiedNamespaces.contains(context.key().identifier().getNamespace());
    }

    public static BiomeSelector hasPlacedFeature(ResourceKey<PlacedFeature> featureKey) {
        Objects.requireNonNull(featureKey, "featureKey");
        return context -> context.biome().getGenerationSettings().features().stream()
            .flatMap(features -> features.stream())
            .anyMatch(holder -> holder.unwrapKey().filter(featureKey::equals).isPresent());
    }

    public static BiomeSelector hasConfiguredFeature(ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
        Objects.requireNonNull(featureKey, "featureKey");
        return context -> context.biome().getGenerationSettings().features().stream()
            .flatMap(features -> features.stream())
            .flatMap(holder -> holder.value().getFeatures())
            .anyMatch(holder -> holder.unwrapKey().filter(featureKey::equals).isPresent());
    }
}
