package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.Objects;
import java.util.function.Predicate;

public class PredicateSingleSlurryStorage extends SingleSlurryStorage {
    private final Predicate<ResourceVariant<Slurry>> canInsert;
    private final Predicate<ResourceVariant<Slurry>> canExtract;

    public PredicateSingleSlurryStorage(
        long capacity,
        Predicate<ResourceVariant<Slurry>> canInsert,
        Predicate<ResourceVariant<Slurry>> canExtract
    ) {
        super(capacity);
        this.canInsert = Objects.requireNonNull(canInsert, "canInsert");
        this.canExtract = Objects.requireNonNull(canExtract, "canExtract");
    }

    public PredicateSingleSlurryStorage(long capacity, Predicate<ResourceVariant<Slurry>> isValid) {
        this(capacity, isValid, isValid);
    }

    @Override
    public boolean isValid(int index, ResourceVariant<Slurry> resource) {
        return super.isValid(index, resource) && (this.canInsert.test(resource) || this.canExtract.test(resource));
    }

    @Override
    public long insert(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        return this.canInsert.test(resource) ? super.insert(index, resource, maxAmount, transaction) : 0;
    }

    @Override
    public long extract(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        return this.canExtract.test(resource) ? super.extract(index, resource, maxAmount, transaction) : 0;
    }
}
