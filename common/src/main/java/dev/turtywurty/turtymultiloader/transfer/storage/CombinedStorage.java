package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.List;

/**
 * A live indexed view over multiple storages.
 */
public final class CombinedStorage<V extends ResourceVariant<?>> implements ResourceStorage<V> {
    private final List<? extends ResourceStorage<V>> parts;

    public CombinedStorage(List<? extends ResourceStorage<V>> parts) {
        this.parts = List.copyOf(parts);
    }

    @SafeVarargs
    public static <V extends ResourceVariant<?>> CombinedStorage<V> of(ResourceStorage<V>... parts) {
        return new CombinedStorage<>(List.of(parts));
    }

    @Override
    public boolean hasStableIndices() {
        return this.parts.stream().allMatch(ResourceStorage::hasStableIndices);
    }

    @Override
    public int size() {
        return hasStableIndices() ? this.parts.stream().mapToInt(ResourceStorage::size).sum() : 0;
    }

    @Override
    public V resource(int index) {
        Located<V> located = locate(index);
        return located.storage.resource(located.index);
    }

    @Override
    public long amount(int index) {
        Located<V> located = locate(index);
        return located.storage.amount(located.index);
    }

    @Override
    public long capacity(int index, V resource) {
        Located<V> located = locate(index);
        return located.storage.capacity(located.index, resource);
    }

    @Override
    public boolean isValid(int index, V resource) {
        Located<V> located = locate(index);
        return located.storage.isValid(located.index, resource);
    }

    @Override
    public TransferSupport support(int index) {
        Located<V> located = locate(index);
        return located.storage.support(located.index);
    }

    @Override
    public long insert(int index, V resource, long maxAmount, TransferContext transaction) {
        Located<V> located = locate(index);
        return located.storage.insert(located.index, resource, maxAmount, transaction);
    }

    @Override
    public long extract(int index, V resource, long maxAmount, TransferContext transaction) {
        Located<V> located = locate(index);
        return located.storage.extract(located.index, resource, maxAmount, transaction);
    }

    @Override
    public long insert(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long inserted = 0;
        for (ResourceStorage<V> part : this.parts) {
            if (inserted >= maxAmount)
                break;
            inserted += part.insert(resource, maxAmount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public long extract(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long extracted = 0;
        for (ResourceStorage<V> part : this.parts) {
            if (extracted >= maxAmount)
                break;
            extracted += part.extract(resource, maxAmount - extracted, transaction);
        }
        return extracted;
    }

    @Override
    public boolean supportsInsertion() {
        return this.parts.stream().anyMatch(ResourceStorage::supportsInsertion);
    }

    @Override
    public boolean supportsExtraction() {
        return this.parts.stream().anyMatch(ResourceStorage::supportsExtraction);
    }

    @Override
    public long version() {
        long result = 1;
        for (ResourceStorage<V> part : this.parts)
            result = 31 * result + part.version();
        return result;
    }

    private Located<V> locate(int requestedIndex) {
        StoragePreconditions.index(requestedIndex, size());
        int index = requestedIndex;
        for (ResourceStorage<V> part : this.parts) {
            if (index < part.size())
                return new Located<>(part, index);
            index -= part.size();
        }
        throw new IndexOutOfBoundsException(requestedIndex);
    }

    private record Located<V extends ResourceVariant<?>>(ResourceStorage<V> storage, int index) {
    }
}
