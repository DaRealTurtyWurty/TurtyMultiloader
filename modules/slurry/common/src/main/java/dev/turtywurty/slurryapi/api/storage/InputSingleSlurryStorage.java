package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

import java.util.function.Predicate;

public final class InputSingleSlurryStorage extends PredicateSingleSlurryStorage {
    public InputSingleSlurryStorage(long capacity, Predicate<ResourceVariant<Slurry>> canInsert) {
        super(capacity, canInsert, ignored -> false);
    }

    public InputSingleSlurryStorage(long capacity) {
        this(capacity, ignored -> true);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.INSERT_ONLY;
    }
}
