package dev.turtywurty.gasapi.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

public final class SimpleGasRenderHandler implements GasRenderHandler {
    private final int color;

    public SimpleGasRenderHandler(int color) {
        this.color = color;
    }

    @Override
    public int getColor(BlockAndTintGetter view, BlockPos pos) {
        return this.color;
    }
}
