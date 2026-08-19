package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.api.Slurry;
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
 * Loader-neutral slurry exposure and lookup helpers.
 */
public final class SlurryStorage {
    public static final StorageKey<ResourceVariant<Slurry>> KEY = SlurryApi.RESOURCE_FAMILY.storageKey();

    private SlurryStorage() {
    }

    @SafeVarargs
    public static void registerBlockProvider(
        BlockStorageProvider<ResourceVariant<Slurry>> provider,
        Supplier<? extends Block>... blocks
    ) {
        TransferService.get().registerBlockProvider(KEY, provider, blocks);
    }

    @SafeVarargs
    public static void registerItemProvider(
        ItemStorageProvider<ResourceVariant<Slurry>> provider,
        Supplier<? extends Item>... items
    ) {
        TransferService.get().registerItemProvider(KEY, provider, items);
    }

    public static ResourceStorage<ResourceVariant<Slurry>> findBlock(Level level, BlockPos pos, Direction side) {
        return TransferService.get().findBlock(KEY, level, pos, side);
    }

    public static ResourceStorage<ResourceVariant<Slurry>> findItem(ItemStack stack, MutableItemContext context) {
        return TransferService.get().findItem(KEY, stack, context);
    }
}
