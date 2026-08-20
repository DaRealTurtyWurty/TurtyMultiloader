package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeEnergyAdapter;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeResourceAdapters;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeStorageAdapter;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.concurrent.atomic.AtomicInteger;

public final class NeoForgeTransferGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "neoforge_nested_transaction"
    );
    public static final Identifier LONG_TRANSFER_TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "neoforge_long_transfer_is_bounded"
    );

    private NeoForgeTransferGameTests() {
    }

    public static void verifyNestedTransaction(GameTestHelper helper) {
        AtomicInteger commitCount = new AtomicInteger();
        SimpleSingleSlotStorage<ResourceVariant<Item>> storage = new SimpleSingleSlotStorage<>(
            ResourceTypes.ITEM,
            64,
            TransferSupport.BOTH,
            ignored -> true,
            ignored -> commitCount.incrementAndGet()
        );
        ResourceHandler<ItemResource> handler = NeoForgeStorageAdapter.toNeoForge(
            storage,
            NeoForgeResourceAdapters::fromNeoForge,
            NeoForgeResourceAdapters::toNeoForgeItem,
            Units.ITEM,
            Units.ITEM
        );
        ItemResource item = NeoForgeResourceAdapters.toNeoForgeItem(ResourceVariant.of(
            ResourceTypes.ITEM,
            TestModContent.TEST_LOG_ITEM.holder()
        ));

        try (Transaction root = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, item, 4, root), 4, "Root insertion");
            try (Transaction nested = Transaction.open(root)) {
                helper.assertValueEqual(handler.insert(0, item, 3, nested), 3, "Nested insertion");
                nested.commit();
            }
        }
        helper.assertValueEqual(storage.amount(), 0L, "Root rollback restored an intermediate nested value");
        helper.assertValueEqual(commitCount.get(), 0, "Rollback ran a commit notification");

        try (Transaction root = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, item, 4, root), 4, "Committed root insertion");
            try (Transaction nested = Transaction.open(root)) {
                helper.assertValueEqual(handler.insert(0, item, 3, nested), 3, "Committed nested insertion");
                nested.commit();
            }
            root.commit();
        }
        helper.assertValueEqual(storage.amount(), 7L, "Root commit amount");
        helper.assertValueEqual(commitCount.get(), 1, "Nested transaction ran the commit notification twice");

        ResourceStorage<ResourceVariant<Item>> restrictedRoundTrip = NeoForgeStorageAdapter.fromNeoForgeItems(
            NeoForgeStorageAdapter.toNeoForge(
                storage.restrictedTo(TransferSupport.EXTRACT_ONLY),
                NeoForgeResourceAdapters::fromNeoForge,
                NeoForgeResourceAdapters::toNeoForgeItem,
                Units.ITEM,
                Units.ITEM
            )
        );
        helper.assertTrue(
            restrictedRoundTrip.support(0) == TransferSupport.EXTRACT_ONLY,
            "NeoForge resource round-trip lost transfer direction metadata"
        );

        SimpleSingleSlotStorage<ResourceVariant<UnitResource>> energyStorage = new SimpleSingleSlotStorage<>(
            ResourceTypes.ENERGY,
            100,
            TransferSupport.INSERT_ONLY,
            ignored -> true,
            ignored -> {
            }
        );
        ResourceStorage<ResourceVariant<UnitResource>> energyRoundTrip = NeoForgeEnergyAdapter.fromNeoForge(
            NeoForgeEnergyAdapter.toNeoForge(energyStorage)
        );
        helper.assertTrue(
            energyRoundTrip.support(0) == TransferSupport.INSERT_ONLY,
            "NeoForge energy round-trip lost transfer direction metadata"
        );
        helper.succeed();
    }

    public static void verifyLongTransferIsBounded(GameTestHelper helper) {
        AtomicInteger energyInsertCalls = new AtomicInteger();
        AtomicInteger energyExtractCalls = new AtomicInteger();
        EnergyHandler energyHandler = new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return Long.MAX_VALUE;
            }

            @Override
            public long getCapacityAsLong() {
                return Long.MAX_VALUE;
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                energyInsertCalls.incrementAndGet();
                return amount;
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                energyExtractCalls.incrementAndGet();
                return amount;
            }
        };
        ResourceStorage<ResourceVariant<UnitResource>> energy = NeoForgeEnergyAdapter.fromNeoForge(energyHandler);
        ResourceVariant<UnitResource> energyResource = ResourceTypes.ENERGY.of(Holder.direct(UnitResource.VALUE));

        AtomicInteger resourceInsertCalls = new AtomicInteger();
        AtomicInteger resourceExtractCalls = new AtomicInteger();
        ItemResource item = NeoForgeResourceAdapters.toNeoForgeItem(ResourceVariant.of(
            ResourceTypes.ITEM,
            TestModContent.TEST_LOG_ITEM.holder()
        ));
        ResourceHandler<ItemResource> itemHandler = new ResourceHandler<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public ItemResource getResource(int index) {
                return item;
            }

            @Override
            public long getAmountAsLong(int index) {
                return Long.MAX_VALUE;
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return Long.MAX_VALUE;
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return true;
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                resourceInsertCalls.incrementAndGet();
                return amount;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                resourceExtractCalls.incrementAndGet();
                return amount;
            }
        };
        ResourceStorage<ResourceVariant<Item>> items = NeoForgeStorageAdapter.fromNeoForgeItems(itemHandler);
        ResourceVariant<Item> itemResource = NeoForgeResourceAdapters.fromNeoForge(item);

        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                energy.insert(energyResource, Long.MAX_VALUE, transaction),
                (long) Integer.MAX_VALUE,
                "Energy insertion was not bounded to one native request"
            );
            helper.assertValueEqual(
                energy.extract(energyResource, Long.MAX_VALUE, transaction),
                (long) Integer.MAX_VALUE,
                "Energy extraction was not bounded to one native request"
            );
            helper.assertValueEqual(
                items.insert(0, itemResource, Long.MAX_VALUE, transaction),
                (long) Integer.MAX_VALUE,
                "Resource insertion was not bounded to one native request"
            );
            helper.assertValueEqual(
                items.extract(0, itemResource, Long.MAX_VALUE, transaction),
                (long) Integer.MAX_VALUE,
                "Resource extraction was not bounded to one native request"
            );
        }

        helper.assertValueEqual(energyInsertCalls.get(), 1, "Energy insertion invoked the native handler repeatedly");
        helper.assertValueEqual(energyExtractCalls.get(), 1, "Energy extraction invoked the native handler repeatedly");
        helper.assertValueEqual(resourceInsertCalls.get(), 1, "Resource insertion invoked the native handler repeatedly");
        helper.assertValueEqual(resourceExtractCalls.get(), 1, "Resource extraction invoked the native handler repeatedly");
        helper.succeed();
    }
}
