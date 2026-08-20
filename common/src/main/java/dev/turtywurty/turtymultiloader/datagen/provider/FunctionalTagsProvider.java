package dev.turtywurty.turtymultiloader.datagen.provider;

import dev.turtywurty.turtymultiloader.datagen.tag.TagGenerationContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A key-based vanilla tag provider that works for static and dynamic registries.
 */
public final class FunctionalTagsProvider<T> extends TagsProvider<T> {
    private final TagGenerator<T> generator;

    public FunctionalTagsProvider(
        PackOutput output,
        ResourceKey<? extends Registry<T>> registry,
        CompletableFuture<HolderLookup.Provider> registries,
        TagGenerator<T> generator
    ) {
        super(output, registry, registries);
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        generator.generate(registries, new TagGenerationContext<>(registryKey, this::appender));
    }

    private TagAppender<ResourceKey<T>, T> appender(TagKey<T> tag) {
        return TagAppender.forBuilder(getOrCreateRawBuilder(tag));
    }

    @FunctionalInterface
    public interface TagGenerator<T> {
        void generate(HolderLookup.Provider registries, TagGenerationContext<T> tags);
    }
}
