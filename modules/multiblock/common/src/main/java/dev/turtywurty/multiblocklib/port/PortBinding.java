package dev.turtywurty.multiblocklib.port;

import dev.turtywurty.multiblocklib.data.PortIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public record PortBinding<S>(
    PortTransfer<S> transfer,
    PortIO io,
    Supplier<? extends S> storage,
    PortMatcher matcher
) {
    public PortBinding {
        Objects.requireNonNull(transfer, "transfer must not be null");
        Objects.requireNonNull(io, "io must not be null");
        Objects.requireNonNull(storage, "storage must not be null");
        Objects.requireNonNull(matcher, "matcher must not be null");
    }

    public boolean exposes(BlockPos localOffset, @Nullable Direction localSide) {
        return this.matcher.matches(localOffset, localSide);
    }
}
