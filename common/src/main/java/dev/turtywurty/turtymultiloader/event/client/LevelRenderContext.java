package dev.turtywurty.turtymultiloader.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Vanilla-only view of the current level rendering callback.
 */
public record LevelRenderContext(
    Minecraft client,
    ClientLevel level,
    GameRenderer gameRenderer,
    LevelRenderer levelRenderer,
    LevelRenderState levelState,
    PoseStack poseStack,
    MultiBufferSource.BufferSource bufferSource,
    @Nullable SubmitNodeCollector submitNodeCollector
) {
    public LevelRenderContext {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gameRenderer, "gameRenderer");
        Objects.requireNonNull(levelRenderer, "levelRenderer");
        Objects.requireNonNull(levelState, "levelState");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(bufferSource, "bufferSource");
    }
}
