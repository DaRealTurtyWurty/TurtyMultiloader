package dev.turtywurty.turtymultiloader.client.registration;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Client-only facade for renderer, tint, fluid, and model registration.
 *
 * <p>Menu screens are registered through {@code ClientMenus}; key mappings, tooltip callbacks,
 * reload listeners, and world render stages are registered through {@code ClientEvents}.</p>
 */
public final class ClientRegistrations {
    private ClientRegistrations() {
    }

    public static <E extends Entity> void registerEntityRenderer(
        EntityType<? extends E> entityType,
        EntityRendererProvider<E> rendererProvider
    ) {
        require(entityType);
        registerEntityRenderer(() -> entityType, rendererProvider);
    }

    public static <E extends Entity> void registerEntityRenderer(
        Supplier<? extends EntityType<? extends E>> entityType,
        EntityRendererProvider<E> rendererProvider
    ) {
        ClientRegistrationService.get().registerEntityRenderer(require(entityType), require(rendererProvider));
    }

    public static <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        BlockEntityType<? extends E> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    ) {
        require(blockEntityType);
        registerBlockEntityRenderer(() -> blockEntityType, rendererProvider);
    }

    public static <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        Supplier<? extends BlockEntityType<? extends E>> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    ) {
        ClientRegistrationService.get().registerBlockEntityRenderer(require(blockEntityType), require(rendererProvider));
    }

    public static void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        ClientRegistrationService.get().registerModelLayer(require(layer), require(definition));
    }

    public static void registerBlockTintSources(List<BlockTintSource> tintSources, Block... blocks) {
        require(blocks);
        registerBlockTintSources(tintSources, Arrays.stream(blocks).<Supplier<? extends Block>>map(block -> () -> block).toList());
    }

    @SafeVarargs
    public static void registerBlockTintSources(
        List<BlockTintSource> tintSources,
        Supplier<? extends Block>... blocks
    ) {
        require(blocks);
        registerBlockTintSources(tintSources, List.of(blocks));
    }

    public static void registerBlockTintSources(
        List<BlockTintSource> tintSources,
        List<? extends Supplier<? extends Block>> blocks
    ) {
        ClientRegistrationService.get().registerBlockTintSources(
            List.copyOf(require(tintSources)),
            List.copyOf(require(blocks))
        );
    }

    public static void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec) {
        ClientRegistrationService.get().registerItemTintSource(require(id), require(codec));
    }

    public static void registerFluidModel(FluidModel.Unbaked model, Fluid... fluids) {
        require(fluids);
        registerFluidModel(model, Arrays.stream(fluids).<Supplier<? extends Fluid>>map(fluid -> () -> fluid).toList());
    }

    @SafeVarargs
    public static void registerFluidModel(FluidModel.Unbaked model, Supplier<? extends Fluid>... fluids) {
        require(fluids);
        registerFluidModel(model, List.of(fluids));
    }

    public static void registerFluidModel(
        FluidModel.Unbaked model,
        List<? extends Supplier<? extends Fluid>> fluids
    ) {
        ClientRegistrationService.get().registerFluidModel(require(model), List.copyOf(require(fluids)));
    }

    public static void registerItemModel(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec) {
        ClientRegistrationService.get().registerItemModel(require(id), require(codec));
    }

    public static void registerSpecialModelRenderer(
        Identifier id,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec
    ) {
        ClientRegistrationService.get().registerSpecialModelRenderer(require(id), require(codec));
    }

    public static AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(Identifier modelId) {
        Identifier id = require(modelId);
        return registerAdditionalBlockStateModel(
            id,
            AdditionalBlockStateModelDefinition.blockStateModel(id)
        );
    }

    public static AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(
        Identifier modelId,
        ModelState modelState
    ) {
        Identifier id = require(modelId);
        return registerAdditionalBlockStateModel(
            id,
            AdditionalBlockStateModelDefinition.blockStateModel(id, require(modelState))
        );
    }

    public static AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(
        Identifier id,
        AdditionalBlockStateModelDefinition definition
    ) {
        return ClientRegistrationService.get().registerAdditionalBlockStateModel(require(id), require(definition));
    }

    public static void registerBlockStateModelAugmenter(Block block, BlockStateModelAugmenter augmenter) {
        require(block);
        registerBlockStateModelAugmenter(() -> block, augmenter);
    }

    public static void registerBlockStateModelAugmenter(
        Supplier<? extends Block> block,
        BlockStateModelAugmenter augmenter
    ) {
        Supplier<? extends Block> target = require(block);
        registerBlockStateModelAugmenter(state -> state.is(target.get()), augmenter);
    }

    public static void registerBlockStateModelAugmenter(
        Predicate<BlockState> selector,
        BlockStateModelAugmenter augmenter
    ) {
        ClientRegistrationService.get().registerBlockStateModelAugmenter(require(selector), require(augmenter));
    }

    /**
     * Registers a custom pipeline with vanilla's static render-pipeline registry.
     *
     * @return the supplied pipeline
     */
    public static RenderPipeline registerRenderPipeline(RenderPipeline pipeline) {
        return RenderPipelines.register(require(pipeline));
    }

    /**
     * Creates a render type from a registered pipeline setup.
     */
    public static RenderType createRenderType(String name, RenderSetup setup) {
        return RenderType.create(require(name), require(setup));
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
