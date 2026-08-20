package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.fabric.transfer.FabricMutableItemContext;
import dev.turtywurty.turtymultiloader.fabric.transfer.FabricResourceAdapters;
import dev.turtywurty.turtymultiloader.testmod.*;
import dev.turtywurty.turtymultiloader.transfer.lookup.BlockStorageCache;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageItemContext;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

public final class TestModFabricGameTests {
    @GameTest(structure = "minecraft:empty")
    public void configurationService(GameTestHelper helper) {
        ConfigurationGameTests.verifyConfigurationService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void attachmentService(GameTestHelper helper) {
        AttachmentGameTests.verifyAttachmentService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void registryService(GameTestHelper helper) {
        RegistryGameTests.verifyRegistryService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void menuService(GameTestHelper helper) {
        MenuGameTests.verifyMenuService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void transferService(GameTestHelper helper) {
        TransferGameTests.verifyTransferService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void worldGenerationService(GameTestHelper helper) {
        WorldGenerationGameTests.verifyWorldGenerationService(helper);
    }

    @GameTest(structure = "minecraft:empty")
    public void teamRebornEnergy(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, TestModContent.TEST_LOG.get());
        EnergyStorage blockStorage = EnergyStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.UP
        );
        helper.assertTrue(blockStorage != null, "Team Reborn block energy lookup did not resolve");

        EnergyStorage itemStorage = ContainerItemContext.withConstant(
            new ItemStack(TestModContent.TEST_LOG_ITEM.get())
        ).find(EnergyStorage.ITEM);
        helper.assertTrue(itemStorage != null, "Team Reborn item energy lookup did not resolve");
        helper.succeed();
    }

    @GameTest(structure = "minecraft:empty")
    public void fabricContextAndCacheLifecycle(GameTestHelper helper) {
        ResourceVariant<Item> item = ResourceVariant.of(
            ResourceTypes.ITEM,
            TestModContent.TEST_LOG_ITEM.holder()
        );
        SimpleSingleSlotStorage<ResourceVariant<Item>> main =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        SimpleSingleSlotStorage<ResourceVariant<Item>> additional =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        SimpleSingleSlotStorage<ResourceVariant<Item>> overflow =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        ContainerItemContext nativeContext = FabricMutableItemContext.toFabric(
            new StorageItemContext(main, List.of(additional), overflow)
        );
        helper.assertValueEqual(nativeContext.getAdditionalSlots().size(), 1, "Fabric additional-slot count");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(
                nativeContext.getAdditionalSlots().getFirst().insert(
                    FabricResourceAdapters.toFabricItem(item), 2, transaction
                ),
                2L,
                "Fabric additional-slot insertion"
            );
            helper.assertValueEqual(
                nativeContext.insertOverflow(FabricResourceAdapters.toFabricItem(item), 3, transaction),
                3L,
                "Fabric overflow insertion"
            );
            transaction.commit();
        }
        helper.assertValueEqual(additional.amount(), 2L, "Neutral additional-slot amount");
        helper.assertValueEqual(overflow.amount(), 3L, "Neutral overflow amount");
        helper.assertValueEqual(
            new FabricMutableItemContext(nativeContext).additionalSlots().size(),
            1,
            "Neutral context did not preserve Fabric additional slots"
        );

        BlockPos relative = new BlockPos(2, 1, 1);
        helper.setBlock(relative, TestModContent.TEST_LOG.get());
        BlockStorageCache<ResourceVariant<Item>> cache = TestModContent.TRANSFERS.createBlockCache(
            StorageKeys.ITEM,
            helper.getLevel(),
            helper.absolutePos(relative),
            Direction.UP
        );
        helper.assertTrue(cache.find() != null, "Fabric cache did not resolve");
        cache.close();
        boolean rejectedClosedLookup = false;
        try {
            cache.find();
        } catch (IllegalStateException ignored) {
            rejectedClosedLookup = true;
        }
        helper.assertTrue(rejectedClosedLookup, "Closed Fabric cache remained usable");
        helper.succeed();
    }
}
