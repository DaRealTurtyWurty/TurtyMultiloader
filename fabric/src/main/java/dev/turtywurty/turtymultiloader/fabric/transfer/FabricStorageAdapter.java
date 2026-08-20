package dev.turtywurty.turtymultiloader.fabric.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Adapts Fabric Storage/StorageView to neutral resource storage and vice versa.
 */
public final class FabricStorageAdapter {
    private FabricStorageAdapter() {
    }

    public static ResourceStorage<ResourceVariant<Item>> fromFabricItems(
        Storage<ItemVariant> storage
    ) {
        return fromFabric(storage, FabricResourceAdapters::fromFabric,
            FabricResourceAdapters::toFabricItem,
            () -> ResourceTypes.ITEM.empty());
    }

    public static ResourceStorage<ResourceVariant<Fluid>> fromFabricFluids(
        Storage<FluidVariant> storage
    ) {
        return fromFabric(storage, FabricResourceAdapters::fromFabric,
            FabricResourceAdapters::toFabricFluid,
            () -> ResourceTypes.FLUID.empty());
    }

    public static <N, V extends ResourceVariant<?>> ResourceStorage<V> fromFabric(
        Storage<N> fabric,
        Function<N, V> fromNative,
        Function<V, N> toNative,
        Supplier<V> empty
    ) {
        return new FromFabric<>(fabric, fromNative, toNative, empty);
    }

    public static <N, V extends ResourceVariant<?>> Storage<N> toFabric(
        ResourceStorage<V> neutral,
        Function<N, V> fromNative,
        Function<V, N> toNative,
        Predicate<N> blank
    ) {
        return new ToFabric<>(neutral, fromNative, toNative, blank);
    }

    private static final class FromFabric<N, V extends ResourceVariant<?>> implements ResourceStorage<V> {
        private final Storage<N> fabric;
        private final Function<N, V> fromNative;
        private final Function<V, N> toNative;
        private final Supplier<V> empty;

        private FromFabric(Storage<N> fabric, Function<N, V> fromNative,
                           Function<V, N> toNative, Supplier<V> empty) {
            this.fabric = Objects.requireNonNull(fabric, "fabric");
            this.fromNative = fromNative;
            this.toNative = toNative;
            this.empty = empty;
        }

        @Override
        public boolean hasStableIndices() {
            return this.fabric instanceof SingleSlotStorage<?> || this.fabric instanceof SlottedStorage<?>;
        }

        @Override
        public int size() {
            if (this.fabric instanceof SingleSlotStorage<?>)
                return 1;
            if (this.fabric instanceof SlottedStorage<?> slotted)
                return slotted.getSlotCount();
            return 0;
        }

        @Override
        public V resource(int index) {
            var view = viewAt(index);
            return view == null || view.isResourceBlank() ? this.empty.get() : this.fromNative.apply(view.getResource());
        }

        @Override
        public long amount(int index) {
            var view = viewAt(index);
            return view == null ? 0 : view.getAmount();
        }

        @Override
        @SuppressWarnings("deprecation")
        public long capacity(int index, V resource) {
            var view = viewAt(index);
            if (resource.isBlank())
                return 0;
            N nativeResource = this.toNative.apply(resource);
            boolean resourceMatches = view.isResourceBlank() || Objects.equals(view.getResource(), nativeResource);
            if (!resourceMatches)
                return 0;

            long declaredCapacity = view.getCapacity();
            if (!(view instanceof SingleSlotStorage<N> slot) || !slot.supportsInsertion())
                return declaredCapacity;

            long currentAmount = view.getAmount();
            try (Transaction simulation = Transaction.openNested(Transaction.getCurrentUnsafe())) {
                long insertable = slot.insert(nativeResource, Long.MAX_VALUE, simulation);
                long measuredCapacity = insertable > Long.MAX_VALUE - currentAmount
                    ? Long.MAX_VALUE
                    : currentAmount + insertable;
                return Math.max(declaredCapacity, measuredCapacity);
            }
        }

        @Override
        public boolean isValid(int index, V resource) {
            StoragePreconditions.index(index, size());
            return !resource.isBlank();
        }

        @Override
        public TransferSupport support(int index) {
            StoragePreconditions.index(index, size());
            // A plain Fabric Storage only supports insertion into the storage as a whole. Its iterated views are not
            // addressable insertion targets, so indexed insertion must not claim to support them.
            boolean insert = (this.fabric instanceof SlottedStorage<?>
                || this.fabric instanceof SingleSlotStorage<?>) && this.fabric.supportsInsertion();
            boolean extract = this.fabric.supportsExtraction();
            return insert ? extract ? TransferSupport.BOTH : TransferSupport.INSERT_ONLY
                : extract ? TransferSupport.EXTRACT_ONLY : TransferSupport.NONE;
        }

