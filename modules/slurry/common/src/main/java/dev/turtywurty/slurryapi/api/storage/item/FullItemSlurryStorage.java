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
import java.util.function.Function;

/**
 * Turns a full container item into an empty one when its complete slurry charge is extracted.
 */
public final class FullItemSlurryStorage implements SingleSlotStorage<ResourceVariant<Slurry>> {
    private final MutableItemContext context;
    private final Item fullItem;
    private final Function<ResourceVariant<Item>, ResourceVariant<Item>> fullToEmpty;
    private final ResourceVariant<Slurry> containedSlurry;
    private final long containedAmount;

    public FullItemSlurryStorage(
        MutableItemContext context,
        Item emptyItem,
        ResourceVariant<Slurry> containedSlurry,
        long containedAmount
    ) {
        this(
            context,
            full -> ResourceVariant.of(
                ResourceTypes.ITEM,
                BuiltInRegistries.ITEM.wrapAsHolder(emptyItem),
                full.components()
            ),
            containedSlurry,
            containedAmount
        );
    }

    public FullItemSlurryStorage(
        MutableItemContext context,
        Function<ResourceVariant<Item>, ResourceVariant<Item>> fullToEmpty,
        ResourceVariant<Slurry> containedSlurry,
        long containedAmount
    ) {
        StoragePreconditions.check(containedSlurry, containedAmount);
        this.context = Objects.requireNonNull(context, "context");
        this.fullItem = context.resource().value();
        this.fullToEmpty = Objects.requireNonNull(fullToEmpty, "fullToEmpty");
        this.containedSlurry = containedSlurry;
        this.containedAmount = containedAmount;
    }

    private boolean isFullItem() {
        return !this.context.resource().isBlank() && this.context.resource().value() == this.fullItem;
    }

    @Override
    public ResourceVariant<Slurry> resource(int index) {
        StoragePreconditions.index(index, 1);
        return isFullItem() ? this.containedSlurry : SlurryVariant.blank();
    }

    @Override
    public long amount(int index) {
        StoragePreconditions.index(index, 1);
        return isFullItem() ? this.containedAmount : 0;
    }

    @Override
    public long capacity(int index, ResourceVariant<Slurry> resource) {
        StoragePreconditions.index(index, 1);
        return isValid(index, resource) ? this.containedAmount : 0;
    }

    @Override
    public boolean isValid(int index, ResourceVariant<Slurry> resource) {
        StoragePreconditions.index(index, 1);
        return resource.equals(this.containedSlurry);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.EXTRACT_ONLY;
    }

    @Override
    public long insert(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.index(index, 1);
        return 0;
    }

    @Override
    public long extract(int index, ResourceVariant<Slurry> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, 1);
        if (!isFullItem() || this.containedAmount == 0 || maxAmount == 0
            || !resource.equals(this.containedSlurry) || maxAmount < this.containedAmount)
            return 0;
        ResourceVariant<Item> empty = this.fullToEmpty.apply(this.context.resource());
        return this.context.exchange(empty, 1, transaction) == 1 ? this.containedAmount : 0;
    }
}
