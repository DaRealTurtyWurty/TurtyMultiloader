package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.StorageTransfer;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.Iterator;

/**
 * Indexed, transactional storage. All amounts use the unit declared by its StorageKey.
 */
public interface ResourceStorage<V extends ResourceVariant<?>> extends Iterable<ResourceStorageView<V>> {
    int size();

    V resource(int index);

    long amount(int index);

    long capacity(int index, V resource);

    boolean isValid(int index, V resource);

    TransferSupport support(int index);

    long insert(int index, V resource, long maxAmount, TransferContext transaction);

    long extract(int index, V resource, long maxAmount, TransferContext transaction);

    default long insert(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long inserted = 0;
        for (int index = 0; index < size() && inserted < maxAmount; index++)
            inserted += insert(index, resource, maxAmount - inserted, transaction);
        return inserted;
    }

    default long extract(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long extracted = 0;
        for (int index = 0; index < size() && extracted < maxAmount; index++)
            extracted += extract(index, resource, maxAmount - extracted, transaction);
        return extracted;
    }

    default boolean supportsInsertion() {
        for (int index = 0; index < size(); index++)
            if (support(index).supportsInsertion())
                return true;
        return false;
    }

    default boolean supportsExtraction() {
        for (int index = 0; index < size(); index++)
            if (support(index).supportsExtraction())
                return true;
        return false;
    }

    default ResourceStorageView<V> view(int index) {
        return new ResourceStorageView<>(this, index);
    }

    @Override
    default Iterator<ResourceStorageView<V>> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return this.index < size();
            }

            @Override
            public ResourceStorageView<V> next() {
                return view(this.index++);
            }
        };
    }

    default long version() {
        return 0;
    }

    /**
     * Atomically moves as much of a variant as the target accepts.
     */
    default long moveTo(ResourceStorage<V> target, V resource, long maxAmount) {
        return StorageTransfer.move(this, target, resource, maxAmount);
    }

    /**
     * Atomically moves as much of a variant as the target accepts within the supplied transaction.
     */
    default long moveTo(ResourceStorage<V> target, V resource, long maxAmount, TransferContext transaction) {
        return StorageTransfer.move(this, target, resource, maxAmount, transaction);
    }
}
