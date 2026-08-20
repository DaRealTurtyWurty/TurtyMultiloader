package dev.turtywurty.gasapi.api.storage.util;

import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.StorageTransfer;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;

/**
 * Neutral gas-container interaction utilities.
 */
public final class GasStorageUtil {
    private GasStorageUtil() {
    }

    public static boolean interactWithGasStorage(
        ResourceStorage<ResourceVariant<Gas>> storage,
        ResourceStorage<ResourceVariant<Gas>> heldItemStorage
    ) {
        requireStableIndices(storage, "storage");
        requireStableIndices(heldItemStorage, "heldItemStorage");
        return moveFirst(storage, heldItemStorage) || moveFirst(heldItemStorage, storage);
    }

    private static void requireStableIndices(ResourceStorage<?> storage, String name) {
        if (!storage.hasStableIndices())
            throw new IllegalArgumentException(name + " must expose stable indices");
    }

    private static boolean moveFirst(
        ResourceStorage<ResourceVariant<Gas>> source,
        ResourceStorage<ResourceVariant<Gas>> target
    ) {
        for (int index = 0; index < source.size(); index++) {
            if (source.amount(index) == 0)
                continue;
            ResourceVariant<Gas> resource = source.resource(index);
            if (StorageTransfer.move(source, target, resource, source.amount(index)) > 0)
                return true;
        }
        return false;
    }
}
