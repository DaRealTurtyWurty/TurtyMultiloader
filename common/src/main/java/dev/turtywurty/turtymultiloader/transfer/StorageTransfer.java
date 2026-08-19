package dev.turtywurty.turtymultiloader.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransactionScope;

import java.util.Objects;

/**
 * TransferTransaction-safe transfer helpers used by pipes and multiblock ports.
 */
public final class StorageTransfer {
    private StorageTransfer() {
    }

    public static <V extends ResourceVariant<?>> long move(
        ResourceStorage<V> source,
        ResourceStorage<V> target,
        V resource,
        long maxAmount
    ) {
        return TransferTransaction.current()
            .<Long>map(transaction -> move(source, target, resource, maxAmount, transaction))
            .orElseGet(() -> {
                try (TransferTransaction transaction = TransferTransaction.openRoot()) {
                    long moved = move(source, target, resource, maxAmount, transaction);
                    transaction.commit();
                    return moved;
                }
            });
    }

    /**
     * Atomically moves as much of a variant as the target accepts within the supplied transaction.
     * The operation uses a nested scope, so failure does not disturb other changes in the caller's transaction.
     */
    public static <V extends ResourceVariant<?>> long move(
        ResourceStorage<V> source,
        ResourceStorage<V> target,
        V resource,
        long maxAmount,
        TransferContext transaction
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(transaction, "transaction");
        if (!source.supportsExtraction() || !target.supportsInsertion())
            return 0;
        try (TransferTransactionScope operation = transaction.openNested()) {
            long available;
            try (TransferTransactionScope simulation = operation.openNested()) {
                available = source.extract(resource, maxAmount, simulation);
            }
            long inserted = target.insert(resource, available, operation);
            long extracted = source.extract(resource, inserted, operation);
            if (inserted > 0 && extracted == inserted) {
                operation.commit();
                return inserted;
            }
            return 0;
        }
    }
}
