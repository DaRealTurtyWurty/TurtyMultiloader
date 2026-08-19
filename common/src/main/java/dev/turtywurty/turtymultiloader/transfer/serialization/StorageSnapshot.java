package dev.turtywurty.turtymultiloader.transfer.serialization;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable serializable/synchronizable representation of indexed storage contents.
 */
public record StorageSnapshot<V extends ResourceVariant<?>>(List<Entry<V>> entries) {
    public StorageSnapshot {
        entries = List.copyOf(entries);
    }

    public static <V extends ResourceVariant<?>> StorageSnapshot<V> capture(ResourceStorage<V> storage) {
        List<Entry<V>> entries = new ArrayList<>(storage.size());
        for (int index = 0; index < storage.size(); index++)
            entries.add(new Entry<>(storage.resource(index), storage.amount(index)));
        return new StorageSnapshot<>(entries);
    }

    /**
     * Replaces contents transactionally, failing without mutation if a target slot cannot match the snapshot.
     */
    public boolean apply(ResourceStorage<V> storage) {
        return TransferTransaction.current()
            .map(transaction -> apply(storage, transaction))
            .orElseGet(() -> {
                try (TransferTransaction transaction = TransferTransaction.openRoot()) {
                    boolean applied = apply(storage, transaction);
                    if (applied)
                        transaction.commit();
                    return applied;
                }
            });
    }

    /**
     * Replaces contents within the supplied transaction. A failed application rolls back only its nested scope.
     */
    public boolean apply(ResourceStorage<V> storage, TransferContext transaction) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(transaction, "transaction");
        if (storage.size() != this.entries.size())
            return false;
        try (TransferTransactionScope operation = transaction.openNested()) {
            for (int index = 0; index < storage.size(); index++) {
                V current = storage.resource(index);
                long currentAmount = storage.amount(index);
                if (currentAmount > 0 && storage.extract(index, current, currentAmount, operation) != currentAmount)
                    return false;
                Entry<V> entry = this.entries.get(index);
                if (entry.amount > 0 && storage.insert(index, entry.resource, entry.amount, operation) != entry.amount)
                    return false;
            }
            operation.commit();
            return true;
        }
    }

    public record Entry<V extends ResourceVariant<?>>(V resource, long amount) {
        public Entry {
            if (amount < 0)
                throw new IllegalArgumentException("Amount must not be negative");
            if (amount > 0 && resource.isBlank())
                throw new IllegalArgumentException("A non-empty entry needs a non-blank resource");
        }
    }
}
