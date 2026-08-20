package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;

@FunctionalInterface
public interface ItemStorageProvider<V extends ResourceVariant<?>> {
    /**
     * Finds storage for {@link MutableItemContext#stack() the stack owned by the context}.
     */
    ResourceStorage<V> find(MutableItemContext context);
}
