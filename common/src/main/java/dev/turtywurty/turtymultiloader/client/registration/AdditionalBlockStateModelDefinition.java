package dev.turtywurty.turtymultiloader.client.registration;

import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Loader-neutral description of a standalone block-state model before it is baked.
 *
 * <p>Fabric converts this to a {@code SimpleUnbakedExtraModel}; NeoForge converts it to a
 * {@code SimpleUnbakedStandaloneModel}.</p>
 */
public record AdditionalBlockStateModelDefinition(Identifier modelId, ModelState modelState) {
    public AdditionalBlockStateModelDefinition {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(modelState, "modelState");
    }

    public static AdditionalBlockStateModelDefinition blockStateModel(Identifier modelId) {
        return blockStateModel(modelId, BlockModelRotation.IDENTITY);
    }

    public static AdditionalBlockStateModelDefinition blockStateModel(Identifier modelId, ModelState modelState) {
        return new AdditionalBlockStateModelDefinition(modelId, modelState);
    }
}
