package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.Objects;
import java.util.function.Predicate;

public class PredicateSingleGasStorage extends SingleGasStorage {
    private final Predicate<ResourceVariant<Gas>> canInsert;
    private final Predicate<ResourceVariant<Gas>> canExtract;

    public PredicateSingleGasStorage(
        long capacity,
        Predicate<ResourceVariant<Gas>> canInsert,
        Predicate<ResourceVariant<Gas>> canExtract
    ) {
        super(capacity);
        this.canInsert = Objects.requireNonNull(canInsert, "canInsert");
        this.canExtract = Objects.requireNonNull(canExtract, "canExtract");
    }

    public PredicateSingleGasStorage(long capacity, Predicate<ResourceVariant<Gas>> isValid) {
        this(capacity, isValid, isValid);
    }

    @Override
    public long insert(int index, ResourceVariant<Gas> resource, long maxAmount, TransferContext transaction) {
        return this.canInsert.test(resource) ? super.insert(index, resource, maxAmount, transaction) : 0;
    }

    @Override
    public long extract(int index, ResourceVariant<Gas> resource, long maxAmount, TransferContext transaction) {
        return this.canExtract.test(resource) ? super.extract(index, resource, maxAmount, transaction) : 0;
    }
}
