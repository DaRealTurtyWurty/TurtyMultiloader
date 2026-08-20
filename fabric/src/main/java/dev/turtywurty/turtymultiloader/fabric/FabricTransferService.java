package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.fabric.transfer.FabricEnergyAdapter;
import dev.turtywurty.turtymultiloader.fabric.transfer.FabricMutableItemContext;
import dev.turtywurty.turtymultiloader.fabric.transfer.FabricResourceAdapters;
import dev.turtywurty.turtymultiloader.fabric.transfer.FabricStorageAdapter;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.*;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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
import net.minecraft.world.level.material.Fluid;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Fabric implementation backed by BlockApiLookup, ItemApiLookup, EntityApiLookup, and Fabric Storage.
 */
public final class FabricTransferService implements TransferService {
    private final Map<StorageKey<?>, Lookups<?>> lookups = new HashMap<>();
    private final List<Runnable> declarations = new ArrayList<>();
    private boolean applied;

    public FabricTransferService() {
        registerStorageKey(StorageKeys.ITEM);
        registerStorageKey(StorageKeys.FLUID);
        registerStorageKey(StorageKeys.ENERGY);
    }

    @Override
    public synchronized <V extends ResourceVariant<?>> void registerStorageKey(StorageKey<V> key) {
        this.lookups.computeIfAbsent(key, this::createLookups);
    }

    @Override
    @SafeVarargs
    public final <V extends ResourceVariant<?>> void registerBlockProvider(
        StorageKey<V> key,
        BlockStorageProvider<V> provider,
        Supplier<? extends Block>... blocks
    ) {
        Lookups<V> lookups = lookups(key);
        declare(() -> lookups.block.registerForBlocks(
            (level, pos, state, blockEntity, side) -> adapt(
                provider.find(level, pos, state, blockEntity, side), lookups.toNative
            ), supplied(blocks, Block[]::new)
        ));
    }

    @Override
    public <BE extends BlockEntity, V extends ResourceVariant<?>> void registerBlockEntityProvider(
        StorageKey<V> key,
        Supplier<? extends BlockEntityType<BE>> blockEntityType,
        BlockEntityStorageProvider<BE, V> provider
    ) {
        Lookups<V> lookups = lookups(key);
        declare(() -> lookups.block.registerForBlockEntity(
            (blockEntity, side) -> adapt(provider.find(blockEntity, side), lookups.toNative),
            blockEntityType.get()
        ));
    }

    @Override
    public <E extends Entity, V extends ResourceVariant<?>> void registerEntityProvider(
        StorageKey<V> key,
        Supplier<? extends EntityType<E>> entityType,
        EntityStorageProvider<E, V> provider
    ) {
        Lookups<V> lookups = lookups(key);
        declare(() -> lookups.entity.registerForType(
            (entity, context) -> adapt(provider.find(entity, context), lookups.toNative), entityType.get()
        ));
    }

