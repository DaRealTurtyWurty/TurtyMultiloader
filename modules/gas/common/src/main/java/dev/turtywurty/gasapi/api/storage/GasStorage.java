package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.BlockStorageProvider;
import dev.turtywurty.turtymultiloader.transfer.lookup.ItemStorageProvider;
import dev.turtywurty.turtymultiloader.transfer.lookup.MutableItemContext;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKey;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Loader-neutral gas exposure and lookup helpers.
 */
public final class GasStorage {
    public static final StorageKey<ResourceVariant<Gas>> KEY = GasApi.RESOURCE_FAMILY.storageKey();

    private GasStorage() {
    }

    @SafeVarargs
    public static void registerBlockProvider(
        BlockStorageProvider<ResourceVariant<Gas>> provider,
        Supplier<? extends Block>... blocks
    ) {
        TransferService.get().registerBlockProvider(KEY, provider, blocks);
    }

    @SafeVarargs
    public static void registerItemProvider(
        ItemStorageProvider<ResourceVariant<Gas>> provider,
        Supplier<? extends Item>... items
    ) {
        TransferService.get().registerItemProvider(KEY, provider, items);
    }

    public static ResourceStorage<ResourceVariant<Gas>> findBlock(
        Level level,
        BlockPos pos,
        Direction side
    ) {
        return TransferService.get().findBlock(KEY, level, pos, side);
    }

    public static ResourceStorage<ResourceVariant<Gas>> findItem(
        ItemStack stack,
        MutableItemContext context
    ) {
        return TransferService.get().findItem(KEY, stack, context);
    }
}
