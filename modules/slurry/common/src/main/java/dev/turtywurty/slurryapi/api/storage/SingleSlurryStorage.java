package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.Objects;

/**
 * Neutral single-slot slurry storage.
 */
public class SingleSlurryStorage extends SimpleSingleSlotStorage<ResourceVariant<Slurry>> {
    public SingleSlurryStorage(long capacity) {
        this(capacity, TransferSupport.BOTH, () -> {
        });
    }

    public SingleSlurryStorage(long capacity, TransferSupport support, Runnable onChange) {
        super(
            SlurryApi.RESOURCE_FAMILY.type(),
            capacity,
            Objects.requireNonNull(support, "support"),
            variant -> true,
            storage -> Objects.requireNonNull(onChange, "onChange").run()
        );
    }

    public static SingleSlurryStorage withFixedCapacity(long capacity, Runnable onChange) {
        return new SingleSlurryStorage(capacity, TransferSupport.BOTH, onChange);
    }
}
