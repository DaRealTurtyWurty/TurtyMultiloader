package dev.turtywurty.turtymultiloader.fabric.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Holder;
import team.reborn.energy.api.EnergyStorage;

/**
 * Adapts Team Reborn Energy API storages to the neutral energy storage and vice versa.
 */
public final class FabricEnergyAdapter {
    private static final ResourceVariant<UnitResource> ENERGY =
        ResourceTypes.ENERGY.of(Holder.direct(UnitResource.VALUE));

    private FabricEnergyAdapter() {
    }

    public static ResourceStorage<ResourceVariant<UnitResource>> fromFabric(EnergyStorage storage) {
        return new SingleSlotStorage<>() {
            @Override
            public ResourceVariant<UnitResource> resource(int index) {
                StoragePreconditions.index(index, 1);
                return storage.getAmount() == 0 ? ResourceTypes.ENERGY.empty() : ENERGY;
            }

            @Override
            public long amount(int index) {
                StoragePreconditions.index(index, 1);
                return storage.getAmount();
            }

            @Override
            public long capacity(int index, ResourceVariant<UnitResource> resource) {
                StoragePreconditions.index(index, 1);
                return storage.getCapacity();
            }

            @Override
            public boolean isValid(int index, ResourceVariant<UnitResource> resource) {
                StoragePreconditions.index(index, 1);
                return resource.equals(ENERGY);
            }

            @Override
            public TransferSupport support(int index) {
                StoragePreconditions.index(index, 1);
                boolean insert = storage.supportsInsertion();
                boolean extract = storage.supportsExtraction();
                return insert ? extract ? TransferSupport.BOTH : TransferSupport.INSERT_ONLY
                    : extract ? TransferSupport.EXTRACT_ONLY : TransferSupport.NONE;
            }

            @Override
            public long insert(
                int index,
                ResourceVariant<UnitResource> resource,
                long maxAmount,
                TransferContext transaction
            ) {
                StoragePreconditions.index(index, 1);
                if (!resource.equals(ENERGY))
                    return 0;
                return storage.insert(maxAmount, FabricTransactionAdapters.toFabric(transaction));
            }

            @Override
            public long extract(
                int index,
                ResourceVariant<UnitResource> resource,
                long maxAmount,
                TransferContext transaction
            ) {
                StoragePreconditions.index(index, 1);
                if (!resource.equals(ENERGY))
                    return 0;
                return storage.extract(maxAmount, FabricTransactionAdapters.toFabric(transaction));
            }
        };
    }

    public static EnergyStorage toFabric(ResourceStorage<ResourceVariant<UnitResource>> storage) {
        return new EnergyStorage() {
            @Override
            public boolean supportsInsertion() {
                return storage.support(0).supportsInsertion();
            }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                return storage.insert(ENERGY, maxAmount, FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public boolean supportsExtraction() {
                return storage.support(0).supportsExtraction();
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                return storage.extract(ENERGY, maxAmount, FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public long getAmount() {
                return storage.amount(0);
            }

            @Override
            public long getCapacity() {
                return storage.capacity(0, ENERGY);
            }
        };
    }
}
