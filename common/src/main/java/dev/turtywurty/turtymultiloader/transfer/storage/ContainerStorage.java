package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransactionParticipant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * A live transactional {@link ResourceStorage} view over a vanilla {@link Container}.
 *
 * <p>If the container is a {@link WorldlyContainer} and a side is supplied, only the slots exposed for that side are
 * visible and the sided insertion and extraction rules are enforced. The sided slot list is captured when the view
 * is created; call {@link #of(Container, Direction)} again if a container changes its exposed slots dynamically.</p>
 *
 * <p>All views of the same container share transaction snapshots. A committed root transaction calls
 * {@link Container#setChanged()}, while a transaction closed without committing restores the original stacks.</p>
 */
public final class ContainerStorage implements ResourceStorage<ResourceVariant<Item>> {
    private static final Map<Container, WeakReference<Backing>> BACKINGS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private final Backing backing;
    private final @Nullable Direction direction;
    private final int @Nullable [] sidedSlots;

    private ContainerStorage(Backing backing, @Nullable Direction direction) {
        this.backing = backing;
        this.direction = backing.container instanceof WorldlyContainer && direction != null ? direction : null;
        this.sidedSlots = this.direction == null ? null : sidedSlots((WorldlyContainer) backing.container, direction);
    }

    /**
     * Returns an unsided live view over every slot in {@code container}.
     */
    public static ContainerStorage of(Container container) {
        return of(container, null);
    }

    /**
     * Returns a live view over {@code container}. For a {@link WorldlyContainer}, a non-null direction applies its
     * sided slot and transfer rules; otherwise the returned view is unsided.
     */
    public static ContainerStorage of(Container container, @Nullable Direction direction) {
        Objects.requireNonNull(container, "container");
        return new ContainerStorage(backing(container), direction);
    }

    public Container container() {
        return this.backing.container;
    }

    public @Nullable Direction direction() {
        return this.direction;
    }

    /**
     * Returns the backing container slot represented by an index in this view.
     */
    public int containerSlot(int index) {
        return slot(index);
    }

    @Override
    public int size() {
        return this.sidedSlots == null ? this.backing.container.getContainerSize() : this.sidedSlots.length;
    }

    @Override
    public ResourceVariant<Item> resource(int index) {
        return ResourceVariant.ofItem(this.backing.container.getItem(slot(index)));
    }

    @Override
    public long amount(int index) {
        return this.backing.container.getItem(slot(index)).getCount();
    }

    @Override
    public long capacity(int index, ResourceVariant<Item> resource) {
        checkType(resource);
        int slot = slot(index);
        if (!isValidContainerSlot(slot, resource))
            return 0;
        return this.backing.container.getMaxStackSize(toStack(resource, 1));
    }

    @Override
    public boolean isValid(int index, ResourceVariant<Item> resource) {
        checkType(resource);
        return isValidContainerSlot(slot(index), resource);
    }

    @Override
    public TransferSupport support(int index) {
        slot(index);
        return TransferSupport.BOTH;
    }

    @Override
    public long insert(int index, ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        checkType(resource);
        Objects.requireNonNull(transaction, "transaction");
        int slot = slot(index);
        if (maxAmount == 0 || !isValidContainerSlot(slot, resource))
            return 0;

        ItemStack current = this.backing.container.getItem(slot);
        ResourceVariant<Item> currentResource = ResourceVariant.ofItem(current);
        if (!currentResource.isBlank() && !currentResource.equals(resource))
            return 0;

        long inserted = Math.min(maxAmount, Math.max(0, capacity(index, resource) - current.getCount()));
        if (inserted > 0) {
            int newAmount = Math.addExact(current.getCount(), Math.toIntExact(inserted));
            this.backing.set(slot, toStack(resource, newAmount), transaction);
        }
        return inserted;
    }

    @Override
    public long extract(int index, ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        checkType(resource);
        Objects.requireNonNull(transaction, "transaction");
        int slot = slot(index);
        ItemStack current = this.backing.container.getItem(slot);
        if (maxAmount == 0 || !ResourceVariant.ofItem(current).equals(resource) || !canExtract(slot, current))
            return 0;

        long extracted = Math.min(maxAmount, current.getCount());
        if (extracted > 0) {
            int remaining = current.getCount() - Math.toIntExact(extracted);
            this.backing.set(slot, remaining == 0 ? ItemStack.EMPTY : toStack(resource, remaining), transaction);
        }
        return extracted;
    }

    @Override
    public long version() {
        return this.backing.version;
    }

    private boolean isValidContainerSlot(int slot, ResourceVariant<Item> resource) {
        if (resource.isBlank())
            return false;
        ItemStack stack = toStack(resource, 1);
        if (!this.backing.container.canPlaceItem(slot, stack))
            return false;
        return this.direction == null
            || ((WorldlyContainer) this.backing.container).canPlaceItemThroughFace(slot, stack, this.direction);
    }

    private boolean canExtract(int slot, ItemStack stack) {
        return this.direction == null
            || ((WorldlyContainer) this.backing.container).canTakeItemThroughFace(slot, stack, this.direction);
    }

    private int slot(int index) {
        StoragePreconditions.index(index, size());
        return this.sidedSlots == null ? index : this.sidedSlots[index];
    }

    private static int[] sidedSlots(WorldlyContainer container, Direction direction) {
        int[] slots = Objects.requireNonNull(container.getSlotsForFace(direction), "getSlotsForFace returned null").clone();
        int containerSize = container.getContainerSize();
        for (int slot : slots)
            StoragePreconditions.index(slot, containerSize);
        return slots;
    }

    private static Backing backing(Container container) {
        synchronized (BACKINGS) {
            WeakReference<Backing> reference = BACKINGS.get(container);
            Backing backing = reference == null ? null : reference.get();
            if (backing == null) {
                backing = new Backing(container);
                BACKINGS.put(container, new WeakReference<>(backing));
            }
            return backing;
        }
    }

    private static void checkType(ResourceVariant<Item> resource) {
        Objects.requireNonNull(resource, "resource");
        if (!ResourceTypes.ITEM.equals(resource.type()))
            throw new IllegalArgumentException("Expected " + ResourceTypes.ITEM + ", got " + resource.type());
    }

    private static ItemStack toStack(ResourceVariant<Item> resource, int amount) {
        return new ItemStack(resource.holder(), amount, resource.components());
    }

    private static final class Backing extends TransactionParticipant<List<ItemStack>> {
        private final Container container;
        private long version;

        private Backing(Container container) {
            this.container = container;
        }

        private void set(int slot, ItemStack stack, TransferContext transaction) {
            updateSnapshots(transaction);
            this.container.setItem(slot, stack);
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            int size = this.container.getContainerSize();
            List<ItemStack> snapshot = new ArrayList<>(size);
            for (int slot = 0; slot < size; slot++)
                snapshot.add(this.container.getItem(slot).copy());
            return snapshot;
        }

        @Override
        protected void restoreSnapshot(List<ItemStack> snapshot) {
            if (this.container.getContainerSize() != snapshot.size())
                throw new IllegalStateException("Container size changed during a transaction");
            for (int slot = 0; slot < snapshot.size(); slot++)
                this.container.setItem(slot, snapshot.get(slot).copy());
        }

        @Override
        protected void onFinalCommit(List<ItemStack> originalState) {
            this.version++;
            this.container.setChanged();
        }
    }
}
