package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.serialization.Lifecycle;
import dev.turtywurty.turtymultiloader.registration.CreativeTabOutput;
import dev.turtywurty.turtymultiloader.registration.CustomRegistry;
import dev.turtywurty.turtymultiloader.registration.CustomRegistryOptions;
import dev.turtywurty.turtymultiloader.registration.PayloadFlow;
import dev.turtywurty.turtymultiloader.registration.PayloadPhase;
import dev.turtywurty.turtymultiloader.registration.QueuedValue;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
    private final List<PayloadDeclaration<?, ?>> payloads = new ArrayList<>();
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
    public synchronized <B extends FriendlyByteBuf, T extends CustomPacketPayload> void registerPayloadType(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec
    ) {
        ensureOpen();
        payloads.add(new PayloadDeclaration<>(
            Objects.requireNonNull(phase, "phase"),
            Objects.requireNonNull(flow, "flow"),
            Objects.requireNonNull(type, "type"),
            Objects.requireNonNull(codec, "codec")
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
        if (applied)
            return;

        applied = true;
        customRegistries.forEach(FabricRegistryService::createCustomRegistry);
        entries.forEach(FabricRegistryService::registerEntry);
        entityAttributes.forEach(FabricRegistryService::registerAttributes);
        creativeTabPopulations.forEach(FabricRegistryService::registerCreativeTabPopulation);
        payloads.forEach(FabricRegistryService::registerPayload);
        woodTypes.forEach(declaration -> declaration.result().bind(declaration.factory().get()));
        strippables.forEach(declaration -> StrippableBlockRegistry.register(
            declaration.block().get(),
            declaration.stripped().get()
        ));
        flammability.forEach(declaration -> FlammableBlockRegistry.getDefaultInstance().add(
            declaration.block().get(),
            declaration.igniteOdds(),
            declaration.burnOdds()
        ));
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerPayload(PayloadDeclaration<?, ?> declaration) {
        PayloadTypeRegistry clientbound;
        PayloadTypeRegistry serverbound;
        if (declaration.phase() == PayloadPhase.PLAY) {
            clientbound = PayloadTypeRegistry.clientboundPlay();
            serverbound = PayloadTypeRegistry.serverboundPlay();
        } else {
            clientbound = PayloadTypeRegistry.clientboundConfiguration();
            serverbound = PayloadTypeRegistry.serverboundConfiguration();
        }

        if (declaration.flow() != PayloadFlow.SERVERBOUND)
            clientbound.register(declaration.type(), declaration.codec());
        if (declaration.flow() != PayloadFlow.CLIENTBOUND)
            serverbound.register(declaration.type(), declaration.codec());
    }

    private void ensureOpen() {
        if (applied)
            throw new IllegalStateException("Registry service has already been applied");
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

    private record PayloadDeclaration<B extends FriendlyByteBuf, T extends CustomPacketPayload>(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec
    ) {
    }

    private record WoodTypeDeclaration(Supplier<? extends WoodType> factory, QueuedValue<WoodType> result) {
    }

    private record StrippableDeclaration(Supplier<? extends Block> block, Supplier<? extends Block> stripped) {
    }

    private record FlammabilityDeclaration(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
    }
}
