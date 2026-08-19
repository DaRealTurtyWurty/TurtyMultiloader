package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.registration.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NeoForgeRegistryService implements RegistryService {
    private static volatile IEventBus modBus;
    private static volatile boolean registrationClosed;

    private final List<CustomRegistryDeclaration<?>> customRegistries = new ArrayList<>();
    private final List<EntryDeclaration<?, ?>> entries = new ArrayList<>();
    private final List<EntityAttributesDeclaration<?>> entityAttributes = new ArrayList<>();
    private final List<CreativeTabPopulation> creativeTabPopulations = new ArrayList<>();
    private final List<WoodTypeDeclaration> woodTypes = new ArrayList<>();
    private final List<StrippableDeclaration> strippables = new ArrayList<>();
    private final List<FlammabilityDeclaration> flammability = new ArrayList<>();
    private boolean applied;
    private int appliedCustomRegistries;
    private int appliedEntries;
    private boolean attributesListenerRegistered;
    private boolean creativeTabsListenerRegistered;
    private boolean blockHooksListenerRegistered;

    public static void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge registry service is already bound to a mod event bus");

        modBus = Objects.requireNonNull(bus, "bus");
        bus.addListener(RegisterCapabilitiesEvent.class, NeoForgeRegistryService::closeRegistration);
    }

    @Override
    public synchronized <R, T extends R> RegistrationHandle<R, T> register(
        ResourceKey<? extends Registry<R>> registryKey,
        Identifier id,
        Supplier<? extends T> factory
    ) {
        ensureOpen();
        ResourceKey<R> entryKey = ResourceKey.create(
            Objects.requireNonNull(registryKey, "registryKey"),
            Objects.requireNonNull(id, "id")
        );
        RegistrationHandle<R, T> result = new RegistrationHandle<>(id, entryKey);
        entries.add(new EntryDeclaration<>(registryKey, id, Objects.requireNonNull(factory, "factory"), result));
        return result;
    }

    @Override
    public synchronized <T> CustomRegistry<T> customRegistry(
        ResourceKey<Registry<T>> key,
        CustomRegistryOptions options
    ) {
        ensureOpen();
        CustomRegistry<T> result = new CustomRegistry<>(this, Objects.requireNonNull(key, "key"));
        customRegistries.add(new CustomRegistryDeclaration<>(result, Objects.requireNonNull(options, "options")));
        return result;
    }

    @Override
    public synchronized <T extends LivingEntity> void registerEntityAttributes(
        RegistrationHandle<EntityType<?>, EntityType<T>> entityType,
        Supplier<AttributeSupplier> attributes
    ) {
        ensureOpen();
        entityAttributes.add(new EntityAttributesDeclaration<>(
            Objects.requireNonNull(entityType, "entityType"),
            Objects.requireNonNull(attributes, "attributes")
        ));
    }

    @Override
    public synchronized void populateCreativeTab(
        ResourceKey<CreativeModeTab> tab,
        Consumer<CreativeTabOutput> population
    ) {
        ensureOpen();
        creativeTabPopulations.add(new CreativeTabPopulation(
            Objects.requireNonNull(tab, "tab"),
            Objects.requireNonNull(population, "population")
        ));
    }

    @Override
    public synchronized QueuedValue<WoodType> registerWoodType(Supplier<? extends WoodType> factory) {
        ensureOpen();
        QueuedValue<WoodType> result = new QueuedValue<>();
        woodTypes.add(new WoodTypeDeclaration(Objects.requireNonNull(factory, "factory"), result));
        return result;
    }

    @Override
    public synchronized void registerStrippable(
        Supplier<? extends Block> block,
        Supplier<? extends Block> stripped
    ) {
        ensureOpen();
        strippables.add(new StrippableDeclaration(
            Objects.requireNonNull(block, "block"),
            Objects.requireNonNull(stripped, "stripped")
        ));
    }

    @Override
    public synchronized void registerFlammable(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
        ensureOpen();
        if (igniteOdds < 0 || burnOdds < 0)
            throw new IllegalArgumentException("Flammability odds must be non-negative");

        flammability.add(new FlammabilityDeclaration(
            Objects.requireNonNull(block, "block"),
            igniteOdds,
            burnOdds
        ));
    }

    @Override
    public synchronized void apply() {
        ensureOpen();

        IEventBus bus = modBus;
        if (bus == null)
            throw new IllegalStateException("NeoForge registry service has not been bound to a mod event bus");

        applied = true;
        Map<RegistrarKey, DeferredRegister<?>> registrars = new LinkedHashMap<>();
        customRegistries.subList(appliedCustomRegistries, customRegistries.size())
            .forEach(declaration -> createCustomRegistry(declaration, registrars));
        entries.subList(appliedEntries, entries.size()).forEach(declaration -> queueEntry(declaration, registrars));
        registrars.values().forEach(registrar -> registrar.register(bus));
        appliedCustomRegistries = customRegistries.size();
        appliedEntries = entries.size();

        if (!entityAttributes.isEmpty() && !attributesListenerRegistered) {
            bus.addListener(EntityAttributeCreationEvent.class, this::registerAttributes);
            attributesListenerRegistered = true;
        }
        if (!creativeTabPopulations.isEmpty() && !creativeTabsListenerRegistered) {
            bus.addListener(BuildCreativeModeTabContentsEvent.class, this::populateCreativeTab);
            creativeTabsListenerRegistered = true;
        }
        if ((!woodTypes.isEmpty() || !strippables.isEmpty() || !flammability.isEmpty())
            && !blockHooksListenerRegistered) {
            bus.addListener(RegisterEvent.class, this::registerBlockHooks);
            blockHooksListenerRegistered = true;
        }
    }

    @Override
    public synchronized boolean isApplied() {
        return applied;
    }

    private static <T> void createCustomRegistry(
        CustomRegistryDeclaration<T> declaration,
        Map<RegistrarKey, DeferredRegister<?>> registrars
    ) {
        ResourceKey<Registry<T>> key = declaration.result().key();
        String namespace = key.identifier().getNamespace();
        DeferredRegister<T> registrar = DeferredRegister.create(key, namespace);
        registrar.makeRegistry(builder -> {
            if (declaration.options().synced())
                builder.sync(true);
            if (declaration.options().intrusiveHolders())
                builder.withIntrusiveHolders();
        });
        declaration.result().bind(registrar.getRegistry());
        registrars.put(new RegistrarKey(key, namespace), registrar);
    }

    @SuppressWarnings("unchecked")
    private static <R, T extends R> void queueEntry(
        EntryDeclaration<R, T> declaration,
        Map<RegistrarKey, DeferredRegister<?>> registrars
    ) {
        String namespace = declaration.id().getNamespace();
        RegistrarKey registrarKey = new RegistrarKey(declaration.registryKey(), namespace);
        DeferredRegister<R> registrar = (DeferredRegister<R>) registrars.computeIfAbsent(
            registrarKey,
            ignored -> DeferredRegister.create(declaration.registryKey(), namespace)
        );
        DeferredHolder<R, T> holder = registrar.register(declaration.id().getPath(), declaration.factory());
        declaration.result().bind(holder, holder);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        entityAttributes.forEach(declaration -> registerAttributes(event, declaration));
    }

    private static <T extends LivingEntity> void registerAttributes(
        EntityAttributeCreationEvent event,
        EntityAttributesDeclaration<T> declaration
    ) {
        event.put(declaration.entityType().get(), declaration.attributes().get());
    }

    private void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        creativeTabPopulations.stream()
            .filter(declaration -> declaration.tab().equals(event.getTabKey()))
            .forEach(declaration -> declaration.population().accept(stack -> event.accept(stack)));
    }

    private boolean blockHooksRegistered;

    private void registerBlockHooks(RegisterEvent event) {
        if (blockHooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK))
            return;

        blockHooksRegistered = true;
        woodTypes.forEach(declaration -> declaration.result().bind(declaration.factory().get()));
        strippables.forEach(declaration -> VanillaRegistryHooks.registerStrippable(
            declaration.block().get(),
            declaration.stripped().get()
        ));
        flammability.forEach(declaration -> VanillaRegistryHooks.registerFlammable(
            declaration.block().get(),
            declaration.igniteOdds(),
            declaration.burnOdds()
        ));
    }

    private void ensureOpen() {
        if (registrationClosed)
            throw new IllegalStateException("NeoForge registration has already reached capability registration");
    }

    private static void closeRegistration(RegisterCapabilitiesEvent ignored) {
        registrationClosed = true;
    }

    private record RegistrarKey(ResourceKey<? extends Registry<?>> registryKey, String namespace) {
    }

    private record CustomRegistryDeclaration<T>(CustomRegistry<T> result, CustomRegistryOptions options) {
    }

    private record EntryDeclaration<R, T extends R>(
        ResourceKey<? extends Registry<R>> registryKey,
        Identifier id,
        Supplier<? extends T> factory,
        RegistrationHandle<R, T> result
    ) {
    }

    private record EntityAttributesDeclaration<T extends LivingEntity>(
        RegistrationHandle<EntityType<?>, EntityType<T>> entityType,
        Supplier<AttributeSupplier> attributes
    ) {
    }

    private record CreativeTabPopulation(
        ResourceKey<CreativeModeTab> tab,
        Consumer<CreativeTabOutput> population
    ) {
    }

    private record WoodTypeDeclaration(Supplier<? extends WoodType> factory, QueuedValue<WoodType> result) {
    }

    private record StrippableDeclaration(Supplier<? extends Block> block, Supplier<? extends Block> stripped) {
    }

    private record FlammabilityDeclaration(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
    }
}
