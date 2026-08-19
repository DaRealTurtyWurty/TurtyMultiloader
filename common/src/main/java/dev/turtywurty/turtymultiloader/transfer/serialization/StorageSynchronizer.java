package dev.turtywurty.turtymultiloader.transfer.serialization;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Emits snapshots only when storage contents change and applies received snapshots transactionally.
 */
public final class StorageSynchronizer<V extends ResourceVariant<?>> {
    private final ResourceStorage<V> storage;
    private StorageSnapshot<V> lastSnapshot;

    public StorageSynchronizer(ResourceStorage<V> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public boolean sendIfChanged(Consumer<StorageSnapshot<V>> sender) {
        Objects.requireNonNull(sender, "sender");
        StorageSnapshot<V> snapshot = StorageSnapshot.capture(this.storage);
        if (snapshot.equals(this.lastSnapshot))
            return false;
        sender.accept(snapshot);
        this.lastSnapshot = snapshot;
        return true;
    }

    public boolean receive(StorageSnapshot<V> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean applied = snapshot.apply(this.storage);
        if (applied)
            this.lastSnapshot = StorageSnapshot.capture(this.storage);
        return applied;
    }
}
