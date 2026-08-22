package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.data.PortIO;
import dev.turtywurty.multiblocklib.pattern.MultiblockPatternRegistry;
import dev.turtywurty.multiblocklib.port.storage.MultiblockItemPortStorage;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class MultiblockModuleGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "multiblock_module"
    );

    private MultiblockModuleGameTests() {
    }

    public static void verifyModule(GameTestHelper helper) {
        helper.assertValueEqual(
            BuiltInRegistries.BLOCK.getValue(MultiblockLib.MULTIBLOCK_PART_KEY.identifier()),
            MultiblockLib.MULTIBLOCK_PART,
            "Multiblock part registration"
        );
        helper.assertTrue(
            MultiblockLib.MULTIBLOCK_CONTROLLER_ENTITY_HANDLE.get()
                .isValid(MultiblockLib.MULTIBLOCK_CONTROLLER.defaultBlockState()),
            "Multiblock controller block-entity type does not accept its block"
        );
        helper.assertTrue(
            MultiblockPatternRegistry.get(Identifier.fromNamespaceAndPath(MultiblockLib.MOD_ID, "grid")) != null,
            "Built-in multiblock patterns were not initialized"
        );

        MultiblockItemPortStorage storage = new MultiblockItemPortStorage(8, PortIO.BOTH, () -> {
        });
        ResourceVariant<Item> stone = ResourceTypes.ITEM.of(Items.STONE.builtInRegistryHolder());
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(storage.insert(stone, 5, transaction), 5L, "Neutral item port insertion");
            transaction.commit();
        }
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                storage.restrictedTo(TransferSupport.EXTRACT_ONLY).insert(stone, 1, transaction),
                0L,
                "Output-only port accepted insertion"
            );
        }
        helper.assertValueEqual(storage.getAmount(), 5L, "Neutral item port amount");
        helper.succeed();
    }
}
