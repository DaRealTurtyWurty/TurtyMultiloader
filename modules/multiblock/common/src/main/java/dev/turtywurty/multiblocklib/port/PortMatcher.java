package dev.turtywurty.multiblocklib.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PortMatcher {
    boolean matches(BlockPos localOffset, @Nullable Direction localSide);
}
