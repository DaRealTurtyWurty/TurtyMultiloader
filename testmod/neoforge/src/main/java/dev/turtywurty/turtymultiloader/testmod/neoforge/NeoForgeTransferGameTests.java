package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeResourceAdapters;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeStorageAdapter;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

public final class NeoForgeTransferGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "neoforge_nested_transaction"
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
        helper.succeed();
    }
}
