package dev.turtywurty.turtymultiloader.client.registration;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
        return ClientRegistrationService.get().registerAdditionalBlockStateModel(require(modelId));
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
