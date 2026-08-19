package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.function.Predicate;

public final class OutputSingleSlurryStorage extends PredicateSingleSlurryStorage {
    public OutputSingleSlurryStorage(long capacity, Predicate<ResourceVariant<Slurry>> canExtract) {
        super(capacity, ignored -> false, canExtract);
    }

    public OutputSingleSlurryStorage(long capacity) {
        this(capacity, ignored -> true);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.EXTRACT_ONLY;
    }
}
