package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.function.Predicate;

public final class OutputSingleGasStorage extends PredicateSingleGasStorage {
    public OutputSingleGasStorage(long capacity, Predicate<ResourceVariant<Gas>> canExtract) {
        super(capacity, ignored -> false, canExtract);
    }

    public OutputSingleGasStorage(long capacity) {
        this(capacity, ignored -> true);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.EXTRACT_ONLY;
    }
}
