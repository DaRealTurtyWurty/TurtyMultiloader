package dev.turtywurty.turtymultiloader.fabric.transfer;

import dev.turtywurty.turtymultiloader.transfer.lookup.MutableItemContext;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Neutral view of Fabric's mutable container-item context.
 */
public final class FabricMutableItemContext implements MutableItemContext {
    private final ContainerItemContext fabric;

    public FabricMutableItemContext(ContainerItemContext fabric) {
        this.fabric = fabric;
    }

    public ContainerItemContext fabricContext() {
        return this.fabric;
    }

    public static ContainerItemContext toFabric(MutableItemContext neutral) {
        SingleSlotStorage<ItemVariant> mainSlot = new SingleSlotStorage<>() {
            @Override
            public boolean supportsInsertion() {
                return neutral.support().supportsInsertion();
            }

            @Override
            public long insert(ItemVariant resource, long maxAmount,
                               TransactionContext transaction) {
                return neutral.insert(FabricResourceAdapters.fromFabric(resource), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public boolean supportsExtraction() {
                return neutral.support().supportsExtraction();
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount,
                                TransactionContext transaction) {
                return neutral.extract(FabricResourceAdapters.fromFabric(resource), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public boolean isResourceBlank() {
                return neutral.resource().isBlank();
            }

            @Override
            public ItemVariant getResource() {
                return FabricResourceAdapters.toFabricItem(neutral.resource());
            }

            @Override
            public long getAmount() {
                return neutral.amount();
            }

            @Override
            public long getCapacity() {
                return neutral.capacity(neutral.resource());
            }
        };
        List<SingleSlotStorage<ItemVariant>> additionalSlots = neutral.additionalSlots().stream()
            .map(FabricMutableItemContext::toFabricSlot)
            .toList();
        return new ContainerItemContext() {
            @Override
            public SingleSlotStorage<ItemVariant> getMainSlot() {
                return mainSlot;
            }

            @Override
            public long exchange(ItemVariant newVariant, long maxAmount, TransactionContext transaction) {
                return neutral.exchange(FabricResourceAdapters.fromFabric(newVariant), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public long insertOverflow(ItemVariant itemVariant, long maxAmount,
                                       TransactionContext transaction) {
                return neutral.insertOverflow(FabricResourceAdapters.fromFabric(itemVariant), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
                return additionalSlots;
            }
        };
    }

    private static SingleSlotStorage<ItemVariant> toFabricSlot(
        ResourceStorage<ResourceVariant<Item>> neutral
    ) {
        if (neutral.size() != 1)
            throw new IllegalArgumentException("An item context's additional storage must have exactly one slot");
        return new SingleSlotStorage<>() {
            @Override
            public boolean supportsInsertion() {
                return neutral.support(0).supportsInsertion();
            }

            @Override
            public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return neutral.insert(0, FabricResourceAdapters.fromFabric(resource), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public boolean supportsExtraction() {
                return neutral.support(0).supportsExtraction();
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return neutral.extract(0, FabricResourceAdapters.fromFabric(resource), maxAmount,
                    FabricTransactionAdapters.fromFabric(transaction));
            }

            @Override
            public boolean isResourceBlank() {
                return neutral.resource(0).isBlank();
            }

            @Override
            public ItemVariant getResource() {
                return FabricResourceAdapters.toFabricItem(neutral.resource(0));
            }

            @Override
            public long getAmount() {
                return neutral.amount(0);
            }

            @Override
            public long getCapacity() {
                return neutral.capacity(0, neutral.resource(0));
            }
        };
    }

    @Override
    public ResourceVariant<Item> resource() {
        return FabricResourceAdapters.fromFabric(this.fabric.getItemVariant());
    }

    @Override
    public long amount() {
        return resource().isBlank() ? 0 : this.fabric.getAmount();
    }

    @Override
    public long insert(ResourceVariant<Item> resource, long maxAmount,
                       TransferContext transaction) {
        return this.fabric.insert(FabricResourceAdapters.toFabricItem(resource), maxAmount,
            FabricTransactionAdapters.toFabric(transaction));
    }

    @Override
    public long extract(ResourceVariant<Item> resource, long maxAmount,
                        TransferContext transaction) {
        return this.fabric.extract(FabricResourceAdapters.toFabricItem(resource), maxAmount,
            FabricTransactionAdapters.toFabric(transaction));
    }

    @Override
    public long exchange(ResourceVariant<Item> replacement, long maxAmount, TransferContext transaction) {
        return this.fabric.exchange(FabricResourceAdapters.toFabricItem(replacement), maxAmount,
            FabricTransactionAdapters.toFabric(transaction));
    }

    @Override
    public long capacity(ResourceVariant<Item> resource) {
        return this.fabric.getMainSlot().getCapacity();
    }

    @Override
    public TransferSupport support() {
        boolean insert = this.fabric.getMainSlot().supportsInsertion();
        boolean extract = this.fabric.getMainSlot().supportsExtraction();
        return insert ? extract ? TransferSupport.BOTH : TransferSupport.INSERT_ONLY
            : extract ? TransferSupport.EXTRACT_ONLY : TransferSupport.NONE;
    }

    @Override
    public List<? extends ResourceStorage<ResourceVariant<Item>>> additionalSlots() {
        return this.fabric.getAdditionalSlots().stream()
            .map(FabricStorageAdapter::fromFabricItems)
            .toList();
    }

    @Override
    public long insertOverflow(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.fabric.insertOverflow(FabricResourceAdapters.toFabricItem(resource), maxAmount,
            FabricTransactionAdapters.toFabric(transaction));
    }
}
