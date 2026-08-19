package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;

public interface SingleSlotStorage<V extends ResourceVariant<?>> extends ResourceStorage<V> {
    @Override
    default int size() {
        return 1;
    }

    default V resource() {
        return resource(0);
    }

    default long amount() {
        return amount(0);
    }

    default long capacity(V resource) {
        return capacity(0, resource);
    }

    default long insert(V resource, long maxAmount, TransferContext transaction) {
        return insert(0, resource, maxAmount, transaction);
    }

    default long extract(V resource, long maxAmount, TransferContext transaction) {
        return extract(0, resource, maxAmount, transaction);
    }
}
