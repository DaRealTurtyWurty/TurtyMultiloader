package dev.turtywurty.multiblocklib.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MultiblockControllerRenderState extends BlockEntityRenderState {
    public final List<RenderPart> parts = new ArrayList<>();
    public final List<RenderPart> portMarkers = new ArrayList<>();

    public record RenderPart(BlockPos offset, MovingBlockRenderState movingBlockRenderState) {
    }
}
