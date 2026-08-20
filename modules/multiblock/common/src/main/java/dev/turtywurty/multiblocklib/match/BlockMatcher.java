package dev.turtywurty.multiblocklib.match;

import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockMatcher {
    boolean matches(BlockState state);
}
