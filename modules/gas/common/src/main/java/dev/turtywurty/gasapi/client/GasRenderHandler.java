package dev.turtywurty.gasapi.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

public interface GasRenderHandler {
    default int getColor(BlockAndTintGetter view, BlockPos pos) {
        return -1;
    }
}
