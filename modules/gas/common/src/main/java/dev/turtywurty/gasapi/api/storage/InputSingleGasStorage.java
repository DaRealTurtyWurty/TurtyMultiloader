package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.function.Predicate;

public final class InputSingleGasStorage extends PredicateSingleGasStorage {
    public InputSingleGasStorage(long capacity, Predicate<ResourceVariant<Gas>> canInsert) {
        super(capacity, canInsert, ignored -> false);
    }

    public InputSingleGasStorage(long capacity) {
        this(capacity, ignored -> true);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.INSERT_ONLY;
    }
}
