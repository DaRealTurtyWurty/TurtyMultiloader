package dev.turtywurty.multiblocklib.match;

import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record BlockMatcherList(List<BlockMatcher> matchers, List<String> debugTokens) implements BlockMatcher {
    public BlockMatcherList(final List<BlockMatcher> matchers) {
        this(matchers, List.of());
    }

    public BlockMatcherList(final List<BlockMatcher> matchers, final List<String> debugTokens) {
        this.matchers = List.copyOf(matchers);
        this.debugTokens = List.copyOf(debugTokens);
    }

    @Override
    public boolean matches(final BlockState state) {
        Objects.requireNonNull(state, "state");
        for (BlockMatcher matcher : this.matchers) {
            if (matcher.matches(state)) {
                return true;
            }
        }

        return false;
    }

    public String describe() {
        if (this.debugTokens.isEmpty()) {
            return "<pattern matcher>";
        }

        return this.debugTokens.stream().collect(Collectors.joining(" | "));
    }
}
