package dev.turtywurty.turtymultiloader.testmod;

import com.mojang.serialization.JsonOps;
import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.gasapi.api.GasVariant;
import dev.turtywurty.gasapi.api.storage.GasStorage;
import dev.turtywurty.gasapi.api.storage.InputSingleGasStorage;
import dev.turtywurty.gasapi.api.storage.OutputSingleGasStorage;
import dev.turtywurty.gasapi.api.storage.item.EmptyItemGasStorage;
import dev.turtywurty.gasapi.api.storage.item.FullItemGasStorage;
import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.slurryapi.api.SlurryVariant;
import dev.turtywurty.slurryapi.api.storage.InputSingleSlurryStorage;
import dev.turtywurty.slurryapi.api.storage.OutputSingleSlurryStorage;
import dev.turtywurty.slurryapi.api.storage.SlurryStorage;
import dev.turtywurty.turtymultiloader.transfer.StorageTransfer;
import dev.turtywurty.turtymultiloader.transfer.lookup.BlockStorageCache;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageItemContext;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariantCodecs;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.serialization.StorageSnapshot;
import dev.turtywurty.turtymultiloader.transfer.serialization.StorageSynchronizer;
import dev.turtywurty.turtymultiloader.transfer.storage.*;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.UnitDimension;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TransferGameTests {
    public static final Identifier TEST_ID = id("transfer_service");
    private static final TransferUnit DOUBLE_ENERGY = Units.REGISTRY.register(
        id("double_energy"), UnitDimension.ENERGY, "2E", 2
    );

    private TransferGameTests() {
    }

    public static void verifyTransferService(GameTestHelper helper) {
        helper.assertTrue(TestModContent.TRANSFERS.isApplied(), "TransferService was not applied");

        ResourceVariant<Item> item = ResourceVariant.of(
            ResourceTypes.ITEM,
            TestModContent.TEST_LOG_ITEM.holder()
        );
        clear(TestModContent.TEST_ITEM_STORAGE);

        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.insert(item, 10, transaction), 10L,
                "Rolled-back insertion amount");
        }
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 0L, "Root rollback");

        try (TransferTransaction root = TransferTransaction.openRoot()) {
            try (TransferTransaction nested = root.openNested()) {
                TestModContent.TEST_ITEM_STORAGE.insert(item, 7, nested);
                nested.commit();
            }
        }
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 0L, "Nested commit followed by root rollback");

        AtomicBoolean committed = new AtomicBoolean();
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            TestModContent.TEST_ITEM_STORAGE.insert(item, 12, transaction);
            transaction.addCommitCallback(() -> committed.set(true));
            transaction.commit();
        }
        helper.assertTrue(committed.get(), "Root commit callback did not run");
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 12L, "Committed amount");
        long simulated = TransferTransaction.runSimulation(transaction ->
            TestModContent.TEST_ITEM_STORAGE.extract(item, 5, transaction));
        helper.assertValueEqual(simulated, 5L, "Simulation result");
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 12L, "Simulation rollback");

        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, TestModContent.TEST_LOG.get());
        BlockPos absolute = helper.absolutePos(relative);
        ResourceStorage<ResourceVariant<Item>> exposed = TestModContent.TRANSFERS.findBlock(
            StorageKeys.ITEM, helper.getLevel(), absolute, Direction.UP
        );
        helper.assertTrue(exposed != null, "Loader-native block exposure was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(exposed.extract(item, 2, transaction), 2L, "Adapter extraction");
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 10L, "Adapter committed amount");

        ResourceVariant<UnitResource> energy = ResourceTypes.ENERGY.of(Holder.direct(UnitResource.VALUE));
        clear(TestModContent.TEST_ENERGY_STORAGE);
        ResourceStorage<ResourceVariant<UnitResource>> exposedEnergy = TestModContent.TRANSFERS.findBlock(
            StorageKeys.ENERGY, helper.getLevel(), absolute, Direction.UP
        );
        helper.assertTrue(exposedEnergy != null, "Loader-native energy block exposure was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(exposedEnergy.insert(energy, 250, transaction), 250L, "Energy adapter insertion");
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_ENERGY_STORAGE.amount(), 250L, "Energy adapter committed amount");

        StorageSynchronizer<ResourceVariant<UnitResource>> synchronizer = new StorageSynchronizer<>(exposedEnergy);
        helper.assertTrue(synchronizer.sendIfChanged(ignored -> {
        }), "Initial synchronization was not sent");
        helper.assertTrue(!synchronizer.sendIfChanged(ignored -> {
        }), "Unchanged storage synchronized twice");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            exposedEnergy.insert(energy, 1, transaction);
            transaction.commit();
        }
        helper.assertTrue(
            synchronizer.sendIfChanged(ignored -> {
            }),
            "Synchronization missed a loader-native storage mutation"
        );

        ResourceVariant<Fluid> water = ResourceTypes.FLUID.of(Fluids.WATER.builtInRegistryHolder());
        clear(TestModContent.TEST_FLUID_STORAGE);
        ResourceStorage<ResourceVariant<Fluid>> exposedFluid = TestModContent.TRANSFERS.findBlock(
            StorageKeys.FLUID, helper.getLevel(), absolute, Direction.UP
        );
        helper.assertTrue(exposedFluid != null, "Loader-native fluid block exposure was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                exposedFluid.insert(water, 81_000, transaction),
                81_000L,
                "One bucket did not cross the loader unit adapter"
            );
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_FLUID_STORAGE.amount(), 81_000L, "Neutral fluid amount");

        ResourceVariant<Gas> gas = GasVariant.of(TestModContent.TEST_GAS.holder());
        clear(TestModContent.TEST_GAS_STORAGE);
        ResourceStorage<ResourceVariant<Gas>> exposedGas = GasStorage.findBlock(
            helper.getLevel(), absolute, Direction.UP
        );
        helper.assertTrue(exposedGas != null, "Custom gas capability was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(exposedGas.insert(gas, 4_000, transaction), 4_000L, "Gas adapter insertion");
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_GAS_STORAGE.amount(), 4_000L, "Gas committed amount");
        helper.assertTrue(
            new InputSingleGasStorage(1).support(0) == TransferSupport.INSERT_ONLY,
            "Input gas storage advertised extraction support"
        );
        helper.assertTrue(
            new OutputSingleGasStorage(1).support(0) == TransferSupport.EXTRACT_ONLY,
            "Output gas storage advertised insertion support"
        );
        var registryOps = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var encodedGas = GasVariant.CODEC.encodeStart(registryOps, gas).getOrThrow();
        helper.assertValueEqual(
            GasVariant.CODEC.parse(registryOps, encodedGas).getOrThrow(),
            gas,
            "Gas resource codec round-trip"
        );

        SimpleSingleSlotStorage<ResourceVariant<Item>> gasContainerSlot =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 1);
        ItemStack componentContainer = new ItemStack(Items.BUCKET);
        componentContainer.set(TestModContent.TEST_NUMBER.get(), 42);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            gasContainerSlot.insert(ResourceVariant.ofItem(componentContainer), 1, transaction);
            transaction.commit();
        }
        StorageItemContext gasContainerContext = new StorageItemContext(gasContainerSlot);
        EmptyItemGasStorage emptyGasContainer = new EmptyItemGasStorage(
            gasContainerContext,
            Items.GLASS_BOTTLE,
            TestModContent.TEST_GAS.get(),
            1_000
        );
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                emptyGasContainer.insert(gas, 1_000, transaction),
                1_000L,
                "Empty gas container insertion"
            );
            transaction.commit();
        }
        helper.assertTrue(gasContainerContext.resource().value() == Items.GLASS_BOTTLE,
            "Gas insertion did not exchange the container item");
        helper.assertValueEqual(gasContainerContext.stack().get(TestModContent.TEST_NUMBER.get()), 42,
            "Gas container exchange lost item components");
        FullItemGasStorage fullGasContainer = new FullItemGasStorage(
            gasContainerContext,
            Items.BUCKET,
            gas,
            1_000
        );
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                fullGasContainer.extract(gas, 1_000, transaction),
                1_000L,
                "Full gas container extraction"
            );
            transaction.commit();
        }
        helper.assertTrue(gasContainerContext.resource().value() == Items.BUCKET,
            "Gas extraction did not exchange the container item");

        ResourceVariant<Slurry> slurry = SlurryVariant.of(TestModContent.TEST_SLURRY.holder());
        clear(TestModContent.TEST_SLURRY_STORAGE);
        ResourceStorage<ResourceVariant<Slurry>> exposedSlurry = SlurryStorage.findBlock(
            helper.getLevel(), absolute, Direction.UP
        );
        helper.assertTrue(exposedSlurry != null, "Custom slurry capability was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                exposedSlurry.insert(slurry, 3_000, transaction),
                3_000L,
                "Slurry adapter insertion"
            );
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_SLURRY_STORAGE.amount(), 3_000L, "Slurry committed amount");
        helper.assertTrue(
            new InputSingleSlurryStorage(1).support(0) == TransferSupport.INSERT_ONLY,
            "Input slurry storage advertised extraction support"
        );
        helper.assertTrue(
            new OutputSingleSlurryStorage(1).support(0) == TransferSupport.EXTRACT_ONLY,
            "Output slurry storage advertised insertion support"
        );
        var encodedSlurry = SlurryVariant.CODEC.encodeStart(registryOps, slurry).getOrThrow();
        helper.assertValueEqual(
            SlurryVariant.CODEC.parse(registryOps, encodedSlurry).getOrThrow(),
            slurry,
            "Slurry resource codec round-trip"
        );

        SimpleSingleSlotStorage<ResourceVariant<Item>> itemContainer =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            itemContainer.insert(item, 1, transaction);
            transaction.commit();
        }
        ResourceStorage<ResourceVariant<Item>> itemExposed = TestModContent.TRANSFERS.findItem(
            StorageKeys.ITEM,
            new ItemStack(TestModContent.TEST_LOG_ITEM.get()),
            new StorageItemContext(itemContainer)
        );
        helper.assertTrue(itemExposed != null, "Loader-native item exposure was not found");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(itemExposed.extract(item, 1, transaction), 1L, "Item adapter extraction");
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_ITEM_STORAGE.amount(), 9L, "Item adapter committed amount");
        ResourceStorage<ResourceVariant<UnitResource>> itemEnergy = TestModContent.TRANSFERS.findItem(
            StorageKeys.ENERGY,
            new ItemStack(TestModContent.TEST_LOG_ITEM.get()),
            new StorageItemContext(itemContainer)
        );
        helper.assertTrue(itemEnergy != null, "Loader-native energy item exposure was not found");
        helper.assertValueEqual(itemEnergy.amount(0), 251L, "Energy item adapter amount");

        try (BlockStorageCache<ResourceVariant<Item>> cache = TestModContent.TRANSFERS.createBlockCache(
            StorageKeys.ITEM, helper.getLevel(), absolute, Direction.UP
        )) {
            helper.assertTrue(cache.find() != null, "Cached lookup did not resolve");
            TestModContent.TRANSFERS.invalidateBlock(helper.getLevel(), absolute);
            cache.invalidate();
            helper.assertTrue(cache.find() != null, "Lookup did not recover after explicit invalidation");
        }

        BlockPos multiRelative = new BlockPos(3, 1, 2);
        helper.setBlock(multiRelative, TestModContent.STRIPPED_TEST_LOG.get());
        clear(TestModContent.TEST_MULTI_ITEM_STORAGE);
        ResourceStorage<ResourceVariant<Item>> exposedMulti = TestModContent.TRANSFERS.findBlock(
            StorageKeys.ITEM, helper.getLevel(), helper.absolutePos(multiRelative), Direction.UP
        );
        helper.assertTrue(exposedMulti != null, "Multi-slot block exposure was not found");
        helper.assertValueEqual(exposedMulti.size(), 2, "Multi-slot adapter size");
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(exposedMulti.insert(1, item, 5, transaction), 5L, "Indexed slot insertion");
            transaction.commit();
        }
        helper.assertValueEqual(TestModContent.TEST_MULTI_ITEM_STORAGE.amount(0), 0L, "Untargeted slot amount");
        helper.assertValueEqual(TestModContent.TEST_MULTI_ITEM_STORAGE.amount(1), 5L, "Targeted slot amount");
        ResourceStorage<ResourceVariant<UnitResource>> lateEnergy = TestModContent.TRANSFERS.findBlock(
            StorageKeys.ENERGY,
            helper.getLevel(),
            helper.absolutePos(multiRelative),
            Direction.UP
        );
        helper.assertTrue(lateEnergy != null, "Provider declared after the first apply() was not registered");

        StorageSnapshot<ResourceVariant<Item>> snapshot = StorageSnapshot.capture(TestModContent.TEST_ITEM_STORAGE);
        SimpleSingleSlotStorage<ResourceVariant<Item>> restored =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        helper.assertTrue(snapshot.apply(restored), "ResourceStorage snapshot did not apply");
        helper.assertValueEqual(restored.amount(), 9L, "Serialized/synchronized amount");

        SimpleSingleSlotStorage<ResourceVariant<Item>> moveSource =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 100);
        SimpleSingleSlotStorage<ResourceVariant<Item>> moveTarget =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 40);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            moveSource.insert(item, 100, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(moveSource.moveTo(moveTarget, item, 100), 40L, "Partial-capacity move amount");
        helper.assertValueEqual(moveSource.amount(), 60L, "Partial move source amount");
        helper.assertValueEqual(moveTarget.amount(), 40L, "Partial move target amount");

        SimpleSingleSlotStorage<ResourceVariant<Item>> composedSource =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        SimpleSingleSlotStorage<ResourceVariant<Item>> composedTarget =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            composedSource.insert(item, 10, transaction);
            transaction.commit();
        }
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertValueEqual(
                StorageTransfer.move(composedSource, composedTarget, item, 6, transaction),
                6L,
                "Composed transfer amount"
            );
        }
        helper.assertValueEqual(composedSource.amount(), 10L, "Outer rollback restored transfer source");
        helper.assertValueEqual(composedTarget.amount(), 0L, "Outer rollback restored transfer target");

        SimpleSingleSlotStorage<ResourceVariant<Item>> snapshotSource =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        SimpleSingleSlotStorage<ResourceVariant<Item>> snapshotTarget =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            snapshotSource.insert(item, 3, transaction);
            transaction.commit();
        }
        StorageSnapshot<ResourceVariant<Item>> composedSnapshot = StorageSnapshot.capture(snapshotSource);
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            helper.assertTrue(composedSnapshot.apply(snapshotTarget, transaction), "Composed snapshot application");
            helper.assertValueEqual(snapshotTarget.amount(), 3L, "Snapshot amount inside outer transaction");
        }
        helper.assertValueEqual(snapshotTarget.amount(), 0L, "Outer rollback restored snapshot target");

        SimpleSingleSlotStorage<ResourceVariant<Item>> second =
            new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
        CombinedStorage<ResourceVariant<Item>> combined = CombinedStorage.of(restored, second);
        helper.assertValueEqual(combined.size(), 2, "Combined indexed view size");
        SidedStorage<ResourceVariant<Item>> sided = new SidedStorage<>(Map.of(Direction.UP, restored), second);
        helper.assertTrue(sided.forSide(Direction.UP) == restored, "Sided view");
        helper.assertTrue(sided.forSide(Direction.DOWN) == second, "Unsided fallback view");

        helper.assertValueEqual(
            Units.FLUID_BUCKET.convert(1, Units.FLUID_MILLIBUCKET, RoundingMode.UNNECESSARY),
            1_000L,
            "Fluid unit conversion"
        );
        helper.assertTrue(
            Units.REGISTRY.get(DOUBLE_ENERGY.id()).orElseThrow() == DOUBLE_ENERGY,
            "Custom unit did not resolve to its canonical registry value"
        );
        helper.assertValueEqual(
            GasApi.BUCKET.convert(1, GasApi.UNIT, RoundingMode.UNNECESSARY),
            81_000L,
            "Gas module bucket conversion"
        );
        helper.assertValueEqual(
            SlurryApi.BUCKET.convert(1, SlurryApi.UNIT, RoundingMode.UNNECESSARY),
            81_000L,
            "Slurry module bucket conversion"
        );
        var encodedUnit = Units.REGISTRY.codec().encodeStart(JsonOps.INSTANCE, DOUBLE_ENERGY).getOrThrow();
        helper.assertTrue(
            Units.REGISTRY.codec().parse(JsonOps.INSTANCE, encodedUnit).getOrThrow() == DOUBLE_ENERGY,
            "Unit codec did not resolve the canonical registry value"
        );
        var encodedEnergy = ResourceVariantCodecs.ENERGY.encodeStart(JsonOps.INSTANCE, energy).getOrThrow();
        helper.assertValueEqual(
            ResourceVariantCodecs.ENERGY.parse(JsonOps.INSTANCE, encodedEnergy).getOrThrow(),
            energy,
            "Energy resource codec round-trip"
        );
        boolean rejectedMismatchedUnit = false;
        try {
            StorageKeys.key(id("invalid_fluid_unit"), ResourceTypes.FLUID, Units.ENERGY);
        } catch (IllegalArgumentException ignored) {
            rejectedMismatchedUnit = true;
        }
        helper.assertTrue(rejectedMismatchedUnit, "StorageKey accepted a unit from the wrong dimension");

        ItemStack componentStack = new ItemStack(TestModContent.TEST_LOG_ITEM.get());
        componentStack.set(TestModContent.TEST_NUMBER.get(), 42);
        ResourceVariant<Item> componentVariant = ResourceVariant.ofItem(componentStack);
        helper.assertTrue(componentVariant.hasComponents(), "Item variant did not retain its data components");
        helper.succeed();
    }

    private static <V extends ResourceVariant<?>> void clear(ResourceStorage<V> storage) {
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            for (int index = 0; index < storage.size(); index++) {
                if (storage.amount(index) > 0)
                    storage.extract(index, storage.resource(index), storage.amount(index), transaction);
            }
            transaction.commit();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, path);
    }
}
