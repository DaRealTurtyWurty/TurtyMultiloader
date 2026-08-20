package dev.turtywurty.turtymultiloader.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface BlockBreakCallback {
    /**
     * Runs for a successful server-player block break after its drops have been determined.
     *
     * @param blockEntity the removed block entity, when one was present
     */
    void afterBlockBreak(
        ServerLevel level,
        ServerPlayer player,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity blockEntity
    );
}
