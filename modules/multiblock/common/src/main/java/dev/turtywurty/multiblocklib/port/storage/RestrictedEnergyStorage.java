package dev.turtywurty.multiblocklib.port.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;

/**
 * Compatibility specialization for neutral energy storage.
 */
public final class RestrictedEnergyStorage extends RestrictedStorage<ResourceVariant<UnitResource>> {
    public RestrictedEnergyStorage(
        final ResourceStorage<ResourceVariant<UnitResource>> storage,
        final boolean allowInsert,
        final boolean allowExtract
    ) {
        super(storage, allowInsert, allowExtract);
    }
}
