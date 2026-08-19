package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransactionParticipant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * General fixed-size storage with per-index capacity, validity, and access policy.
 */
public class SimpleStorage<V extends ResourceVariant<?>> extends TransactionParticipant<List<SimpleStorage.Slot<V>>>
    implements ResourceStorage<V> {
    private final ResourceType<?> resourceType;
    private final long[] capacities;
    private final TransferSupport[] support;
    private final BiPredicate<Integer, V> validity;
    private final List<Slot<V>> slots;
    private final Consumer<SimpleStorage<V>> changeListener;
    private long version;

    public SimpleStorage(ResourceType<?> resourceType, int size, long capacity) {
        this(resourceType, filled(size, capacity), filled(size, TransferSupport.BOTH), (index, resource) -> true, storage -> {
        });
    }

    public SimpleStorage(
        ResourceType<?> resourceType,
        long[] capacities,
        TransferSupport[] support,
        BiPredicate<Integer, V> validity,
        Consumer<SimpleStorage<V>> changeListener
    ) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.capacities = capacities.clone();
        this.support = support.clone();
        if (this.capacities.length != this.support.length)
            throw new IllegalArgumentException("Capacity and support arrays must have the same length");
        this.validity = Objects.requireNonNull(validity, "validity");
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
        this.slots = new ArrayList<>(this.capacities.length);
        for (int index = 0; index < this.capacities.length; index++) {
            if (this.capacities[index] < 0)
                throw new IllegalArgumentException("Capacity must not be negative");
            // Registry-backed resource families may use a queued empty holder which is not bound while storages are
            // constructed. Keep an unobserved empty slot null internally and resolve the holder lazily in resource().
            this.slots.add(new Slot<>(null, 0));
        }
    }

    @SuppressWarnings("unchecked")
    private V empty() {
        return (V) this.resourceType.empty();
    }

    @Override
    public int size() {
        return this.slots.size();
    }

    @Override
    public V resource(int index) {
        Slot<V> slot = slot(index);
        return slot.amount() == 0 ? empty() : slot.resource();
    }

    @Override
    public long amount(int index) {
        return slot(index).amount();
    }

    @Override
    public long capacity(int index, V resource) {
        checkType(resource);
        return isValid(index, resource) ? this.capacities[StoragePreconditions.index(index, size())] : 0;
    }

    @Override
    public boolean isValid(int index, V resource) {
        checkType(resource);
        return this.validity.test(StoragePreconditions.index(index, size()), resource);
    }

    @Override
    public TransferSupport support(int index) {
        return this.support[StoragePreconditions.index(index, size())];
    }

    @Override
    public long insert(int index, V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        checkType(resource);
        index = StoragePreconditions.index(index, size());
        Slot<V> current = this.slots.get(index);
        if (!support(index).supportsInsertion() || !isValid(index, resource)
            || current.amount() > 0 && !current.resource().equals(resource))
            return 0;
        long inserted = Math.min(maxAmount, Math.max(0, this.capacities[index] - current.amount()));
        if (inserted > 0) {
            updateSnapshots(transaction);
            this.slots.set(index, new Slot<>(resource, current.amount() + inserted));
        }
        return inserted;
    }

    @Override
    public long extract(int index, V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        checkType(resource);
        index = StoragePreconditions.index(index, size());
        Slot<V> current = this.slots.get(index);
        if (!support(index).supportsExtraction() || current.amount() == 0 || !current.resource().equals(resource))
            return 0;
        long extracted = Math.min(maxAmount, current.amount());
        if (extracted > 0) {
            updateSnapshots(transaction);
            long remaining = current.amount() - extracted;
            this.slots.set(index, new Slot<>(remaining == 0 ? null : current.resource(), remaining));
        }
        return extracted;
    }

    @Override
    public long version() {
        return this.version;
    }

    @Override
    protected List<Slot<V>> createSnapshot() {
        return List.copyOf(this.slots);
    }

    @Override
    protected void restoreSnapshot(List<Slot<V>> snapshot) {
        this.slots.clear();
        this.slots.addAll(snapshot);
    }

    @Override
    protected void onFinalCommit(List<Slot<V>> originalState) {
        if (!originalState.equals(this.slots)) {
            this.version++;
            this.changeListener.accept(this);
        }
    }

    private Slot<V> slot(int index) {
        return this.slots.get(StoragePreconditions.index(index, size()));
    }

    private void checkType(V resource) {
        if (!this.resourceType.equals(resource.type()))
            throw new IllegalArgumentException("Expected " + this.resourceType + ", got " + resource.type());
    }

    private static long[] filled(int size, long value) {
        long[] result = new long[size];
        Arrays.fill(result, value);
        return result;
    }

    private static TransferSupport[] filled(int size, TransferSupport value) {
        TransferSupport[] result = new TransferSupport[size];
        Arrays.fill(result, value);
        return result;
    }

    protected record Slot<V>(V resource, long amount) {
    }
}
