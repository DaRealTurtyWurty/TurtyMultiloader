package dev.turtywurty.slurryapi.client;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public interface SlurryVariantRenderHandler {
    default void appendTooltip(ResourceVariant<Slurry> variant, List<Component> tooltip, TooltipFlag tooltipType) {
    }

    default TextureAtlasSprite getSprite(ResourceVariant<Slurry> variant) {
        return SlurryRenderHandlerRegistry.get(variant.value()).getSprite(null, null);
    }

    default int getColor(ResourceVariant<Slurry> variant, BlockAndTintGetter view, BlockPos pos) {
        return SlurryRenderHandlerRegistry.get(variant.value()).getColor(view, pos) | 0xFF000000;
    }
}
