package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;

import java.util.Objects;

public final class StoragePreconditions {
    private StoragePreconditions() {
    }

    public static void check(ResourceVariant<?> resource, long amount) {
        Objects.requireNonNull(resource, "resource");
        if (resource.isBlank())
            throw new IllegalArgumentException("A blank resource cannot be transferred");
        if (amount < 0)
            throw new IllegalArgumentException("Amount must not be negative: " + amount);
    }

    public static int index(int index, int size) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(index);
        return index;
    }
}
