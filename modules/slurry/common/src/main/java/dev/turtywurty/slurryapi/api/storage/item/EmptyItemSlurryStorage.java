package dev.turtywurty.slurryapi.api.storage.item;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.slurryapi.api.SlurryVariant;
import dev.turtywurty.turtymultiloader.transfer.lookup.MutableItemContext;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Turns an empty container item into a full one when exactly one slurry charge is inserted.
 */
public final class EmptyItemSlurryStorage implements SingleSlotStorage<ResourceVariant<Slurry>> {
    private final MutableItemContext context;
    private final Item emptyItem;
    private final BiFunction<ResourceVariant<Item>, ResourceVariant<Slurry>, ResourceVariant<Item>> emptyToFull;
    private final Slurry insertableSlurry;
    private final long insertableAmount;

    public EmptyItemSlurryStorage(
        MutableItemContext context,
        Item fullItem,
        Slurry insertableSlurry,
        long insertableAmount
    ) {
        this(
            context,
            (empty, insertedSlurry) -> ResourceVariant.of(
                ResourceTypes.ITEM,
                BuiltInRegistries.ITEM.wrapAsHolder(fullItem),
                empty.components()
            ),
            insertableSlurry,
            insertableAmount
        );
    }

    public EmptyItemSlurryStorage(
        MutableItemContext context,
        Function<ResourceVariant<Item>, ResourceVariant<Item>> emptyToFull,
        Slurry insertableSlurry,
        long insertableAmount
    ) {
        this(context, (empty, insertedSlurry) -> emptyToFull.apply(empty), insertableSlurry, insertableAmount);
    }

    /**
     * Creates an item storage whose transformation can encode the complete inserted slurry variant, including components.
     */
    public EmptyItemSlurryStorage(
        MutableItemContext context,
        BiFunction<ResourceVariant<Item>, ResourceVariant<Slurry>, ResourceVariant<Item>> emptyToFull,
        Slurry insertableSlurry,
        long insertableAmount
    ) {
        if (insertableAmount < 0)
            throw new IllegalArgumentException("insertableAmount must not be negative");
        this.context = Objects.requireNonNull(context, "context");
        this.emptyItem = context.resource().value();
        this.emptyToFull = Objects.requireNonNull(emptyToFull, "emptyToFull");
        this.insertableSlurry = Objects.requireNonNull(insertableSlurry, "insertableSlurry");
        this.insertableAmount = insertableAmount;
    }

    @Override
    public ResourceVariant<Slurry> resource(int index) {
        StoragePreconditions.index(index, 1);
        return SlurryVariant.blank();
    }

    @Override
    public long amount(int index) {
        StoragePreconditions.index(index, 1);
        return 0;
    }

    @Override
    public long capacity(int index, ResourceVariant<Slurry> resource) {
        StoragePreconditions.index(index, 1);
        return isValid(index, resource) ? this.insertableAmount : 0;
    }

    @Override
    public boolean isValid(int index, ResourceVariant<Slurry> resource) {
        StoragePreconditions.index(index, 1);
        return !resource.isBlank() && resource.value() == this.insertableSlurry;
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.INSERT_ONLY;
    }

    @Override
    public long insert(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, 1);
        if (this.context.resource().value() != this.emptyItem
            || this.insertableAmount == 0
            || maxAmount == 0
            || !isValid(index, resource)
            || maxAmount < this.insertableAmount)
            return 0;
        ResourceVariant<Item> full = this.emptyToFull.apply(this.context.resource(), resource);
        return this.context.exchange(full, 1, transaction) == 1 ? this.insertableAmount : 0;
    }

    @Override
    public long extract(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.index(index, 1);
        return 0;
    }
}
