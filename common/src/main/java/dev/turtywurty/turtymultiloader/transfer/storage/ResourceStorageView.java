package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

public record ResourceStorageView<V extends ResourceVariant<?>>(ResourceStorage<V> storage, int index) {
    public ResourceStorageView {
        if (!storage.hasStableIndices())
            throw new IllegalArgumentException("Storage does not expose stable indexed slots");
        if (index < 0 || index >= storage.size())
            throw new IndexOutOfBoundsException(index);
    }

    public V resource() {
        return this.storage.resource(this.index);
    }

    public long amount() {
        return this.storage.amount(this.index);
    }

    public long capacity(V resource) {
        return this.storage.capacity(this.index, resource);
    }

    public boolean isValid(V resource) {
        return this.storage.isValid(this.index, resource);
    }

    public TransferSupport support() {
        return this.storage.support(this.index);
    }

    public long insert(V resource, long maxAmount, TransferContext transaction) {
        return this.storage.insert(this.index, resource, maxAmount, transaction);
    }

    public long extract(V resource, long maxAmount, TransferContext transaction) {
        return this.storage.extract(this.index, resource, maxAmount, transaction);
    }
}
