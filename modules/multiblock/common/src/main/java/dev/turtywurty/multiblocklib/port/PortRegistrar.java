package dev.turtywurty.multiblocklib.port;

import dev.turtywurty.multiblocklib.data.PortIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class PortRegistrar {
    private final List<PortBinding<?>> bindings = new ArrayList<>();

    public <S> Builder<S> input(PortTransfer<S> transfer, Supplier<? extends S> storage) {
        return new Builder<>(this, transfer, PortIO.INPUT, storage);
    }

    public <S> Builder<S> output(PortTransfer<S> transfer, Supplier<? extends S> storage) {
        return new Builder<>(this, transfer, PortIO.OUTPUT, storage);
    }

    public <S> Builder<S> both(PortTransfer<S> transfer, Supplier<? extends S> storage) {
        return new Builder<>(this, transfer, PortIO.BOTH, storage);
    }

    public List<PortBinding<?>> bindings() {
        return List.copyOf(this.bindings);
    }

    public <S> @Nullable S find(
        PortTransfer<S> transfer,
        BlockPos localOffset,
        @Nullable Direction localSide
    ) {
        for (PortBinding<?> untypedBinding : this.bindings) {
            if (untypedBinding.transfer() != transfer || !untypedBinding.exposes(localOffset, localSide)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            PortBinding<S> binding = (PortBinding<S>) untypedBinding;
            return binding.storage().get();
        }

        return null;
    }

    private <S> PortBinding<S> add(
        PortTransfer<S> transfer,
        PortIO io,
        Supplier<? extends S> storage,
        PortMatcher matcher
    ) {
        PortBinding<S> binding = new PortBinding<>(transfer, io, storage, matcher);
        this.bindings.add(binding);
        return binding;
    }

    public static final class Builder<S> {
        private final PortRegistrar registrar;
        private final PortTransfer<S> transfer;
        private final PortIO io;
        private final Supplier<? extends S> storage;

        private Builder(
            PortRegistrar registrar,
            PortTransfer<S> transfer,
            PortIO io,
            Supplier<? extends S> storage
        ) {
            this.registrar = registrar;
            this.transfer = transfer;
            this.io = io;
            this.storage = storage;
        }

        public PortBinding<S> where(PortMatcher matcher) {
            return this.registrar.add(this.transfer, this.io, this.storage, matcher);
        }

        public PortBinding<S> wherePosition(Predicate<BlockPos> matcher) {
            return where((localOffset, localSide) -> matcher.test(localOffset));
        }

        public PortBinding<S> at(BlockPos... localOffsets) {
            Set<BlockPos> offsets = Set.copyOf(Arrays.asList(localOffsets));
            return wherePosition(offsets::contains);
        }

        public PortBinding<S> atSides(BlockPos localOffset, Direction... localSides) {
            Set<Direction> sides = localSides.length == 0
                ? EnumSet.allOf(Direction.class)
                : EnumSet.copyOf(Arrays.asList(localSides));
            return where((offset, side) -> localOffset.equals(offset) && (side == null || sides.contains(side)));
        }
    }
}