    @Override
    @SafeVarargs
    public final <V extends ResourceVariant<?>> void registerItemProvider(
        StorageKey<V> key,
        ItemStorageProvider<V> provider,
        Supplier<? extends Item>... items
    ) {
        Lookups<V> lookups = lookups(key);
        declare(() -> lookups.item.registerForItems(
            (stack, context) -> adapt(
                provider.find(new FabricMutableItemContext(stack, context)), lookups.toNative
            ),
            supplied(items, Item[]::new)
        ));
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findBlock(StorageKey<V> key, Level level, BlockPos pos,
                                                                       BlockState state, BlockEntity blockEntity, Direction side) {
        Lookups<V> lookups = lookups(key);
        Object found = lookups.block.find(level, pos, state, blockEntity, side);
        return found == null ? null : lookups.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findEntity(StorageKey<V> key, Entity entity,
                                                                        EntityStorageContext context) {
        Lookups<V> lookups = lookups(key);
        Object found = lookups.entity.find(entity, context);
        return found == null ? null : lookups.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findItem(
        StorageKey<V> key,
        MutableItemContext context
    ) {
        Lookups<V> lookups = lookups(key);
        ContainerItemContext nativeContext = context instanceof FabricMutableItemContext fabricContext
            ? fabricContext.fabricContext()
            : FabricMutableItemContext.toFabric(context);
        Object found = lookups.item.find(context.stack(), nativeContext);
        return found == null ? null : lookups.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> BlockStorageCache<V> createBlockCache(StorageKey<V> key, ServerLevel level,
                                                                                BlockPos pos, Direction side) {
        Lookups<V> lookups = lookups(key);
        BlockApiCache<Object, Direction> nativeCache = BlockApiCache.create(lookups.block, level, pos);
        return new CachedLookup<>(nativeCache, lookups.fromNative, side);
    }

    @Override
    public void invalidateBlock(Level level, BlockPos pos) {
        // BlockApiCache automatically invalidates its block entity and provider caches when blocks or block entities
        // are replaced, loaded, or unloaded. It never retains the resolved API value, so no manual invalidation is
        // necessary for Fabric.
    }

    @Override
    public synchronized void apply() {
        this.applied = true;
        while (!this.declarations.isEmpty()) {
            this.declarations.getFirst().run();
            this.declarations.removeFirst();
        }
    }

    @Override
    public boolean isApplied() {
        return this.applied;
    }

    private synchronized void declare(Runnable declaration) {
        if (this.applied)
            declaration.run();
        else
            this.declarations.add(declaration);
    }

    @SuppressWarnings("unchecked")
    private <V extends ResourceVariant<?>> Lookups<V> lookups(StorageKey<V> key) {
        Lookups<?> result = this.lookups.get(key);
        if (result == null) {
            registerStorageKey(key);
            result = this.lookups.get(key);
        }
        return (Lookups<V>) result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Lookups<?> createLookups(StorageKey<?> rawKey) {
        StorageKey key = rawKey;
        BlockApiLookup block;
        ItemApiLookup item;
        EntityApiLookup entity;
        Function<Object, ResourceStorage> fromNative;
        Function<ResourceStorage, Object> toNative;

        if (key.equals(StorageKeys.ITEM)) {
            block = ItemStorage.SIDED;
            item = ItemStorage.ITEM;
            fromNative = nativeStorage -> FabricStorageAdapter.fromFabricItems(
                (Storage) nativeStorage);
            toNative = neutral -> FabricStorageAdapter.toFabric(neutral,
                value -> FabricResourceAdapters.fromFabric((ItemVariant) value),
                value -> FabricResourceAdapters.toFabricItem((ResourceVariant<Item>) value),
                value -> ((ItemVariant) value).isBlank());
            entity = EntityApiLookup.get(key.id(), Storage.asClass(), EntityStorageContext.class);
        } else if (key.equals(StorageKeys.FLUID)) {
            block = FluidStorage.SIDED;
            item = FluidStorage.ITEM;
            fromNative = nativeStorage -> FabricStorageAdapter.fromFabricFluids(
                (Storage) nativeStorage);
            toNative = neutral -> FabricStorageAdapter.toFabric(neutral,
                value -> FabricResourceAdapters.fromFabric((FluidVariant) value),
                value -> FabricResourceAdapters.toFabricFluid((ResourceVariant<Fluid>) value),
                value -> ((FluidVariant) value).isBlank());
            entity = EntityApiLookup.get(key.id(), Storage.asClass(), EntityStorageContext.class);
        } else if (key.equals(StorageKeys.ENERGY)) {
            block = EnergyStorage.SIDED;
            item = EnergyStorage.ITEM;
            fromNative = nativeStorage -> FabricEnergyAdapter.fromFabric((EnergyStorage) nativeStorage);
            toNative = neutral -> FabricEnergyAdapter.toFabric(
                (ResourceStorage<ResourceVariant<UnitResource>>) neutral
            );
            entity = EntityApiLookup.get(key.id(), EnergyStorage.class, EntityStorageContext.class);
        } else {
            block = BlockApiLookup.get(key.id(), Storage.asClass(), Direction.class);
            item = ItemApiLookup.get(key.id(), Storage.asClass(), ContainerItemContext.class);
            fromNative = nativeStorage -> FabricStorageAdapter.fromFabric(
                (Storage<ResourceVariant<?>>) nativeStorage,
                Function.identity(), Function.identity(), () -> (ResourceVariant<?>) key.resourceType().empty());
            toNative = neutral -> FabricStorageAdapter.toFabric(neutral, Function.identity(), Function.identity(),
                value -> ((ResourceVariant<?>) value).isBlank());
            entity = EntityApiLookup.get(key.id(), Storage.asClass(), EntityStorageContext.class);
        }
        return new Lookups(block, item, entity, fromNative, toNative);
    }

    private static <T> T[] supplied(Supplier<? extends T>[] suppliers, IntFunction<T[]> factory) {
        T[] values = factory.apply(suppliers.length);
        for (int index = 0; index < suppliers.length; index++)
            values[index] = suppliers[index].get();
        return values;
    }

    private static <V extends ResourceVariant<?>> Object adapt(ResourceStorage<V> storage, Function<ResourceStorage<V>, Object> adapter) {
        return storage == null ? null : adapter.apply(storage);
    }

    private record Lookups<V extends ResourceVariant<?>>(
        BlockApiLookup<Object, Direction> block,
        ItemApiLookup<Object, ContainerItemContext> item,
        EntityApiLookup<Object, EntityStorageContext> entity,
        Function<Object, ResourceStorage<V>> fromNative,
        Function<ResourceStorage<V>, Object> toNative
    ) {
    }

    private static final class CachedLookup<V extends ResourceVariant<?>> implements BlockStorageCache<V> {
        private final BlockApiCache<Object, Direction> nativeCache;
        private final Function<Object, ResourceStorage<V>> fromNative;
        private final Direction side;
        private boolean closed;

        private CachedLookup(BlockApiCache<Object, Direction> nativeCache,
                             Function<Object, ResourceStorage<V>> fromNative, Direction side) {
            this.nativeCache = nativeCache;
            this.fromNative = fromNative;
            this.side = side;
        }

        @Override
        public ResourceStorage<V> find() {
            if (this.closed)
                throw new IllegalStateException("Cannot use a closed block storage cache");
            Object found = this.nativeCache.find(this.side);
            return found == null ? null : this.fromNative.apply(found);
        }

        @Override
        public void invalidate() {
            // Native Fabric caches retain only the block entity/provider lookup and invalidate those automatically.
            // The resolved API itself is queried on every find().
        }

        @Override
        public void close() {
            if (this.closed)
                return;
            this.closed = true;
        }
    }
}
