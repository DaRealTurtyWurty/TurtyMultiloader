package dev.turtywurty.turtymultiloader.client.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Common-code access to the low-level operations hidden by vanilla's {@link GuiGraphicsExtractor}.
 *
 * <p>The extractor's normal public methods should still be called through {@link #extractor()}. This adapter covers
 * its private rendering helpers and the custom render-state submission operations exposed by NeoForge but absent
 * from vanilla and Fabric.</p>
 */
public final class GuiGraphicsExtractorAdapter {
    private final GuiGraphicsExtractor extractor;

    public GuiGraphicsExtractorAdapter(GuiGraphicsExtractor extractor) {
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    public GuiGraphicsExtractorAdapter(
        Minecraft minecraft,
        Matrix3x2fStack pose,
        GuiRenderState renderState,
        int mouseX,
        int mouseY
    ) {
        this(new GuiGraphicsExtractor(
            Objects.requireNonNull(minecraft, "minecraft"),
            Objects.requireNonNull(pose, "pose"),
            Objects.requireNonNull(renderState, "renderState"),
            mouseX,
            mouseY
        ));
    }

    public GuiGraphicsExtractor extractor() {
        return extractor;
    }

    public GuiRenderState renderState() {
        return extractor.guiRenderState;
    }

    public @Nullable ScreenRectangle peekScissorStack() {
        return extractor.scissorStack.peek();
    }

    public void submitGuiElementRenderState(GuiElementRenderState renderState) {
        extractor.guiRenderState.addGuiElement(Objects.requireNonNull(renderState, "renderState"));
    }

    public void submitPictureInPictureRenderState(PictureInPictureRenderState renderState) {
        extractor.guiRenderState.addPicturesInPictureState(Objects.requireNonNull(renderState, "renderState"));
    }

    public void innerFill(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        int x0,
        int y0,
        int x1,
        int y1,
        int color1,
        @Nullable Integer color2
    ) {
        extractor.innerFill(pipeline, textureSetup, x0, y0, x1, y1, color1, color2);
    }

    public void blitSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        int spriteWidth,
        int spriteHeight,
        int textureX,
        int textureY,
        int x,
        int y,
        int width,
        int height,
        int color
    ) {
        extractor.blitSprite(
            pipeline, sprite, spriteWidth, spriteHeight, textureX, textureY, x, y, width, height, color);
    }

    public void blitNineSlicedSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        GuiSpriteScaling.NineSlice nineSlice,
        int x,
        int y,
        int width,
        int height,
        int color
    ) {
        extractor.blitNineSlicedSprite(pipeline, sprite, nineSlice, x, y, width, height, color);
    }

    public void blitNineSliceInnerSegment(
        RenderPipeline pipeline,
        GuiSpriteScaling.NineSlice nineSlice,
        TextureAtlasSprite sprite,
        int x,
        int y,
        int width,
        int height,
        int textureX,
        int textureY,
        int textureWidth,
        int textureHeight,
        int spriteWidth,
        int spriteHeight,
        int color
    ) {
        extractor.blitNineSliceInnerSegment(
            pipeline, nineSlice, sprite, x, y, width, height, textureX, textureY, textureWidth, textureHeight,
            spriteWidth, spriteHeight, color);
    }

    public void blitTiledSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        int x,
        int y,
        int width,
        int height,
        int textureX,
        int textureY,
        int tileWidth,
        int tileHeight,
        int spriteWidth,
        int spriteHeight,
        int color
    ) {
        extractor.blitTiledSprite(
            pipeline, sprite, x, y, width, height, textureX, textureY, tileWidth, tileHeight, spriteWidth,
            spriteHeight, color);
    }

    public void innerBlit(
        RenderPipeline pipeline,
        Identifier texture,
        int x0,
        int x1,
        int y0,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        extractor.innerBlit(pipeline, texture, x0, x1, y0, y1, u0, u1, v0, v1, color);
    }

    public void innerBlit(
        RenderPipeline pipeline,
        GpuTextureView texture,
        GpuSampler sampler,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        extractor.innerBlit(pipeline, texture, sampler, x0, y0, x1, y1, u0, u1, v0, v1, color);
    }

    public void innerTiledBlit(
        RenderPipeline pipeline,
        GpuTextureView texture,
        GpuSampler sampler,
        int tileWidth,
        int tileHeight,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        extractor.innerTiledBlit(
            pipeline, texture, sampler, tileWidth, tileHeight, x0, y0, x1, y1, u0, u1, v0, v1, color);
    }

    public static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return GuiGraphicsExtractor.getSpriteScaling(Objects.requireNonNull(sprite, "sprite"));
    }

    public void item(
        @Nullable LivingEntity owner,
        @Nullable Level level,
        ItemStack itemStack,
        int x,
        int y,
        int seed
    ) {
        extractor.item(owner, level, itemStack, x, y, seed);
    }

    public void itemBar(ItemStack itemStack, int x, int y) {
        extractor.itemBar(itemStack, x, y);
    }

    public void itemCount(Font font, ItemStack itemStack, int x, int y, @Nullable String countText) {
        extractor.itemCount(font, itemStack, x, y, countText);
    }

    public void itemCooldown(ItemStack itemStack, int x, int y) {
        extractor.itemCooldown(itemStack, x, y);
    }

    public void setTooltipForNextFrame(
        Font font,
        List<ClientTooltipComponent> lines,
        int x,
        int y,
        ClientTooltipPositioner positioner,
        @Nullable Identifier style,
        boolean replaceExisting
    ) {
        extractor.setTooltipForNextFrameInternal(font, lines, x, y, positioner, style, replaceExisting);
    }

    public void componentHoverEffect(Font font, Style hoveredStyle, int mouseX, int mouseY) {
        extractor.componentHoverEffect(font, hoveredStyle, mouseX, mouseY);
    }

    public ActiveTextCollector.Parameters createDefaultTextParameters(float opacity) {
        return extractor.createDefaultTextParameters(opacity);
    }
}
