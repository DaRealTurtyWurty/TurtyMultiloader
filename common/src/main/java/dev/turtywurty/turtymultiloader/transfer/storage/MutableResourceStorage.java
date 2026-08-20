package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

/**
 * Machine-owned storage with a trusted mutation path.
 *
 * <p>The internal operations deliberately bypass the storage's externally advertised
 * {@link TransferSupport}. They still enforce resource type, validity, and capacity invariants and participate in the
 * supplied transaction. These methods must not be exposed directly to external automation.</p>
 */
public interface MutableResourceStorage<V extends ResourceVariant<?>> extends ResourceStorage<V> {
    long insertInternal(int index, V resource, long maxAmount, TransferContext transaction);

    long extractInternal(int index, V resource, long maxAmount, TransferContext transaction);

    /**
     * Replaces one slot exactly, bypassing external transfer direction restrictions.
     *
     * @return {@code false} when the requested state violates the storage's validity or capacity invariants
     */
    boolean set(int index, V resource, long amount, TransferContext transaction);

    default long insertInternal(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long inserted = 0;
        for (int index = 0; index < size() && inserted < maxAmount; index++)
            inserted += insertInternal(index, resource, maxAmount - inserted, transaction);
        return inserted;
    }

    default long extractInternal(V resource, long maxAmount, TransferContext transaction) {
        StoragePreconditions.check(resource, maxAmount);
        long extracted = 0;
        for (int index = 0; index < size() && extracted < maxAmount; index++)
            extracted += extractInternal(index, resource, maxAmount - extracted, transaction);
        return extracted;
    }
}
