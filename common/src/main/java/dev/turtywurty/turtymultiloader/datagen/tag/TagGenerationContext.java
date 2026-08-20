package dev.turtywurty.turtymultiloader.datagen.tag;

import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTag;
import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.function.Function;

/**
 * Public access to a vanilla tag appender, including convention-tag resolution.
 */
public final class TagGenerationContext<T> {
    private final ResourceKey<? extends Registry<T>> registry;
    private final Function<TagKey<T>, TagAppender<ResourceKey<T>, T>> appenderFactory;

    public TagGenerationContext(
        ResourceKey<? extends Registry<T>> registry,
        Function<TagKey<T>, TagAppender<ResourceKey<T>, T>> appenderFactory
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.appenderFactory = Objects.requireNonNull(appenderFactory, "appenderFactory");
    }

    public ResourceKey<? extends Registry<T>> registry() {
        return registry;
    }

    public TagAppender<ResourceKey<T>, T> tag(TagKey<T> tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.isFor(registry))
            throw new IllegalArgumentException("Tag " + tag + " is not for registry " + registry.identifier());
        return appenderFactory.apply(tag);
    }

    public TagAppender<ResourceKey<T>, T> tag(ConventionTag<T> tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.registry().equals(registry)) {
            throw new IllegalArgumentException(
                "Convention tag " + tag + " is not for registry " + registry.identifier()
            );
        }
        return tag(tag.key());
    }
}
