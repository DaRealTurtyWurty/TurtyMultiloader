package dev.turtywurty.turtymultiloader.client.registration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;

final class WoodSetBoatRenderer extends AbstractBoatRenderer {
    private final Model.Simple waterPatchModel;
    private final EntityModel<BoatRenderState> model;

    WoodSetBoatRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
        super(context, modelLayer.model().withPath(path -> "textures/entity/" + path + ".png"));
        waterPatchModel = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), ignored -> RenderTypes.waterMask());
        model = new BoatModel(context.bakeLayer(modelLayer));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    protected void submitTypeAdditions(
        BoatRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords
    ) {
        if (!state.isUnderWater) {
            submitNodeCollector.submitModel(
                waterPatchModel, Unit.INSTANCE, poseStack, texture, lightCoords, OverlayTexture.NO_OVERLAY,
                state.outlineColor, null
            );
        }
    }
}
