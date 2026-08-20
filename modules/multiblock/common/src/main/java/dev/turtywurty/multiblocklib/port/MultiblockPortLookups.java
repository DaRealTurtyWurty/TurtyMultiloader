package dev.turtywurty.multiblocklib.port;

import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.world.MultiblockWorldData;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public final class MultiblockPortLookups {
    private MultiblockPortLookups() {
    }

    public static void register() {
        registerBlock(MultiblockLib.MULTIBLOCK_PART_HANDLE);
        registerBlock(MultiblockLib.MULTIBLOCK_CONTROLLER_HANDLE);
    }

    public static void registerControllerBlock(final Block controllerBlock) {
        registerBlock(controllerBlock);
    }

    private static void registerBlock(final Block block) {
        registerBlock(() -> block);
    }

    private static void registerBlock(final java.util.function.Supplier<? extends Block> block) {
        TransferService transfers = TransferService.get();
        transfers.registerBlockProvider(StorageKeys.ITEM, MultiblockPortLookups::findItemStorage, block);
        transfers.registerBlockProvider(StorageKeys.FLUID, MultiblockPortLookups::findFluidStorage, block);
        transfers.registerBlockProvider(StorageKeys.ENERGY, MultiblockPortLookups::findEnergyStorage, block);
    }

    public static ResourceStorage<ResourceVariant<Item>> findItemStorage(
        final Level level, final BlockPos pos, final BlockState state,
        final BlockEntity blockEntity, final Direction direction
    ) {
        MultiblockControllerBlockEntity controller = resolveController(level, pos, blockEntity);
        return controller != null ? controller.getItemStorageForExternal(pos) : null;
    }

    public static ResourceStorage<ResourceVariant<Fluid>> findFluidStorage(
        final Level level, final BlockPos pos, final BlockState state,
        final BlockEntity blockEntity, final Direction direction
    ) {
        MultiblockControllerBlockEntity controller = resolveController(level, pos, blockEntity);
        return controller != null ? controller.getFluidStorageForExternal(pos) : null;
    }

    public static ResourceStorage<ResourceVariant<UnitResource>> findEnergyStorage(
        final Level level, final BlockPos pos, final BlockState state,
        final BlockEntity blockEntity, final Direction direction
    ) {
        MultiblockControllerBlockEntity controller = resolveController(level, pos, blockEntity);
        return controller != null ? controller.getEnergyStorageForExternal(pos) : null;
    }

    private static MultiblockControllerBlockEntity resolveController(
        final Level level, final BlockPos pos, final BlockEntity blockEntity
    ) {
        if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
            return controller;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        MultiblockWorldData data = MultiblockWorldData.get(serverLevel);
        BlockPos controllerPos = data.getControllerFor(pos);
        if (controllerPos == null) {
            return null;
        }
        BlockEntity controllerEntity = level.getBlockEntity(controllerPos);
        if (controllerEntity instanceof MultiblockControllerBlockEntity controller) {
            return controller;
        }
        data.restorePartsForController(serverLevel, controllerPos);
        return null;
    }
}
