package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SimpleSingleSlotStorage<V extends ResourceVariant<?>> extends SimpleStorage<V>
    implements SingleSlotStorage<V> {
    public SimpleSingleSlotStorage(ResourceType<?> resourceType) {
        super(resourceType, 1);
    }

    public SimpleSingleSlotStorage(ResourceType<?> resourceType, long capacity) {
        super(resourceType, 1, capacity);
    }

    public SimpleSingleSlotStorage(
        ResourceType<?> resourceType,
        long capacity,
        TransferSupport support,
        Predicate<V> validity,
        Consumer<SimpleStorage<V>> changeListener
    ) {
        super(resourceType, new long[]{capacity}, new TransferSupport[]{support},
            (index, resource) -> validity.test(resource), changeListener);
    }

    @Override
    protected long getCapacity(int index, V resource) {
        StoragePreconditions.index(index, 1);
        return getCapacity(resource);
    }

    /**
     * Single-slot dynamic capacity hook.
     */
    protected long getCapacity(V resource) {
        return super.getCapacity(0, resource);
    }

    public V getResource() {
        return resource();
    }

    public long getAmount() {
        return amount();
    }

    public boolean set(V resource, long amount, TransferContext transaction) {
        return set(0, resource, amount, transaction);
    }

    protected final boolean setStored(V resource, long amount, TransferContext transaction) {
        return set(resource, amount, transaction);
    }
}
