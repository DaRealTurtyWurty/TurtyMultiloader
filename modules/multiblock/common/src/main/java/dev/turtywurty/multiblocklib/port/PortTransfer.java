package dev.turtywurty.multiblocklib.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface PortTransfer<S> {
    @Nullable S find(Level level, BlockPos pos, @Nullable Direction side);

    void move(S source, S target);
}
