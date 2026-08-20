package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.core.Holder;

/**
 * Common single-slot energy storage with independent capacity, input, and output limits.
 *
 * <p>Public transfer operations apply the per-operation limits. Trusted operations inherited from
 * {@link MutableResourceStorage} bypass those automation limits, allowing machine recipes and persistence to mutate
 * the owned energy state transactionally.</p>
 */
public class SimpleEnergyStorage extends SimpleSingleSlotStorage<ResourceVariant<UnitResource>> {
    public static final ResourceVariant<UnitResource> ENERGY =
        ResourceTypes.ENERGY.of(Holder.direct(UnitResource.VALUE));

    protected long capacity;
    protected long maxInput;
    protected long maxOutput;

    /**
     * Constructor for subclasses that override the dynamic limit accessors.
     */
    public SimpleEnergyStorage() {
        super(ResourceTypes.ENERGY);
    }

    public SimpleEnergyStorage(long capacity, long maxInput, long maxOutput) {
        this();
        this.capacity = nonNegative(capacity, "capacity");
        this.maxInput = nonNegative(maxInput, "maxInput");
        this.maxOutput = nonNegative(maxOutput, "maxOutput");
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        boolean insertion = getMaxInput() > 0;
        boolean extraction = getMaxOutput() > 0;
        if (insertion)
            return extraction ? TransferSupport.BOTH : TransferSupport.INSERT_ONLY;
        return extraction ? TransferSupport.EXTRACT_ONLY : TransferSupport.NONE;
    }

    @Override
    public long insert(
        int index,
        ResourceVariant<UnitResource> resource,
        long maxAmount,
        TransferContext transaction
    ) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, 1);
        return super.insert(index, resource, Math.min(maxAmount, getMaxInput()), transaction);
    }

    @Override
    public long extract(
        int index,
        ResourceVariant<UnitResource> resource,
        long maxAmount,
        TransferContext transaction
    ) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, 1);
        return super.extract(index, resource, Math.min(maxAmount, getMaxOutput()), transaction);
    }

    @Override
    public boolean isValid(int index, ResourceVariant<UnitResource> resource) {
        return super.isValid(index, resource) && ENERGY.equals(resource);
    }

    @Override
    protected long getCapacity(ResourceVariant<UnitResource> resource) {
        return getCapacity();
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getMaxInput() {
        return this.maxInput;
    }

    public long getMaxOutput() {
        return this.maxOutput;
    }

    public boolean setAmount(long amount, TransferContext transaction) {
        return set(0, ENERGY, amount, transaction);
    }

    public long insert(long maxAmount, TransferContext transaction) {
        return insert(ENERGY, maxAmount, transaction);
    }

    public long extract(long maxAmount, TransferContext transaction) {
        return extract(ENERGY, maxAmount, transaction);
    }

    public long insertInternal(long maxAmount, TransferContext transaction) {
        return insertInternal(ENERGY, maxAmount, transaction);
    }

    public long extractInternal(long maxAmount, TransferContext transaction) {
        return extractInternal(ENERGY, maxAmount, transaction);
    }

    private static long nonNegative(long value, String name) {
        if (value < 0)
            throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }
}
