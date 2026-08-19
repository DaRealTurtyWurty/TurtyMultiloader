package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SimpleSingleSlotStorage<V extends ResourceVariant<?>> extends SimpleStorage<V>
    implements SingleSlotStorage<V> {
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
}
