package dev.turtywurty.turtymultiloader.neoforge;

import com.mojang.serialization.MapCodec;
import dev.turtywurty.turtymultiloader.client.registration.AdditionalModel;
import dev.turtywurty.turtymultiloader.client.registration.ClientRegistrationService;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class NeoForgeClientRegistrationService implements ClientRegistrationService {
    private static IEventBus modBus;

    private final List<RendererDeclaration> renderers = new ArrayList<>();
    private final Map<ModelLayerLocation, Supplier<LayerDefinition>> modelLayers = new LinkedHashMap<>();
    private final List<BlockTintDeclaration> blockTints = new ArrayList<>();
    private final Map<Identifier, MapCodec<? extends ItemTintSource>> itemTintSources = new LinkedHashMap<>();
    private final List<FluidModelDeclaration> fluidModels = new ArrayList<>();
    private final Map<Identifier, MapCodec<? extends ItemModel.Unbaked>> itemModels = new LinkedHashMap<>();
    private final Map<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> specialModelRenderers =
        new LinkedHashMap<>();
    private final List<NeoForgeAdditionalModel> additionalModels = new ArrayList<>();

    private boolean renderersClosed;
    private boolean modelLayersClosed;
    private boolean blockTintsClosed;
    private boolean itemTintsClosed;
    private boolean fluidModelsClosed;
    private boolean itemModelsClosed;
    private boolean specialModelRenderersClosed;
    private boolean additionalModelsClosed;

    public static synchronized void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge client registrations are already bound to a mod event bus");
        if (modBus != null)
            return;

        modBus = Objects.requireNonNull(bus, "bus");
        NeoForgeClientRegistrationService service = instance();
        bus.addListener(EntityRenderersEvent.RegisterRenderers.class, service::registerRenderers);
        bus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, service::registerModelLayers);
        bus.addListener(RegisterColorHandlersEvent.BlockTintSources.class, service::registerBlockTints);
        bus.addListener(RegisterColorHandlersEvent.ItemTintSources.class, service::registerItemTints);
        bus.addListener(RegisterFluidModelsEvent.class, service::registerFluidModels);
        bus.addListener(RegisterItemModelsEvent.class, service::registerItemModels);
        bus.addListener(RegisterSpecialModelRendererEvent.class, service::registerSpecialModelRenderers);
        bus.addListener(ModelEvent.RegisterStandalone.class, service::registerAdditionalModels);
    }

    @Override
    public synchronized <E extends Entity> void registerEntityRenderer(
        Supplier<? extends EntityType<? extends E>> entityType,
        EntityRendererProvider<E> rendererProvider
    ) {
        ensureOpen(renderersClosed, "Entity renderers");
        renderers.add(new EntityRendererDeclaration<>(require(entityType), require(rendererProvider)));
    }

    @Override
    public synchronized <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        Supplier<? extends BlockEntityType<? extends E>> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    ) {
        ensureOpen(renderersClosed, "Block-entity renderers");
        renderers.add(new BlockEntityRendererDeclaration<>(require(blockEntityType), require(rendererProvider)));
    }

    @Override
    public synchronized void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        ensureOpen(modelLayersClosed, "Model layers");
        if (modelLayers.putIfAbsent(require(layer), require(definition)) != null)
            throw new IllegalStateException("A model layer is already registered as " + layer);
    }

    @Override
    public synchronized void registerBlockTintSources(
        List<BlockTintSource> tintSources,
        List<? extends Supplier<? extends Block>> blocks
    ) {
        ensureOpen(blockTintsClosed, "Block tint sources");
        blockTints.add(new BlockTintDeclaration(List.copyOf(require(tintSources)), List.copyOf(require(blocks))));
    }

    @Override
    public synchronized void registerItemTintSource(
        Identifier id,
        MapCodec<? extends ItemTintSource> codec
    ) {
        ensureOpen(itemTintsClosed, "Item tint sources");
        putUnique(itemTintSources, require(id), require(codec), "item tint source");
    }

    @Override
    public synchronized void registerFluidModel(
        FluidModel.Unbaked model,
        List<? extends Supplier<? extends Fluid>> fluids
    ) {
        ensureOpen(fluidModelsClosed, "Fluid models");
        fluidModels.add(new FluidModelDeclaration(require(model), List.copyOf(require(fluids))));
    }

    @Override
    public synchronized void registerItemModel(
        Identifier id,
        MapCodec<? extends ItemModel.Unbaked> codec
    ) {
        ensureOpen(itemModelsClosed, "Item models");
        putUnique(itemModels, require(id), require(codec), "item model");
    }

    @Override
    public synchronized void registerSpecialModelRenderer(
        Identifier id,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec
    ) {
        ensureOpen(specialModelRenderersClosed, "Special model renderers");
        putUnique(specialModelRenderers, require(id), require(codec), "special model renderer");
    }

    @Override
    public synchronized AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(Identifier modelId) {
        ensureOpen(additionalModelsClosed, "Additional models");
        Identifier id = require(modelId);
        if (additionalModels.stream().anyMatch(model -> model.id().equals(id)))
            throw new IllegalStateException("An additional model is already registered as " + id);

        NeoForgeAdditionalModel model = new NeoForgeAdditionalModel(
            id,
            new StandaloneModelKey<>(id::toString)
        );
        additionalModels.add(model);
        return model;
    }

    private synchronized void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        renderers.forEach(declaration -> declaration.register(event));
        renderersClosed = true;
    }

    private synchronized void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        modelLayers.forEach(event::registerLayerDefinition);
        modelLayersClosed = true;
    }

    private synchronized void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        blockTints.forEach(declaration -> declaration.register(event));
        blockTintsClosed = true;
    }

    private synchronized void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        itemTintSources.forEach(event::register);
        itemTintsClosed = true;
    }

    private synchronized void registerFluidModels(RegisterFluidModelsEvent event) {
        fluidModels.forEach(declaration -> declaration.register(event));
        fluidModelsClosed = true;
    }

    private synchronized void registerItemModels(RegisterItemModelsEvent event) {
        itemModels.forEach(event::register);
        itemModelsClosed = true;
    }

    private synchronized void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        specialModelRenderers.forEach(event::register);
        specialModelRenderersClosed = true;
    }

    private synchronized void registerAdditionalModels(ModelEvent.RegisterStandalone event) {
        additionalModels.forEach(model -> event.register(
            model.key(),
            SimpleUnbakedStandaloneModel.blockStateModel(model.id())
        ));
        additionalModelsClosed = true;
    }

    private interface RendererDeclaration {
        void register(EntityRenderersEvent.RegisterRenderers event);
    }

    private record EntityRendererDeclaration<E extends Entity>(
        Supplier<? extends EntityType<? extends E>> entityType,
        EntityRendererProvider<E> rendererProvider
    ) implements RendererDeclaration {
        @Override
        public void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(entityType.get(), rendererProvider);
        }
    }

    private record BlockEntityRendererDeclaration<E extends BlockEntity, S extends BlockEntityRenderState>(
        Supplier<? extends BlockEntityType<? extends E>> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    ) implements RendererDeclaration {
        @Override
        public void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(blockEntityType.get(), rendererProvider);
        }
    }

    private record BlockTintDeclaration(
        List<BlockTintSource> tintSources,
        List<? extends Supplier<? extends Block>> blocks
    ) {
        private void register(RegisterColorHandlersEvent.BlockTintSources event) {
            event.register(tintSources, blocks.stream().map(Supplier::get).toArray(Block[]::new));
        }
    }

    private record FluidModelDeclaration(
        FluidModel.Unbaked model,
        List<? extends Supplier<? extends Fluid>> fluids
    ) {
        private void register(RegisterFluidModelsEvent event) {
            fluids.forEach(fluid -> event.register(model, fluid));
        }
    }

    private record NeoForgeAdditionalModel(
        Identifier id,
        StandaloneModelKey<BlockStateModel> key
    ) implements AdditionalModel<BlockStateModel> {
        @Override
        public @Nullable BlockStateModel get(ModelManager modelManager) {
            return Objects.requireNonNull(modelManager, "modelManager").getStandaloneModel(key);
        }
    }

    private static NeoForgeClientRegistrationService instance() {
        ClientRegistrationService service = ClientRegistrationService.get();
        if (!(service instanceof NeoForgeClientRegistrationService neoForgeService))
            throw new IllegalStateException("NeoForge client registration service provider is not active");
        return neoForgeService;
    }

    private static void ensureOpen(boolean closed, String type) {
        if (closed)
            throw new IllegalStateException(type + " must be registered during client initialization");
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value, String type) {
        if (map.putIfAbsent(key, value) != null)
            throw new IllegalStateException("A " + type + " is already registered as " + key);
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
