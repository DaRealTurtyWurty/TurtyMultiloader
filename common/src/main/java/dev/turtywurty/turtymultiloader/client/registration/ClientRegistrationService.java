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

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Client-only loader bridge for static renderer and model registrations.
 * Never resolve this service from a dedicated server.
 */
public interface ClientRegistrationService {
    static ClientRegistrationService get() {
        return ServiceHolder.INSTANCE;
    }

    <E extends Entity> void registerEntityRenderer(
        Supplier<? extends EntityType<? extends E>> entityType,
        EntityRendererProvider<E> rendererProvider
    );

    <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        Supplier<? extends BlockEntityType<? extends E>> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    );

    void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition);

    void registerBlockTintSources(
        List<BlockTintSource> tintSources,
        List<? extends Supplier<? extends Block>> blocks
    );

    void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec);

    void registerFluidModel(
        FluidModel.Unbaked model,
        List<? extends Supplier<? extends Fluid>> fluids
    );

    void registerItemModel(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec);

    void registerSpecialModelRenderer(
        Identifier id,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec
    );

    AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(Identifier modelId);

    final class ServiceHolder {
        private static final ClientRegistrationService INSTANCE = ServiceLoader.load(
                ClientRegistrationService.class,
                ClientRegistrationService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client registration service is available"));

        private ServiceHolder() {
        }
    }
}
