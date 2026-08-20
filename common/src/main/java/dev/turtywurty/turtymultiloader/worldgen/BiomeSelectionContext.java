package dev.turtywurty.turtymultiloader.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Vanilla-only view of a biome while code-based biome modifications are being applied.
 */
public record BiomeSelectionContext(ResourceKey<Biome> key, Holder<Biome> holder, Biome biome) {
    public boolean is(ResourceKey<Biome> biomeKey) {
        return key.equals(biomeKey);
    }

    public boolean is(TagKey<Biome> tag) {
        return holder.is(tag);
    }
}
