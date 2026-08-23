package dev.turtywurty.multiblocklib.match;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

@FunctionalInterface
public interface BlockMatcher {
    boolean matches(BlockState state);

    /**
     * Returns a canonical state that satisfies this matcher when one is known.
     * Matchers backed by arbitrary predicates may not be constructible and
     * should retain the default empty implementation.
     */
    default Optional<BlockState> exampleState() {
        return Optional.empty();
    }
}
