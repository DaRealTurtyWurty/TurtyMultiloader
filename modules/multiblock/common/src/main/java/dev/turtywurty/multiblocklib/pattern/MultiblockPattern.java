package dev.turtywurty.multiblocklib.pattern;

import dev.turtywurty.multiblocklib.match.BlockMatcherList;
import net.minecraft.core.BlockPos;

import java.util.Collection;

public interface MultiblockPattern {
    BlockPos size();

    Collection<BlockPos> positions();

    BlockMatcherList matcherAt(BlockPos pos);
}
