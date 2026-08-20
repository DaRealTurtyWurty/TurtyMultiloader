package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransactionParticipant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Transactional context backed by a live Minecraft stack reference such as an inventory slot, hand, or cursor.
 */
final class StackReferenceItemContext extends TransactionParticipant<ItemStack> implements MutableItemContext {
    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;
    private final Predicate<ItemStack> validity;
    private final ToIntFunction<ItemStack> capacity;
    private final Runnable changeListener;

    StackReferenceItemContext(
        Supplier<ItemStack> getter,
        Consumer<ItemStack> setter,
        Predicate<ItemStack> validity,
        ToIntFunction<ItemStack> capacity,
        Runnable changeListener
    ) {
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
        this.validity = Objects.requireNonNull(validity, "validity");
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    @Override
    public ItemStack stack() {
        return current().copy();
    }

    @Override
    public ResourceVariant<Item> resource() {
        return ResourceVariant.ofItem(stack());
    }

    @Override
    public long amount() {
        return stack().getCount();
    }

    @Override
    public long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        Objects.requireNonNull(transaction, "transaction");
        if (!isValid(resource))
            return 0;

        ItemStack current = current();
        ResourceVariant<Item> currentResource = ResourceVariant.ofItem(current);
        if (!currentResource.isBlank() && !currentResource.equals(resource))
            return 0;

        long inserted = Math.min(maxAmount, Math.max(0, capacity(resource) - current.getCount()));
        if (inserted > 0) {
            updateSnapshots(transaction);
            set(resource, current.getCount() + Math.toIntExact(inserted));
        }
        return inserted;
    }

    @Override
    public long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        Objects.requireNonNull(transaction, "transaction");
        ItemStack current = current();
        if (!ResourceVariant.ofItem(current).equals(resource))
            return 0;

        long extracted = Math.min(maxAmount, current.getCount());
        if (extracted > 0) {
            updateSnapshots(transaction);
            int remaining = current.getCount() - Math.toIntExact(extracted);
            set(resource, remaining);
        }
        return extracted;
    }

    @Override
    public long capacity(ResourceVariant<Item> resource) {
        if (!isValid(resource))
            return 0;
        return Math.max(amount(), this.capacity.applyAsInt(toStack(resource, 1)));
    }

    @Override
    public boolean isValid(ResourceVariant<Item> resource) {
        if (resource.isBlank())
            return false;
        return this.validity.test(toStack(resource, 1));
    }

    @Override
    protected ItemStack createSnapshot() {
        return current().copy();
    }

    @Override
    protected void restoreSnapshot(ItemStack snapshot) {
        this.setter.accept(snapshot.copy());
    }

    @Override
    protected void onFinalCommit(ItemStack originalState) {
        this.changeListener.run();
    }

    private void set(ResourceVariant<Item> resource, int amount) {
        this.setter.accept(amount == 0 ? ItemStack.EMPTY : toStack(resource, amount));
    }

    private ItemStack current() {
        return Objects.requireNonNull(this.getter.get(), "The item location returned a null stack");
    }

    private static ItemStack toStack(ResourceVariant<Item> resource, int amount) {
        return new ItemStack(resource.holder(), amount, resource.components());
    }
}
