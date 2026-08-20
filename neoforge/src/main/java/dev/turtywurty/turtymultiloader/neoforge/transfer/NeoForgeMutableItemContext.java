package dev.turtywurty.turtymultiloader.neoforge.transfer;

import dev.turtywurty.turtymultiloader.transfer.lookup.MutableItemContext;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class NeoForgeMutableItemContext implements MutableItemContext {
    private final ItemAccess itemAccess;

    public NeoForgeMutableItemContext(ItemAccess itemAccess) {
        this.itemAccess = itemAccess;
    }

    public NeoForgeMutableItemContext(ItemStack stack, ItemAccess itemAccess) {
        this(itemAccess);
        if (!ResourceVariant.ofItem(stack).equals(resource()) || stack.getCount() != amount())
            throw new IllegalArgumentException("The NeoForge item context does not describe the supplied stack");
    }

    public ItemAccess itemAccess() {
        return this.itemAccess;
    }

    public static ItemAccess toNeoForge(MutableItemContext neutral) {
        return new ItemAccess() {
            @Override
            public ItemResource getResource() {
                return NeoForgeResourceAdapters.toNeoForgeItem(neutral.resource());
            }

            @Override
            public int getAmount() {
                return Math.toIntExact(Math.min(Integer.MAX_VALUE, neutral.amount()));
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                return Math.toIntExact(neutral.insert(NeoForgeResourceAdapters.fromNeoForge(resource), amount,
                    NeoForgeTransactionAdapters.fromNeoForge(transaction)));
            }

            @Override
            public int extract(ItemResource resource, int amount, TransactionContext transaction) {
                return Math.toIntExact(neutral.extract(NeoForgeResourceAdapters.fromNeoForge(resource), amount,
                    NeoForgeTransactionAdapters.fromNeoForge(transaction)));
            }

            @Override
            public int exchange(ItemResource resource, int amount, TransactionContext transaction) {
                return Math.toIntExact(neutral.exchange(NeoForgeResourceAdapters.fromNeoForge(resource), amount,
                    NeoForgeTransactionAdapters.fromNeoForge(transaction)));
            }
        };
    }

    @Override
    public ItemStack stack() {
        ResourceVariant<Item> resource = resource();
        if (resource.isBlank() || amount() == 0)
            return ItemStack.EMPTY;
        return new ItemStack(
            resource.holder(),
            Math.toIntExact(Math.min(Integer.MAX_VALUE, amount())),
            resource.components()
        );
    }

    @Override
    public ResourceVariant<Item> resource() {
        return NeoForgeResourceAdapters.fromNeoForge(this.itemAccess.getResource());
    }

    @Override
    public long amount() {
        return this.itemAccess.getAmount();
    }

    @Override
    public long insert(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.itemAccess.insert(NeoForgeResourceAdapters.toNeoForgeItem(resource),
            (int) Math.min(Integer.MAX_VALUE, maxAmount), NeoForgeTransactionAdapters.toNeoForge(transaction));
    }

    @Override
    public long extract(ResourceVariant<Item> resource, long maxAmount, TransferContext transaction) {
        return this.itemAccess.extract(NeoForgeResourceAdapters.toNeoForgeItem(resource),
            (int) Math.min(Integer.MAX_VALUE, maxAmount), NeoForgeTransactionAdapters.toNeoForge(transaction));
    }

    @Override
    public long exchange(ResourceVariant<Item> replacement, long maxAmount, TransferContext transaction) {
        return this.itemAccess.exchange(NeoForgeResourceAdapters.toNeoForgeItem(replacement),
            (int) Math.min(Integer.MAX_VALUE, maxAmount), NeoForgeTransactionAdapters.toNeoForge(transaction));
    }
}
