package dev.turtywurty.turtymultiloader.transfer;

import dev.turtywurty.turtymultiloader.transfer.lookup.*;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Loader-neutral storage exposure and lookup service. Provider declarations are applied at loader-safe timing.
 *
 * <p>On Fabric, declarations made after the first {@link #apply()} are registered immediately. On NeoForge,
 * declarations remain queued after {@code apply()} until the capability-registration event, and attempts made after
 * that event are rejected. Calling {@code apply()} more than once is always safe and has no additional effect.</p>
 */
public interface TransferService {
    static TransferService get() {
        return ServiceHolder.INSTANCE;
    }

    <V extends ResourceVariant<?>> void registerStorageKey(StorageKey<V> key);

    <V extends ResourceVariant<?>> void registerBlockProvider(
        StorageKey<V> key,
        BlockStorageProvider<V> provider,
        Supplier<? extends Block>... blocks
    );

    <BE extends BlockEntity, V extends ResourceVariant<?>> void registerBlockEntityProvider(
        StorageKey<V> key,
        Supplier<? extends BlockEntityType<BE>> blockEntityType,
        BlockEntityStorageProvider<BE, V> provider
    );

    <E extends Entity, V extends ResourceVariant<?>> void registerEntityProvider(
        StorageKey<V> key,
        Supplier<? extends EntityType<E>> entityType,
        EntityStorageProvider<E, V> provider
    );

    <V extends ResourceVariant<?>> void registerItemProvider(
        StorageKey<V> key,
        ItemStorageProvider<V> provider,
        Supplier<? extends Item>... items
    );

    <V extends ResourceVariant<?>> ResourceStorage<V> findBlock(
        StorageKey<V> key,
        Level level,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        Direction side
    );

    default <V extends ResourceVariant<?>> ResourceStorage<V> findBlock(
        StorageKey<V> key,
        Level level,
        BlockPos pos,
        Direction side
    ) {
        return findBlock(key, level, pos, null, null, side);
    }

    <V extends ResourceVariant<?>> ResourceStorage<V> findEntity(
        StorageKey<V> key,
        Entity entity,
        EntityStorageContext context
    );

    <V extends ResourceVariant<?>> ResourceStorage<V> findItem(
        StorageKey<V> key,
        MutableItemContext context
    );

    <V extends ResourceVariant<?>> BlockStorageCache<V> createBlockCache(
        StorageKey<V> key,
        ServerLevel level,
        BlockPos pos,
        Direction side
    );

    /**
     * Requests invalidation of loader-native lookup state for a block. Fabric's native cache lifecycle already tracks
     * block and block-entity replacement and therefore needs no additional action.
     */
    void invalidateBlock(Level level, BlockPos pos);

    void apply();

    boolean isApplied();

    final class ServiceHolder {
        private static final TransferService INSTANCE = ServiceLoader.load(
                TransferService.class,
                TransferService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No transfer service is available"));

        private ServiceHolder() {
        }
    }
}
