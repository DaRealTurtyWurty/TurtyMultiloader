package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeEnergyAdapter;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeMutableItemContext;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeResourceAdapters;
import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeStorageAdapter;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.*;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * NeoForge implementation backed by ResourceHandler, EnergyHandler, and the capability registry.
 */
public final class NeoForgeTransferService implements TransferService {
    private static volatile IEventBus modBus;

    private final Map<StorageKey<?>, CapabilitiesForKey<?>> capabilities = new HashMap<>();
    private final List<CapabilityDeclaration> declarations = new ArrayList<>();
    private boolean applied;
    private boolean registrationClosed;

    public NeoForgeTransferService() {
        registerStorageKey(StorageKeys.ITEM);
        registerStorageKey(StorageKeys.FLUID);
        registerStorageKey(StorageKeys.ENERGY);
    }

    public static void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge transfer service is already bound to a mod event bus");
        modBus = Objects.requireNonNull(bus, "bus");
    }

    @Override
    public synchronized <V extends ResourceVariant<?>> void registerStorageKey(StorageKey<V> key) {
        ensureOpen();
        this.capabilities.computeIfAbsent(key, this::createCapabilities);
    }

    @Override
    @SafeVarargs
    public final <V extends ResourceVariant<?>> void registerBlockProvider(StorageKey<V> key,
                                                                           BlockStorageProvider<V> provider,
                                                                           Supplier<? extends Block>... blocks) {
        ensureOpen();
        CapabilitiesForKey<V> capabilities = capabilities(key);
        this.declarations.add(event -> event.registerBlock(capabilities.block,
            (level, pos, state, blockEntity, side) -> adapt(
                provider.find(level, pos, state, blockEntity, side), capabilities.toNative
            ), supplied(blocks, Block[]::new)));
    }

    @Override
    public <BE extends BlockEntity, V extends ResourceVariant<?>> void registerBlockEntityProvider(
        StorageKey<V> key,
        Supplier<? extends BlockEntityType<BE>> blockEntityType,
        BlockEntityStorageProvider<BE, V> provider
    ) {
        ensureOpen();
        CapabilitiesForKey<V> capabilities = capabilities(key);
        this.declarations.add(event -> event.registerBlockEntity(capabilities.block, blockEntityType.get(),
            (blockEntity, side) -> adapt(provider.find(blockEntity, side), capabilities.toNative)));
    }

    @Override
    public <E extends Entity, V extends ResourceVariant<?>> void registerEntityProvider(
        StorageKey<V> key,
        Supplier<? extends EntityType<E>> entityType,
        EntityStorageProvider<E, V> provider
    ) {
        ensureOpen();
        CapabilitiesForKey<V> capabilities = capabilities(key);
        this.declarations.add(event -> event.registerEntity(capabilities.entity, entityType.get(),
            (entity, nativeContext) -> adapt(provider.find(
                entity, capabilities.entityContext.apply(nativeContext)
            ), capabilities.toNative)));
    }

    @Override
    @SafeVarargs
    public final <V extends ResourceVariant<?>> void registerItemProvider(StorageKey<V> key,
                                                                          ItemStorageProvider<V> provider,
                                                                          Supplier<? extends Item>... items) {
        ensureOpen();
        CapabilitiesForKey<V> capabilities = capabilities(key);
        this.declarations.add(event -> event.registerItem(capabilities.item,
            (stack, itemAccess) -> adapt(
                provider.find(new NeoForgeMutableItemContext(stack, itemAccess)), capabilities.toNative
            ), supplied(items, Item[]::new)));
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findBlock(StorageKey<V> key, Level level, BlockPos pos,
                                                                       BlockState state, BlockEntity blockEntity, Direction side) {
        CapabilitiesForKey<V> capabilities = capabilities(key);
        Object found = state == null
            ? level.getCapability(capabilities.block, pos, side)
            : level.getCapability(capabilities.block, pos, state, blockEntity, side);
        return found == null ? null : capabilities.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findEntity(StorageKey<V> key, Entity entity,
                                                                        EntityStorageContext context) {
        CapabilitiesForKey<V> capabilities = capabilities(key);
        Object found = entity.getCapability(capabilities.entity, capabilities.nativeEntityContext.apply(context));
        return found == null ? null : capabilities.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> ResourceStorage<V> findItem(
        StorageKey<V> key,
        MutableItemContext context
    ) {
        CapabilitiesForKey<V> capabilities = capabilities(key);
        ItemAccess itemAccess = context instanceof NeoForgeMutableItemContext neoforgeContext
            ? neoforgeContext.itemAccess()
            : NeoForgeMutableItemContext.toNeoForge(context);
        Object found = context.stack().getCapability(capabilities.item, itemAccess);
        return found == null ? null : capabilities.fromNative.apply(found);
    }

    @Override
    public <V extends ResourceVariant<?>> BlockStorageCache<V> createBlockCache(StorageKey<V> key, ServerLevel level,
                                                                                BlockPos pos, Direction side) {
        CapabilitiesForKey<V> capabilities = capabilities(key);
        AtomicBoolean open = new AtomicBoolean(true);
        BlockCapabilityCache<Object, Direction> nativeCache = BlockCapabilityCache.create(
            capabilities.block,
            level,
            pos,
            side,
            open::get,
            () -> {
            }
        );
        return new BlockStorageCache<>() {
            @Override
            public ResourceStorage<V> find() {
                if (!open.get())
                    throw new IllegalStateException("Cannot use a closed block storage cache");
                Object found = nativeCache.getCapability();
                return found == null ? null : capabilities.fromNative.apply(found);
            }

            @Override
            public void invalidate() {
                if (open.get())
                    level.invalidateCapabilities(pos);
            }

            @Override
            public void close() {
                if (open.getAndSet(false))
                    level.invalidateCapabilities(pos);
            }
        };
    }

    @Override
    public void invalidateBlock(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
    }

    @Override
    public synchronized void apply() {
        if (this.applied)
            return;
        IEventBus bus = modBus;
        if (bus == null)
            throw new IllegalStateException("NeoForge transfer service has not been bound to a mod event bus");
        this.applied = true;
        bus.addListener(RegisterCapabilitiesEvent.class, this::registerCapabilities);
    }

    @Override
    public boolean isApplied() {
        return this.applied;
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        this.registrationClosed = true;
        this.declarations.forEach(declaration -> declaration.register(event));
    }

    private void ensureOpen() {
        if (this.registrationClosed)
            throw new IllegalStateException("The NeoForge capability registration event has already fired");
    }

    @SuppressWarnings("unchecked")
    private <V extends ResourceVariant<?>> CapabilitiesForKey<V> capabilities(StorageKey<V> key) {
        CapabilitiesForKey<?> result = this.capabilities.get(key);
        if (result == null) {
            registerStorageKey(key);
            result = this.capabilities.get(key);
        }
        return (CapabilitiesForKey<V>) result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CapabilitiesForKey<?> createCapabilities(StorageKey<?> rawKey) {
        StorageKey key = rawKey;
        BlockCapability block;
        ItemCapability item;
        EntityCapability entity;
        Function<Object, ResourceStorage> fromNative;
        Function<ResourceStorage, Object> toNative;
        Function<Object, EntityStorageContext> entityContext;
        Function<EntityStorageContext, Object> nativeEntityContext;

        if (key.equals(StorageKeys.ITEM)) {
            block = Capabilities.Item.BLOCK;
            item = Capabilities.Item.ITEM;
            entity = Capabilities.Item.ENTITY_AUTOMATION;
            fromNative = nativeHandler -> NeoForgeStorageAdapter.fromNeoForgeItems((ResourceHandler<ItemResource>) nativeHandler);
            toNative = neutral -> NeoForgeStorageAdapter.toNeoForge(neutral,
                value -> NeoForgeResourceAdapters.fromNeoForge((ItemResource) value),
                value -> NeoForgeResourceAdapters.toNeoForgeItem((ResourceVariant<Item>) value),
                key.unit(), Units.ITEM);
            entityContext = context -> new EntityStorageContext((Direction) context, null);
            nativeEntityContext = EntityStorageContext::side;
        } else if (key.equals(StorageKeys.FLUID)) {
            block = Capabilities.Fluid.BLOCK;
            item = Capabilities.Fluid.ITEM;
            entity = Capabilities.Fluid.ENTITY;
            fromNative = nativeHandler -> NeoForgeStorageAdapter.fromNeoForgeFluids(
                (ResourceHandler<FluidResource>) nativeHandler, key.unit());
            toNative = neutral -> NeoForgeStorageAdapter.toNeoForge(neutral,
                value -> NeoForgeResourceAdapters.fromNeoForge((FluidResource) value),
                value -> NeoForgeResourceAdapters.toNeoForgeFluid((ResourceVariant<Fluid>) value),
                key.unit(), Units.FLUID_MILLIBUCKET);
            entityContext = context -> new EntityStorageContext((Direction) context, null);
            nativeEntityContext = EntityStorageContext::side;
        } else if (key.equals(StorageKeys.ENERGY)) {
            block = Capabilities.Energy.BLOCK;
            item = Capabilities.Energy.ITEM;
            entity = Capabilities.Energy.ENTITY;
            fromNative = nativeHandler -> NeoForgeEnergyAdapter.fromNeoForge((EnergyHandler) nativeHandler);
            toNative = neutral -> NeoForgeEnergyAdapter.toNeoForge(
                (ResourceStorage<ResourceVariant<UnitResource>>) neutral
            );
            entityContext = context -> new EntityStorageContext((Direction) context, null);
            nativeEntityContext = EntityStorageContext::side;
        } else {
            block = BlockCapability.createSided(key.id(), ResourceHandler.asClass());
            item = ItemCapability.create(key.id(), ResourceHandler.asClass(), ItemAccess.class);
            entity = EntityCapability.create(key.id(), ResourceHandler.asClass(), EntityStorageContext.class);
            fromNative = nativeHandler -> NeoForgeStorageAdapter.fromNeoForge(
                (ResourceHandler<NeoForgeResourceAdapters.NeutralResource<ResourceVariant<?>>>) nativeHandler,
                NeoForgeResourceAdapters.NeutralResource::variant,
                NeoForgeResourceAdapters.NeutralResource::new, key.unit(), key.unit());
            toNative = neutral -> NeoForgeStorageAdapter.toNeoForge(neutral,
                value -> ((NeoForgeResourceAdapters.NeutralResource<ResourceVariant<?>>) value).variant(),
                NeoForgeResourceAdapters.NeutralResource::new, key.unit(), key.unit());
            entityContext = context -> (EntityStorageContext) context;
            nativeEntityContext = context -> context;
        }
        return new CapabilitiesForKey(block, item, entity, fromNative, toNative, entityContext, nativeEntityContext);
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

    @FunctionalInterface
    private interface CapabilityDeclaration {
        void register(RegisterCapabilitiesEvent event);
    }

    private record CapabilitiesForKey<V extends ResourceVariant<?>>(
        BlockCapability<Object, Direction> block,
        ItemCapability<Object, ItemAccess> item,
        EntityCapability<Object, Object> entity,
        Function<Object, ResourceStorage<V>> fromNative,
        Function<ResourceStorage<V>, Object> toNative,
        Function<Object, EntityStorageContext> entityContext,
        Function<EntityStorageContext, Object> nativeEntityContext
    ) {
    }
}
