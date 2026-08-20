package dev.turtywurty.turtymultiloader.datagen.provider;

import com.google.gson.JsonElement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs common model declarations against vanilla's typed block and item model generators.
 */
public final class FunctionalVanillaModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockStates;
    private final PackOutput.PathProvider itemDefinitions;
    private final PackOutput.PathProvider models;
    private final ModelGenerator generator;

    public FunctionalVanillaModelProvider(PackOutput output, ModelGenerator generator) {
        Objects.requireNonNull(output, "output");
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.itemDefinitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        BlockStateCollector blockStates = new BlockStateCollector();
        ItemCollector items = new ItemCollector();
        ModelCollector models = new ModelCollector();
        this.generator.generate(
            new BlockModelGenerators(blockStates, items, models),
            new ItemModelGenerators(items, models)
        );
        items.resolveCopies();
        return CompletableFuture.allOf(
            blockStates.save(output, this.blockStates),
            items.save(output, this.itemDefinitions),
            models.save(output, this.models)
        );
    }

    @Override
    public String getName() {
        return "Vanilla block and item models";
    }

    @FunctionalInterface
    public interface ModelGenerator {
        void generate(BlockModelGenerators blockModels, ItemModelGenerators itemModels);
    }

    private static final class BlockStateCollector implements Consumer<BlockModelDefinitionGenerator> {
        private final Map<Block, BlockModelDefinitionGenerator> generators = new HashMap<>();

        @Override
        public void accept(BlockModelDefinitionGenerator generator) {
            Block block = generator.block();
            if (this.generators.putIfAbsent(block, generator) != null)
                throw new IllegalStateException("Duplicate blockstate definition for " + block);
        }

        private CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider paths) {
            Map<Block, BlockStateModelDispatcher> definitions = new HashMap<>();
            this.generators.forEach((block, generator) -> definitions.put(block, generator.create()));
            Function<Block, Path> path = block -> paths.json(block.builtInRegistryHolder().key().identifier());
            return DataProvider.saveAll(output, BlockStateModelDispatcher.CODEC, path, definitions);
        }
    }

    private static final class ItemCollector implements ItemModelOutput {
        private final Map<Item, ClientItem> definitions = new HashMap<>();
        private final Map<Identifier, ClientItem> definitionsById = new HashMap<>();
        private final Map<Item, Item> copies = new HashMap<>();

        @Override
        public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties) {
            put(item, new ClientItem(model, properties));
        }

        // NeoForge extends ItemModelOutput with these registration hooks. They remain ordinary public methods when
        // compiled against Fabric's vanilla interface, keeping this provider source-compatible on both loaders.
        public void register(Item item, ClientItem definition) {
            put(item, definition);
        }

        public void register(Identifier id, ClientItem definition) {
            if (this.definitionsById.putIfAbsent(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(definition, "definition")
            ) != null)
                throw new IllegalStateException("Duplicate item model definition for " + id);
        }

        @Override
        public void copy(Item donor, Item acceptor) {
            Objects.requireNonNull(donor, "donor");
            Objects.requireNonNull(acceptor, "acceptor");
            if (this.copies.putIfAbsent(acceptor, donor) != null)
                throw new IllegalStateException("Duplicate item model copy for " + acceptor);
        }

        private void resolveCopies() {
            this.copies.forEach((acceptor, donor) -> {
                ClientItem definition = this.definitions.get(donor);
                if (definition == null)
                    throw new IllegalStateException("Missing item model donor " + donor + " for " + acceptor);
                put(acceptor, definition);
            });
        }

        private void put(Item item, ClientItem definition) {
            if (this.definitions.putIfAbsent(item, definition) != null)
                throw new IllegalStateException("Duplicate item model definition for " + item);
        }

        private CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider paths) {
            return CompletableFuture.allOf(
                DataProvider.saveAll(
                    output,
                    ClientItem.CODEC,
                    item -> paths.json(item.builtInRegistryHolder().key().identifier()),
                    this.definitions
                ),
                DataProvider.saveAll(output, ClientItem.CODEC, paths::json, this.definitionsById)
            );
        }
    }

    private static final class ModelCollector implements BiConsumer<Identifier, ModelInstance> {
        private final Map<Identifier, ModelInstance> models = new HashMap<>();

        @Override
        public void accept(Identifier id, ModelInstance model) {
            if (this.models.putIfAbsent(Objects.requireNonNull(id, "id"), Objects.requireNonNull(model, "model")) != null)
                throw new IllegalStateException("Duplicate model definition for " + id);
        }

        private CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider paths) {
            Map<Identifier, Supplier<JsonElement>> definitions = new HashMap<>(this.models);
            return DataProvider.saveAll(output, Supplier::get, paths::json, definitions);
        }
    }
}
