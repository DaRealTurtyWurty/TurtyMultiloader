package dev.turtywurty.gasapi.api.storage.item;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.gasapi.api.GasVariant;
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
 * Turns a full container item into an empty one when its complete gas charge is extracted.
 */
public final class FullItemGasStorage implements SingleSlotStorage<ResourceVariant<Gas>> {
    private final MutableItemContext context;
    private final Item fullItem;
    private final Function<ResourceVariant<Item>, ResourceVariant<Item>> fullToEmpty;
    private final ResourceVariant<Gas> containedGas;
    private final long containedAmount;

    public FullItemGasStorage(
        MutableItemContext context,
        Item emptyItem,
        ResourceVariant<Gas> containedGas,
        long containedAmount
    ) {
        this(
            context,
            full -> ResourceVariant.of(
                ResourceTypes.ITEM,
                BuiltInRegistries.ITEM.wrapAsHolder(emptyItem),
                full.components()
            ),
            containedGas,
            containedAmount
        );
    }

    public FullItemGasStorage(
        MutableItemContext context,
        Function<ResourceVariant<Item>, ResourceVariant<Item>> fullToEmpty,
        ResourceVariant<Gas> containedGas,
        long containedAmount
    ) {
        StoragePreconditions.check(containedGas, containedAmount);
        this.context = Objects.requireNonNull(context, "context");
        this.fullItem = context.resource().value();
        this.fullToEmpty = Objects.requireNonNull(fullToEmpty, "fullToEmpty");
        this.containedGas = containedGas;
        this.containedAmount = containedAmount;
    }

    private boolean isFullItem() {
        return !this.context.resource().isBlank() && this.context.resource().value() == this.fullItem;
    }

    @Override
    public ResourceVariant<Gas> resource(int index) {
        StoragePreconditions.index(index, 1);
        return isFullItem() ? this.containedGas : GasVariant.blank();
    }

    @Override
    public long amount(int index) {
        StoragePreconditions.index(index, 1);
        return isFullItem() ? this.containedAmount : 0;
    }

    @Override
    public long capacity(int index, ResourceVariant<Gas> resource) {
        StoragePreconditions.index(index, 1);
        return isValid(index, resource) ? this.containedAmount : 0;
    }

    @Override
    public boolean isValid(int index, ResourceVariant<Gas> resource) {
        StoragePreconditions.index(index, 1);
        return resource.equals(this.containedGas);
    }

    @Override
    public TransferSupport support(int index) {
        StoragePreconditions.index(index, 1);
        return TransferSupport.EXTRACT_ONLY;
    }

    @Override
    public long insert(int index, ResourceVariant<Gas> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.index(index, 1);
        return 0;
    }

    @Override
    public long extract(int index, ResourceVariant<Gas> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        StoragePreconditions.index(index, 1);
        if (!isFullItem() || !resource.equals(this.containedGas) || maxAmount < this.containedAmount)
            return 0;
        ResourceVariant<Item> empty = this.fullToEmpty.apply(this.context.resource());
        return this.context.exchange(empty, 1, transaction) == 1 ? this.containedAmount : 0;
    }
}
