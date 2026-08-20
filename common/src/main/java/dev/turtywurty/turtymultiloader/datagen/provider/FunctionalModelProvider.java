package dev.turtywurty.turtymultiloader.datagen.provider;

import com.google.gson.JsonElement;
import dev.turtywurty.turtymultiloader.datagen.model.ModelGenerationContext;
import dev.turtywurty.turtymultiloader.datagen.model.ModelGenerationContext.ModelResource;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Writes loader-neutral blockstate, model, and item-definition JSON declarations.
 */
public final class FunctionalModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockStates;
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider itemDefinitions;
    private final ModelGenerator generator;

    public FunctionalModelProvider(PackOutput output, ModelGenerator generator) {
        Objects.requireNonNull(output, "output");
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.itemDefinitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        ModelGenerationContext context = new ModelGenerationContext();
        generator.generate(context);

        List<CompletableFuture<?>> writes = new ArrayList<>();
        context.resources().forEach((resource, json) ->
            writes.add(DataProvider.saveStable(output, json, path(resource)))
        );
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private Path path(ModelResource resource) {
        return switch (resource.type()) {
            case BLOCK_STATE -> blockStates.json(resource.id());
            case BLOCK_MODEL -> models.json(prefixed(resource.id(), "block/"));
            case ITEM_MODEL -> models.json(prefixed(resource.id(), "item/"));
            case ITEM_DEFINITION -> itemDefinitions.json(resource.id());
        };
    }

    private static Identifier prefixed(Identifier id, String prefix) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath());
    }

    @Override
    public String getName() {
        return "Blockstates and models";
    }

    @FunctionalInterface
    public interface ModelGenerator {
        void generate(ModelGenerationContext models);
    }
}
