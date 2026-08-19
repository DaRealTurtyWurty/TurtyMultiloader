package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.Objects;

/**
 * Neutral single-slot gas storage.
 */
public class SingleGasStorage extends SimpleSingleSlotStorage<ResourceVariant<Gas>> {
    public SingleGasStorage(long capacity) {
        this(capacity, TransferSupport.BOTH, () -> {
        });
    }

    public SingleGasStorage(long capacity, TransferSupport support, Runnable onChange) {
        super(
            GasApi.RESOURCE_FAMILY.type(),
            capacity,
            Objects.requireNonNull(support, "support"),
            variant -> true,
            storage -> Objects.requireNonNull(onChange, "onChange").run()
        );
    }

    public static SingleGasStorage withFixedCapacity(long capacity, Runnable onChange) {
        return new SingleGasStorage(capacity, TransferSupport.BOTH, onChange);
    }
}
