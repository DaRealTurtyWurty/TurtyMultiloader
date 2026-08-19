package dev.turtywurty.slurryapi.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class SimpleSlurryRenderHandler implements SlurryRenderHandler {
    private final Identifier texture;
    private final int tint;
    private TextureAtlasSprite sprite;

    public SimpleSlurryRenderHandler(Identifier texture, int tint) {
        this.texture = texture;
        this.tint = tint;
    }

    public SimpleSlurryRenderHandler(Identifier texture) {
        this(texture, -1);
    }

    @Override
    public TextureAtlasSprite getSprite(BlockAndTintGetter view, BlockPos pos) {
        return this.sprite;
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        this.sprite = textureAtlas.getSprite(this.texture);
    }

    @Override
    public int getColor(BlockAndTintGetter view, BlockPos pos) {
        return this.tint;
    }
}
