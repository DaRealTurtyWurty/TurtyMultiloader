package dev.turtywurty.multiblocklib.port.storage;

import dev.turtywurty.multiblocklib.data.PortIO;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MultiblockItemPortStorage extends SimpleSingleSlotStorage<ResourceVariant<Item>> {
    public MultiblockItemPortStorage(final int capacity, final PortIO io, final Runnable onChange) {
        super(ResourceTypes.ITEM, capacity, TransferSupport.BOTH, variant -> true, storage -> onChange.run());
    }

    public void loadStack(final ItemStack stack) {
        ItemStack loaded = stack == null ? ItemStack.EMPTY : stack;
        ResourceVariant<Item> resource = ResourceVariant.ofItem(loaded);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            set(resource, Math.min(loaded.getCount(), capacity(0, resource)), transaction);
            transaction.commit();
        }
    }

    public ItemStack getStoredStack() {
        if (getAmount() == 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(getResource().holder(), Math.toIntExact(getAmount()), getResource().components());
    }
}
