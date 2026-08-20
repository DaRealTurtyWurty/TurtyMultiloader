package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Read-only item context for looking up storage in a detached stack.
 */
final class ConstantItemContext implements MutableItemContext {
    private final ItemStack stack;
    private final ResourceVariant<Item> resource;

    ConstantItemContext(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack").copy();
        this.resource = ResourceVariant.ofItem(this.stack);
    }

    @Override
    public ItemStack stack() {
        return this.stack.copy();
    }

    @Override
    public ResourceVariant<Item> resource() {
        return this.resource;
    }

    @Override
    public long amount() {
        return this.stack.getCount();
    }

    @Override
    public long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return 0;
    }

    @Override
    public long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return 0;
    }

    @Override
    public long capacity(ResourceVariant<Item> resource) {
        return this.resource.equals(resource) ? amount() : 0;
    }

    @Override
    public boolean isValid(ResourceVariant<Item> resource) {
        return false;
    }

    @Override
    public TransferSupport support() {
        return TransferSupport.NONE;
    }
}
