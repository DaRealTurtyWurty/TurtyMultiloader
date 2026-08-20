package dev.turtywurty.turtymultiloader.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Loader-neutral inputs supplied to a data-provider factory.
 */
public record DataProviderContext(
    String modId,
    PackOutput output,
    CompletableFuture<HolderLookup.Provider> registries
) {
    public DataProviderContext {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(registries, "registries");
    }
}
