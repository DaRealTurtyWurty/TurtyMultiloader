package dev.turtywurty.turtymultiloader.datagen.convention;

import dev.turtywurty.turtymultiloader.datagen.DataGenerationService;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Objects;

/**
 * A loader-neutral convention-tag identity.
 *
 * <p>Common code should retain this descriptor rather than importing a Fabric or NeoForge tag constant. The active
 * loader resolves it to the appropriate native tag key when {@link #key()} is called.</p>
 */
public record ConventionTag<T>(
    ResourceKey<? extends Registry<T>> registry,
    Identifier fabricId,
    Identifier commonId
) {
    public ConventionTag {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(fabricId, "fabricId");
        Objects.requireNonNull(commonId, "commonId");
    }

    public TagKey<T> key() {
        return DataGenerationService.get().resolveConventionTag(this);
    }
}
