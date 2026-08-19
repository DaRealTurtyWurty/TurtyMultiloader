package dev.turtywurty.turtymultiloader.neoforge.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Function;

/**
 * ResourceHandler adapter with explicit native and neutral amount units.
 */
public final class NeoForgeStorageAdapter {
    private NeoForgeStorageAdapter() {
    }

    public static ResourceStorage<ResourceVariant<Item>> fromNeoForgeItems(
        ResourceHandler<ItemResource> handler
    ) {
        return fromNeoForge(handler, NeoForgeResourceAdapters::fromNeoForge,
            NeoForgeResourceAdapters::toNeoForgeItem, Units.ITEM, Units.ITEM);
    }

    public static ResourceStorage<ResourceVariant<Fluid>> fromNeoForgeFluids(
        ResourceHandler<FluidResource> handler
    ) {
        return fromNeoForgeFluids(handler, Units.FLUID_DROPLET);
    }

    public static ResourceStorage<ResourceVariant<Fluid>> fromNeoForgeFluids(
        ResourceHandler<FluidResource> handler,
        TransferUnit neutralUnit
    ) {
        return fromNeoForge(handler, NeoForgeResourceAdapters::fromNeoForge,
            NeoForgeResourceAdapters::toNeoForgeFluid, neutralUnit, Units.FLUID_MILLIBUCKET);
    }

    public static <N extends Resource, V extends ResourceVariant<?>> ResourceStorage<V> fromNeoForge(
        ResourceHandler<N> handler,
        Function<N, V> fromNative,
        Function<V, N> toNative,
        TransferUnit neutralUnit,
        TransferUnit nativeUnit
    ) {
        return new FromNeoForge<>(handler, fromNative, toNative, neutralUnit, nativeUnit);
    }

    public static <N extends Resource, V extends ResourceVariant<?>> ResourceHandler<N> toNeoForge(
        ResourceStorage<V> storage,
        Function<N, V> fromNative,
        Function<V, N> toNative,
        TransferUnit neutralUnit,
        TransferUnit nativeUnit
    ) {
        return new ToNeoForge<>(storage, fromNative, toNative, neutralUnit, nativeUnit);
    }

