package dev.turtywurty.multiblocklib.pattern;

import dev.turtywurty.multiblocklib.match.BlockMatcherList;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class GridPattern implements MultiblockPattern {
    private final BlockPos size;
    private final Map<BlockPos, BlockMatcherList> matchers;

    public GridPattern(final BlockPos size, final Map<BlockPos, BlockMatcherList> matchers) {
        this.size = size;
        this.matchers = Map.copyOf(matchers);
    }

    @Override
    public BlockPos size() {
        return this.size;
    }

    @Override
    public Collection<BlockPos> positions() {
        return Set.copyOf(this.matchers.keySet());
    }

    @Override
    public BlockMatcherList matcherAt(final BlockPos pos) {
        return this.matchers.get(pos);
    }
}
