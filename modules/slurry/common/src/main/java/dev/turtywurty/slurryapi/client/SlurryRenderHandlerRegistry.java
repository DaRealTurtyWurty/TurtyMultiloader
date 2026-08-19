package dev.turtywurty.slurryapi.client;

import dev.turtywurty.slurryapi.api.Slurry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class SlurryRenderHandlerRegistry {
    private static final Map<Slurry, SlurryRenderHandler> HANDLERS = new IdentityHashMap<>();
    private static TextureAtlasSprite missingSprite;
    private static final SlurryRenderHandler MISSING_HANDLER = new SlurryRenderHandler() {
        @Override
        public TextureAtlasSprite getSprite(BlockAndTintGetter view, BlockPos pos) {
            if (missingSprite == null) {
                missingSprite = Minecraft.getInstance().getAtlasManager().get(
                    new SpriteId(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation())
                );
            }
            return missingSprite;
        }
    };

    private SlurryRenderHandlerRegistry() {
    }

    public static synchronized SlurryRenderHandler get(Slurry slurry) {
        return HANDLERS.getOrDefault(slurry, MISSING_HANDLER);
    }

    public static synchronized void register(Slurry slurry, SlurryRenderHandler handler) {
        if (HANDLERS.putIfAbsent(
            Objects.requireNonNull(slurry, "slurry"),
            Objects.requireNonNull(handler, "handler")
        ) != null)
            throw new IllegalStateException("Duplicate handler for slurry: " + slurry);
    }

    public static synchronized void onResourcesReload() {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        HANDLERS.values().forEach(handler -> handler.reloadTextures(atlas));
    }
}