    private static long convert(long amount, TransferUnit source, TransferUnit target) {
        try {
            return source.convert(amount, target, RoundingMode.DOWN);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static final class FromNeoForge<N extends Resource, V extends ResourceVariant<?>> implements ResourceStorage<V> {
        private final ResourceHandler<N> handler;
        private final Function<N, V> fromNative;
        private final Function<V, N> toNative;
        private final TransferUnit neutralUnit;
        private final TransferUnit nativeUnit;

        private FromNeoForge(ResourceHandler<N> handler, Function<N, V> fromNative, Function<V, N> toNative,
                             TransferUnit neutralUnit, TransferUnit nativeUnit) {
            this.handler = Objects.requireNonNull(handler, "handler");
            this.fromNative = Objects.requireNonNull(fromNative, "fromNative");
            this.toNative = Objects.requireNonNull(toNative, "toNative");
            this.neutralUnit = Objects.requireNonNull(neutralUnit, "neutralUnit");
            this.nativeUnit = Objects.requireNonNull(nativeUnit, "nativeUnit");
            validateDimensions(neutralUnit, nativeUnit);
        }

        @Override
        public int size() {
            return this.handler.size();
        }

        @Override
        public V resource(int index) {
            return this.fromNative.apply(this.handler.getResource(index));
        }

        @Override
        public long amount(int index) {
            return convert(this.handler.getAmountAsLong(index), this.nativeUnit, this.neutralUnit);
        }

        @Override
        public long capacity(int index, V resource) {
            return convert(this.handler.getCapacityAsLong(index, this.toNative.apply(resource)),
                this.nativeUnit, this.neutralUnit);
        }

        @Override
        public boolean isValid(int index, V resource) {
            return this.handler.isValid(index, this.toNative.apply(resource));
        }

        @Override
        public TransferSupport support(int index) {
            StoragePreconditions.index(index, size());
            return TransferSupport.BOTH;
        }

        @Override
        public long insert(int index, V resource, long maxAmount, TransferContext transaction) {
            StoragePreconditions.check(resource, maxAmount);
            long nativeRemaining = convert(maxAmount, this.neutralUnit, this.nativeUnit);
            long inserted = 0;
            N nativeResource = this.toNative.apply(resource);
            var nativeTransaction = NeoForgeTransactionAdapters.toNeoForge(transaction);
            while (nativeRemaining > 0) {
                int request = (int) Math.min(Integer.MAX_VALUE, nativeRemaining);
                int moved = this.handler.insert(index, nativeResource, request, nativeTransaction);
                inserted += moved;
                nativeRemaining -= moved;
                if (moved < request)
                    break;
            }
            return convert(inserted, this.nativeUnit, this.neutralUnit);
        }

        @Override
        public long extract(int index, V resource, long maxAmount, TransferContext transaction) {
            StoragePreconditions.check(resource, maxAmount);
            long nativeRemaining = convert(maxAmount, this.neutralUnit, this.nativeUnit);
            long extracted = 0;
            N nativeResource = this.toNative.apply(resource);
            var nativeTransaction = NeoForgeTransactionAdapters.toNeoForge(transaction);
            while (nativeRemaining > 0) {
                int request = (int) Math.min(Integer.MAX_VALUE, nativeRemaining);
                int moved = this.handler.extract(index, nativeResource, request, nativeTransaction);
                extracted += moved;
                nativeRemaining -= moved;
                if (moved < request)
                    break;
            }
            return convert(extracted, this.nativeUnit, this.neutralUnit);
        }
    }

    private static final class ToNeoForge<N extends Resource, V extends ResourceVariant<?>> implements ResourceHandler<N> {
        private final ResourceStorage<V> storage;
        private final Function<N, V> fromNative;
        private final Function<V, N> toNative;
        private final TransferUnit neutralUnit;
        private final TransferUnit nativeUnit;

        private ToNeoForge(ResourceStorage<V> storage, Function<N, V> fromNative, Function<V, N> toNative,
                           TransferUnit neutralUnit, TransferUnit nativeUnit) {
            this.storage = Objects.requireNonNull(storage, "storage");
            this.fromNative = Objects.requireNonNull(fromNative, "fromNative");
            this.toNative = Objects.requireNonNull(toNative, "toNative");
            this.neutralUnit = Objects.requireNonNull(neutralUnit, "neutralUnit");
            this.nativeUnit = Objects.requireNonNull(nativeUnit, "nativeUnit");
            validateDimensions(neutralUnit, nativeUnit);
        }

        @Override
        public int size() {
            return this.storage.size();
        }

        @Override
        public N getResource(int index) {
            return this.toNative.apply(this.storage.resource(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return convert(this.storage.amount(index), this.neutralUnit, this.nativeUnit);
        }

        @Override
        public long getCapacityAsLong(int index, N resource) {
            return convert(this.storage.capacity(index, this.fromNative.apply(resource)),
                this.neutralUnit, this.nativeUnit);
        }

        @Override
        public boolean isValid(int index, N resource) {
            return this.storage.isValid(index, this.fromNative.apply(resource));
        }

        @Override
        public int insert(int index, N resource, int amount,
                          TransactionContext transaction) {
            V neutralResource = this.fromNative.apply(resource);
            TransferContext neutralTransaction = NeoForgeTransactionAdapters.fromNeoForge(transaction);
            long neutralRequest = convert(amount, this.nativeUnit, this.neutralUnit);
            long simulated = neutralTransaction.simulate(
                nested -> this.storage.insert(index, neutralResource, neutralRequest, nested)
            );
            return insertRepresentable(index, neutralResource, simulated, neutralTransaction);
        }

        @Override
        public int extract(int index, N resource, int amount,
                           TransactionContext transaction) {
            V neutralResource = this.fromNative.apply(resource);
            TransferContext neutralTransaction = NeoForgeTransactionAdapters.fromNeoForge(transaction);
            long neutralRequest = convert(amount, this.nativeUnit, this.neutralUnit);
            long simulated = neutralTransaction.simulate(
                nested -> this.storage.extract(index, neutralResource, neutralRequest, nested)
            );
            return extractRepresentable(index, neutralResource, simulated, neutralTransaction);
        }

        private int insertRepresentable(int index, V resource, long simulated, TransferContext transaction) {
            int nativeAmount = Math.toIntExact(convert(simulated, this.neutralUnit, this.nativeUnit));
            if (nativeAmount == 0)
                return 0;
            long exactNeutralAmount = convert(nativeAmount, this.nativeUnit, this.neutralUnit);
            try (TransferTransactionScope nested = transaction.openNested()) {
                long moved = this.storage.insert(index, resource, exactNeutralAmount, nested);
                if (moved != exactNeutralAmount)
                    return 0;
                nested.commit();
                return nativeAmount;
            }
        }

        private int extractRepresentable(int index, V resource, long simulated, TransferContext transaction) {
            int nativeAmount = Math.toIntExact(convert(simulated, this.neutralUnit, this.nativeUnit));
            if (nativeAmount == 0)
                return 0;
            long exactNeutralAmount = convert(nativeAmount, this.nativeUnit, this.neutralUnit);
            try (TransferTransactionScope nested = transaction.openNested()) {
                long moved = this.storage.extract(index, resource, exactNeutralAmount, nested);
                if (moved != exactNeutralAmount)
                    return 0;
                nested.commit();
                return nativeAmount;
            }
        }
    }

    private static void validateDimensions(TransferUnit neutralUnit, TransferUnit nativeUnit) {
        if (!neutralUnit.dimension().equals(nativeUnit.dimension()))
            throw new IllegalArgumentException("Cannot bridge " + nativeUnit.dimension() + " to "
                + neutralUnit.dimension());
    }
}
