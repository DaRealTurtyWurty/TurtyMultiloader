package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Transactional access to the item location used for an item-storage lookup.
 */
public interface MutableItemContext {
    static MutableItemContext withConstant(ItemStack stack) {
        return new ConstantItemContext(stack);
    }

    static MutableItemContext ofSingleSlot(SingleSlotStorage<ResourceVariant<Item>> slot) {
        return new StorageItemContext(slot);
    }

    static MutableItemContext ofContainerSlot(Container container, int slot) {
        Objects.requireNonNull(container, "container");
        if (slot < 0 || slot >= container.getContainerSize())
            throw new IndexOutOfBoundsException(slot);
        return new StackReferenceItemContext(
            () -> container.getItem(slot),
            stack -> container.setItem(slot, stack),
            stack -> container.canPlaceItem(slot, stack),
            container::getMaxStackSize,
            container::setChanged
        );
    }

    static MutableItemContext ofInventorySlot(Inventory inventory, int slot) {
        return ofContainerSlot(inventory, slot);
    }

    static MutableItemContext ofPlayerHand(Player player, InteractionHand hand) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        return new StackReferenceItemContext(
            () -> player.getItemInHand(hand),
            stack -> player.setItemInHand(hand, stack),
            ignored -> true,
            ItemStack::getMaxStackSize,
            () -> player.getInventory().setChanged()
        );
    }

    static MutableItemContext ofCursor(AbstractContainerMenu menu) {
        Objects.requireNonNull(menu, "menu");
        return new StackReferenceItemContext(
            menu::getCarried,
            menu::setCarried,
            ignored -> true,
            ItemStack::getMaxStackSize,
            () -> {
            }
        );
    }

    static MutableItemContext ofPlayerCursor(Player player) {
        Objects.requireNonNull(player, "player");
        return ofCursor(player.containerMenu);
    }

    static MutableItemContext ofPlayerCursor(Player player, AbstractContainerMenu menu) {
        Objects.requireNonNull(player, "player");
        return ofCursor(menu);
    }

    /**
     * The current stack in the primary item location. Item lookup implementations must use this value rather than
     * accepting a separate stack argument.
     */
    ItemStack stack();

    default <V extends ResourceVariant<?>> ResourceStorage<V> find(StorageKey<V> key) {
        Objects.requireNonNull(key, "key");
        return stack().isEmpty() ? null : TransferService.get().findItem(key, this);
    }

    ResourceVariant<Item> resource();

    long amount();

    long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction);

    long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction);

    /**
     * Atomically replaces items in this location, returning the number replaced.
     */
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

}
