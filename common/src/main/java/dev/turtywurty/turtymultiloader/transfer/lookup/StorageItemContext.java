package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Objects;

/**
 * Mutable item-container context backed by a neutral single item slot.
 */
public final class StorageItemContext implements MutableItemContext {
    private final SingleSlotStorage<ResourceVariant<Item>> slot;
    private final List<? extends ResourceStorage<ResourceVariant<Item>>> additionalSlots;
    private final ResourceStorage<ResourceVariant<Item>> overflow;

    public StorageItemContext(SingleSlotStorage<ResourceVariant<Item>> slot) {
        this(slot, List.of(), null);
    }

    public StorageItemContext(
        SingleSlotStorage<ResourceVariant<Item>> slot,
        List<? extends ResourceStorage<ResourceVariant<Item>>> additionalSlots,
        ResourceStorage<ResourceVariant<Item>> overflow
    ) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.additionalSlots = List.copyOf(additionalSlots);
        for (ResourceStorage<ResourceVariant<Item>> additionalSlot : this.additionalSlots) {
            if (additionalSlot.size() != 1)
                throw new IllegalArgumentException("Additional item context storages must have exactly one slot");
        }
        this.overflow = overflow;
    }

    public SingleSlotStorage<ResourceVariant<Item>> slot() {
        return this.slot;
    }

    @Override
    public ResourceVariant<Item> resource() {
        return this.slot.resource();
    }

    @Override
    public long amount() {
        return this.slot.amount();
    }

    @Override
    public long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.slot.insert(resource, maxAmount, transaction);
    }

    @Override
    public long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.slot.extract(resource, maxAmount, transaction);
    }

    @Override
    public long capacity(ResourceVariant<Item> resource) {
        return this.slot.capacity(resource);
    }

    @Override
    public boolean isValid(ResourceVariant<Item> resource) {
        return this.slot.isValid(0, resource);
    }

    @Override
    public TransferSupport support() {
        return this.slot.support(0);
    }

    @Override
    public List<? extends ResourceStorage<ResourceVariant<Item>>> additionalSlots() {
        return this.additionalSlots;
    }

    @Override
    public long insertOverflow(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.overflow == null ? 0 : this.overflow.insert(resource, maxAmount, transaction);
    }
}
