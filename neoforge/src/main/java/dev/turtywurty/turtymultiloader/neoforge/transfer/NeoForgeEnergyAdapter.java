package dev.turtywurty.turtymultiloader.neoforge.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.StoragePreconditions;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.IntUnaryOperator;

public final class NeoForgeEnergyAdapter {
    private NeoForgeEnergyAdapter() {
    }

    public static ResourceStorage<ResourceVariant<UnitResource>> fromNeoForge(EnergyHandler handler) {
        return fromNeoForge(handler, ResourceTypes.ENERGY);
    }

    public static ResourceStorage<ResourceVariant<UnitResource>> fromNeoForge(
        EnergyHandler handler,
        ResourceType<UnitResource> type
    ) {
        ResourceVariant<UnitResource> scalar = type.of(Holder.direct(UnitResource.VALUE));
        return new SingleSlotStorage<>() {
            @Override
            public ResourceVariant<UnitResource> resource(int index) {
                StoragePreconditions.index(index, 1);
                return handler.getAmountAsLong() == 0 ? type.empty() : scalar;
            }

            @Override
            public long amount(int index) {
                StoragePreconditions.index(index, 1);
                return handler.getAmountAsLong();
            }

            @Override
            public long capacity(int index, ResourceVariant<UnitResource> resource) {
                StoragePreconditions.index(index, 1);
                return handler.getCapacityAsLong();
            }

            @Override
            public boolean isValid(int index, ResourceVariant<UnitResource> resource) {
                StoragePreconditions.index(index, 1);
                return resource.equals(scalar);
            }

            @Override
            public TransferSupport support(int index) {
                StoragePreconditions.index(index, 1);
                return TransferSupport.BOTH;
            }

            @Override
            public long insert(int index, ResourceVariant<UnitResource> resource, long maxAmount,
                               TransferContext transaction) {
                StoragePreconditions.index(index, 1);
                if (!resource.equals(scalar))
                    return 0;
                return move(maxAmount, amount -> handler.insert(amount,
                    NeoForgeTransactionAdapters.toNeoForge(transaction)));
            }

            @Override
            public long extract(int index, ResourceVariant<UnitResource> resource, long maxAmount,
                                TransferContext transaction) {
                StoragePreconditions.index(index, 1);
                if (!resource.equals(scalar))
                    return 0;
                return move(maxAmount, amount -> handler.extract(amount,
                    NeoForgeTransactionAdapters.toNeoForge(transaction)));
            }
        };
    }

    private static long move(long maxAmount, IntUnaryOperator operation) {
        long remaining = maxAmount;
        long movedTotal = 0;
        while (remaining > 0) {
            int requested = (int) Math.min(Integer.MAX_VALUE, remaining);
            int moved = operation.applyAsInt(requested);
            movedTotal += moved;
            remaining -= moved;
            if (moved < requested)
                break;
        }
        return movedTotal;
    }

    public static EnergyHandler toNeoForge(ResourceStorage<ResourceVariant<UnitResource>> storage) {
        return toNeoForge(storage, ResourceTypes.ENERGY);
    }

    public static EnergyHandler toNeoForge(
        ResourceStorage<ResourceVariant<UnitResource>> storage,
        ResourceType<UnitResource> type
    ) {
        ResourceVariant<UnitResource> scalar = type.of(Holder.direct(UnitResource.VALUE));
        return new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return storage.amount(0);
            }

            @Override
            public long getCapacityAsLong() {
                return storage.capacity(0, scalar);
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                return Math.toIntExact(storage.insert(scalar, amount,
                    NeoForgeTransactionAdapters.fromNeoForge(transaction)));
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                return Math.toIntExact(storage.extract(scalar, amount,
                    NeoForgeTransactionAdapters.fromNeoForge(transaction)));
            }
        };
    }
}
