package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Transactional access to the item location used for an item-storage lookup.
 */
public interface MutableItemContext {
    ResourceVariant<Item> resource();

    long amount();

    long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction);

    long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction);

    /** Atomically replaces items in this location, returning the number replaced. */
    default long exchange(ResourceVariant<Item> replacement, long maxAmount, TransferContext transaction) {
        ResourceVariant<Item> current = resource();
        if (current.isBlank() || replacement.isBlank() || maxAmount <= 0)
            return 0;
        long exchanged = Math.min(amount(), maxAmount);
        try (TransferTransactionScope nested = transaction.openNested()) {
            if (extract(current, exchanged, nested) != exchanged
                || insert(replacement, exchanged, nested) != exchanged)
                return 0;
            nested.commit();
            return exchanged;
        }
    }

    /**
     * Capacity of the primary mutable item location for the supplied variant.
     */
    default long capacity(ResourceVariant<Item> resource) {
        return Math.max(amount(), resource.isBlank() ? 0 : resource.value().getDefaultMaxStackSize());
    }

    /**
     * Whether the primary mutable item location accepts the supplied variant.
     */
    default boolean isValid(ResourceVariant<Item> resource) {
        return !resource.isBlank();
    }

    /**
     * Operations potentially supported by the primary mutable item location.
     */
    default TransferSupport support() {
        return TransferSupport.BOTH;
    }

    /**
     * Additional single-slot item locations belonging to this context. Each returned storage must have exactly one
     * slot. These are separate from the primary location exposed by this interface's resource and transfer methods.
     */
    default List<? extends ResourceStorage<ResourceVariant<Item>>> additionalSlots() {
        return List.of();
    }

    /**
     * Inserts an item that could not be placed in the primary or additional locations.
     */
    default long insertOverflow(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return 0;
    }

    default ItemStack stack() {
        ResourceVariant<Item> resource = resource();
        if (resource.isBlank() || amount() == 0)
            return ItemStack.EMPTY;
        return new ItemStack(resource.holder(), Math.toIntExact(Math.min(Integer.MAX_VALUE, amount())), resource.components());
    }
}
