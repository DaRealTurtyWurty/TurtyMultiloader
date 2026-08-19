package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ItemStorageProvider<V extends ResourceVariant<?>> {
    ResourceStorage<V> find(ItemStack stack, MutableItemContext context);
}
