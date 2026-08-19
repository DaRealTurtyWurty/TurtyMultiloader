package dev.turtywurty.gasapi.client;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public interface GasVariantRenderHandler {
    default void appendTooltip(ResourceVariant<Gas> variant, List<Component> tooltip, TooltipFlag tooltipType) {
    }

    default int getColor(ResourceVariant<Gas> variant, BlockAndTintGetter view, BlockPos pos) {
        GasRenderHandler handler = GasRenderHandlerRegistry.get(variant.value());
        return handler == null ? -1 : handler.getColor(view, pos) | 0xFF000000;
    }
}
