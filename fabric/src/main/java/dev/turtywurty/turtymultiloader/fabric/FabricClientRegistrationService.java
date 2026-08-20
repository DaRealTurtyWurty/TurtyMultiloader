package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.serialization.MapCodec;
import dev.turtywurty.turtymultiloader.client.registration.AdditionalModel;
import dev.turtywurty.turtymultiloader.client.registration.ClientRegistrationService;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class FabricClientRegistrationService implements ClientRegistrationService {
    @Override
    public <E extends Entity> void registerEntityRenderer(
        Supplier<? extends EntityType<? extends E>> entityType,
        EntityRendererProvider<E> rendererProvider
    ) {
        EntityRenderers.register(require(entityType).get(), require(rendererProvider));
    }

    @Override
    public <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        Supplier<? extends BlockEntityType<? extends E>> blockEntityType,
        BlockEntityRendererProvider<E, S> rendererProvider
    ) {
        BlockEntityRenderers.register(require(blockEntityType).get(), require(rendererProvider));
    }

    @Override
    public void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        ModelLayerRegistry.registerModelLayer(require(layer), require(definition)::get);
    }

    @Override
    public void registerBlockTintSources(
        List<BlockTintSource> tintSources,
        List<? extends Supplier<? extends Block>> blocks
    ) {
        BlockColorRegistry.register(
            List.copyOf(require(tintSources)),
            require(blocks).stream().map(Supplier::get).toArray(Block[]::new)
        );
    }

    @Override
    public void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec) {
        ItemTintSources.ID_MAPPER.put(require(id), require(codec));
    }

    @Override
    public void registerFluidModel(
        FluidModel.Unbaked model,
        List<? extends Supplier<? extends Fluid>> fluids
    ) {
        for (Supplier<? extends Fluid> fluid : require(fluids))
            FluidRenderingRegistry.register(require(fluid).get(), require(model));
    }

    @Override
    public void registerItemModel(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec) {
        ItemModels.ID_MAPPER.put(require(id), require(codec));
    }

    @Override
    public void registerSpecialModelRenderer(
        Identifier id,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec
    ) {
        SpecialModelRenderers.ID_MAPPER.put(require(id), require(codec));
    }

    @Override
    public AdditionalModel<BlockStateModel> registerAdditionalBlockStateModel(Identifier modelId) {
        Identifier id = require(modelId);
        ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
        ModelLoadingPlugin.register(context ->
            context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id))
        );
        return new FabricAdditionalModel(id, key);
    }

    private record FabricAdditionalModel(
        Identifier id,
        ExtraModelKey<BlockStateModel> key
    ) implements AdditionalModel<BlockStateModel> {
        @Override
        public @Nullable BlockStateModel get(ModelManager modelManager) {
            return ((FabricModelManager) Objects.requireNonNull(modelManager, "modelManager")).getModel(key);
        }
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
