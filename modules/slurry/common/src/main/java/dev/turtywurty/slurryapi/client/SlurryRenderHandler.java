package dev.turtywurty.slurryapi.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;

public interface SlurryRenderHandler {
    TextureAtlasSprite getSprite(BlockAndTintGetter view, BlockPos pos);

    default int getColor(BlockAndTintGetter view, BlockPos pos) {
        return -1;
    }

    default void reloadTextures(TextureAtlas textureAtlas) {
    }
}
