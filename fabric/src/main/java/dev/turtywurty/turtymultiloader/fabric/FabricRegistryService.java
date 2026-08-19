package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.serialization.Lifecycle;
import dev.turtywurty.turtymultiloader.registration.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FabricRegistryService implements RegistryService {
    private final List<CustomRegistryDeclaration<?>> customRegistries = new ArrayList<>();
    private final List<EntryDeclaration<?, ?>> entries = new ArrayList<>();
    private final List<EntityAttributesDeclaration<?>> entityAttributes = new ArrayList<>();
    private final List<CreativeTabPopulation> creativeTabPopulations = new ArrayList<>();
    private final List<WoodTypeDeclaration> woodTypes = new ArrayList<>();
    private final List<StrippableDeclaration> strippables = new ArrayList<>();
    private final List<FlammabilityDeclaration> flammability = new ArrayList<>();
    private boolean applied;

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
        applied = true;
        customRegistries.forEach(FabricRegistryService::createCustomRegistry);
        customRegistries.clear();
        entries.forEach(FabricRegistryService::registerEntry);
        entries.clear();
        entityAttributes.forEach(FabricRegistryService::registerAttributes);
        entityAttributes.clear();
        creativeTabPopulations.forEach(FabricRegistryService::registerCreativeTabPopulation);
        creativeTabPopulations.clear();
        woodTypes.forEach(declaration -> declaration.result().bind(declaration.factory().get()));
        woodTypes.clear();
        strippables.forEach(declaration -> StrippableBlockRegistry.register(
            declaration.block().get(),
            declaration.stripped().get()
        ));
        strippables.clear();
        flammability.forEach(declaration -> FlammableBlockRegistry.getDefaultInstance().add(
            declaration.block().get(),
            declaration.igniteOdds(),
            declaration.burnOdds()
        ));
        flammability.clear();
    }

    @Override
    public synchronized boolean isApplied() {
        return applied;
    }

    private static <T> void createCustomRegistry(CustomRegistryDeclaration<T> declaration) {
        ResourceKey<Registry<T>> key = declaration.result().key();
        MappedRegistry<T> registry = new MappedRegistry<>(
            key,
            Lifecycle.stable(),
            declaration.options().intrusiveHolders()
        );
        FabricRegistryBuilder<T, MappedRegistry<T>> builder = FabricRegistryBuilder.from(registry)
            .attribute(RegistryAttribute.MODDED);
        if (declaration.options().synced())
            builder.attribute(RegistryAttribute.SYNCED);

        MappedRegistry<T> registered = builder.buildAndRegister();
        declaration.result().bind(() -> registered);
    }

    @SuppressWarnings("unchecked")
    private static <R, T extends R> void registerEntry(EntryDeclaration<R, T> declaration) {
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(
            declaration.registryKey().identifier()
        );
        if (registry == null)
            throw new IllegalStateException("Unknown registry " + declaration.registryKey().identifier());

        T value = declaration.factory().get();
        Holder.Reference<T> holder = Registry.registerForHolder(registry, declaration.result().key(), value);
        declaration.result().bind((Holder<R>) holder, () -> value);
    }

    private static <T extends LivingEntity> void registerAttributes(EntityAttributesDeclaration<T> declaration) {
        FabricDefaultAttributeRegistry.register(declaration.entityType().get(), declaration.attributes().get());
    }

    private static void registerCreativeTabPopulation(CreativeTabPopulation declaration) {
        CreativeModeTabEvents.modifyOutputEvent(declaration.tab()).register(output ->
            declaration.population().accept(stack -> output.accept(stack))
        );
    }

    private void ensureOpen() {
        // Fabric can flush another declaration batch while mod entrypoints are still running.
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
