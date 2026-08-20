package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.Objects;

/**
 * Live external view that narrows the operations exposed by another storage.
 *
 * <p>The backing storage remains unrestricted for its owner. This is useful for sided machine ports: expose an
 * insert-only or extract-only view while recipes and persistence retain trusted access to the backing storage.</p>
 */
public final class RestrictedStorage<V extends ResourceVariant<?>> implements ResourceStorage<V> {
    private final ResourceStorage<V> storage;
    private final TransferSupport restriction;

    public RestrictedStorage(ResourceStorage<V> storage, TransferSupport restriction) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.restriction = Objects.requireNonNull(restriction, "restriction");
    }

    public static <V extends ResourceVariant<?>> ResourceStorage<V> insertionOnly(ResourceStorage<V> storage) {
        return new RestrictedStorage<>(storage, TransferSupport.INSERT_ONLY);
    }

    public static <V extends ResourceVariant<?>> ResourceStorage<V> extractionOnly(ResourceStorage<V> storage) {
        return new RestrictedStorage<>(storage, TransferSupport.EXTRACT_ONLY);
    }

    public ResourceStorage<V> backingStorage() {
        return this.storage;
    }

    @Override
    public boolean hasStableIndices() {
        return this.storage.hasStableIndices();
    }

    @Override
    public int size() {
        return this.storage.size();
    }

    @Override
    public V resource(int index) {
        return this.storage.resource(index);
    }

    @Override
    public long amount(int index) {
        return this.storage.amount(index);
    }

    @Override
    public long capacity(int index, V resource) {
        return this.storage.capacity(index, resource);
    }

    @Override
    public boolean isValid(int index, V resource) {
        return this.storage.isValid(index, resource);
    }

    @Override
    public TransferSupport support(int index) {
        return this.storage.support(index).and(this.restriction);
    }

    @Override
    public long insert(int index, V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, size());
        return this.restriction.supportsInsertion() ? this.storage.insert(index, resource, maxAmount, transaction) : 0;
    }

    @Override
    public long extract(int index, V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, size());
        return this.restriction.supportsExtraction() ? this.storage.extract(index, resource, maxAmount, transaction) : 0;
    }

    @Override
    public long insert(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        return this.restriction.supportsInsertion() ? this.storage.insert(resource, maxAmount, transaction) : 0;
    }

    @Override
    public long extract(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        return this.restriction.supportsExtraction() ? this.storage.extract(resource, maxAmount, transaction) : 0;
    }

    @Override
    public boolean supportsInsertion() {
        return this.restriction.supportsInsertion() && this.storage.supportsInsertion();
    }

    @Override
    public boolean supportsExtraction() {
        return this.restriction.supportsExtraction() && this.storage.supportsExtraction();
    }

    @Override
    public long version() {
        return this.storage.version();
    }
}
