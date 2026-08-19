package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;

/**
 * A reusable block lookup cache. Close transient caches when their owner is removed.
 */
public interface BlockStorageCache<V extends ResourceVariant<?>> extends AutoCloseable {
    ResourceStorage<V> find();

    void invalidate();

    /**
     * Releases resources owned by this cache. Implementations without retained resources may simply invalidate it.
     */
    @Override
    default void close() {
        invalidate();
    }
}
