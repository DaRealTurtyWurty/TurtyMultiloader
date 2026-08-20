package dev.turtywurty.multiblocklib.port.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorageView;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.Iterator;

/**
 * Compatibility wrapper around TurtyMultiloader's neutral restricted storage view.
 */
public class RestrictedStorage<V extends ResourceVariant<?>> implements ResourceStorage<V> {
    private final ResourceStorage<V> delegate;

    public RestrictedStorage(final ResourceStorage<V> storage, final boolean allowInsert, final boolean allowExtract) {
        TransferSupport support = allowInsert
            ? allowExtract ? TransferSupport.BOTH : TransferSupport.INSERT_ONLY
            : allowExtract ? TransferSupport.EXTRACT_ONLY : TransferSupport.NONE;
        this.delegate = storage.restrictedTo(support);
    }

    @Override
    public boolean hasStableIndices() {
        return this.delegate.hasStableIndices();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public V resource(int index) {
        return this.delegate.resource(index);
    }

    @Override
    public long amount(int index) {
        return this.delegate.amount(index);
    }

    @Override
    public long capacity(int index, V resource) {
        return this.delegate.capacity(index, resource);
    }

    @Override
    public boolean isValid(int index, V resource) {
        return this.delegate.isValid(index, resource);
    }

    @Override
    public TransferSupport support(int index) {
        return this.delegate.support(index);
    }

    @Override
    public long insert(int index, V resource, long amount, TransferContext transaction) {
        return this.delegate.insert(index, resource, amount, transaction);
    }

    @Override
    public long extract(int index, V resource, long amount, TransferContext transaction) {
        return this.delegate.extract(index, resource, amount, transaction);
    }

    @Override
    public Iterator<ResourceStorageView<V>> iterator() {
        return this.delegate.iterator();
    }

    @Override
    public long version() {
        return this.delegate.version();
    }
}