        @Override
        public long insert(int index, V resource, long maxAmount,
                           TransferContext transaction) {
            StoragePreconditions.index(index, size());
            if (this.fabric instanceof SingleSlotStorage<N> single)
                return single.insert(this.toNative.apply(resource), maxAmount,
                    FabricTransactionAdapters.toFabric(transaction));
            if (this.fabric instanceof SlottedStorage<N> slotted)
                return slotted.getSlot(index).insert(this.toNative.apply(resource), maxAmount,
                    FabricTransactionAdapters.toFabric(transaction));
            return 0;
        }

        @Override
        public long extract(int index, V resource, long maxAmount,
                            TransferContext transaction) {
            var view = viewAt(index);
            return view == null ? 0 : view.extract(this.toNative.apply(resource), maxAmount,
                FabricTransactionAdapters.toFabric(transaction));
        }

        @Override
        public long version() {
            return this.fabric.getVersion();
        }

        @Override
        public long insert(V resource, long maxAmount, TransferContext transaction) {
            StoragePreconditions.check(resource, maxAmount);
            return this.fabric.insert(this.toNative.apply(resource), maxAmount,
                FabricTransactionAdapters.toFabric(transaction));
        }

        @Override
        public long extract(V resource, long maxAmount, TransferContext transaction) {
            StoragePreconditions.check(resource, maxAmount);
            return this.fabric.extract(this.toNative.apply(resource), maxAmount,
                FabricTransactionAdapters.toFabric(transaction));
        }

        @Override
        public boolean supportsInsertion() {
            return this.fabric.supportsInsertion();
        }

        @Override
        public boolean supportsExtraction() {
            return this.fabric.supportsExtraction();
        }

        private StorageView<N> viewAt(int index) {
            StoragePreconditions.index(index, size());
            if (this.fabric instanceof SingleSlotStorage<N> single)
                return single;
            if (this.fabric instanceof SlottedStorage<N> slotted)
                return slotted.getSlot(index);
            throw new IllegalStateException("Storage reported stable indices without addressable slots");
        }
    }

    private static final class ToFabric<N, V extends ResourceVariant<?>>
        implements SlottedStorage<N> {
        private final ResourceStorage<V> neutral;
        private final Function<N, V> fromNative;
        private final Function<V, N> toNative;
        private final Predicate<N> blank;

        private ToFabric(ResourceStorage<V> neutral, Function<N, V> fromNative, Function<V, N> toNative,
                         Predicate<N> blank) {
            this.neutral = neutral;
            this.fromNative = fromNative;
            this.toNative = toNative;
            this.blank = blank;
        }

        @Override
        public boolean supportsInsertion() {
            return this.neutral.supportsInsertion();
        }

        @Override
        public long insert(N resource, long maxAmount,
                           TransactionContext transaction) {
            if (this.blank.test(resource) || maxAmount == 0)
                return 0;
            return this.neutral.insert(this.fromNative.apply(resource), maxAmount,
                FabricTransactionAdapters.fromFabric(transaction));
        }

        @Override
        public boolean supportsExtraction() {
            return this.neutral.supportsExtraction();
        }

        @Override
        public long extract(N resource, long maxAmount,
                            TransactionContext transaction) {
            if (this.blank.test(resource) || maxAmount == 0)
                return 0;
            return this.neutral.extract(this.fromNative.apply(resource), maxAmount,
                FabricTransactionAdapters.fromFabric(transaction));
        }

        @Override
        public int getSlotCount() {
            return this.neutral.size();
        }

        @Override
        public SingleSlotStorage<N> getSlot(int index) {
            StoragePreconditions.index(index, this.neutral.size());
            return new SingleSlotStorage<>() {
                @Override
                public boolean supportsInsertion() {
                    return neutral.support(index).supportsInsertion();
                }

                @Override
                public long insert(N resource, long maxAmount, TransactionContext transaction) {
                    if (blank.test(resource) || maxAmount == 0)
                        return 0;
                    return neutral.insert(index, fromNative.apply(resource), maxAmount,
                        FabricTransactionAdapters.fromFabric(transaction));
                }

                @Override
                public boolean supportsExtraction() {
                    return neutral.support(index).supportsExtraction();
                }

                @Override
                public long extract(N resource, long maxAmount, TransactionContext transaction) {
                    if (blank.test(resource) || maxAmount == 0)
                        return 0;
                    return neutral.extract(index, fromNative.apply(resource), maxAmount,
                        FabricTransactionAdapters.fromFabric(transaction));
                }

                @Override
                public boolean isResourceBlank() {
                    return neutral.resource(index).isBlank();
                }

                @Override
                public N getResource() {
                    return toNative.apply(neutral.resource(index));
                }

                @Override
                public long getAmount() {
                    return neutral.amount(index);
                }

                @Override
                public long getCapacity() {
                    return neutral.capacity(index, neutral.resource(index));
                }
            };
        }

        @Override
        public Iterator<StorageView<N>> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return this.index < neutral.size();
                }

                @Override
                public StorageView<N> next() {
                    return getSlot(this.index++);
                }
            };
        }

        @Override
        public long getVersion() {
            return this.neutral.version();
        }
    }
}
