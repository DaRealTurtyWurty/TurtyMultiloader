package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.material.Fluid;

import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-neutral registry service. Every method queues work; {@link #apply()} attaches or performs it at the
 * loader-appropriate registration point.
 */
public interface RegistryService {
    static RegistryService get() {
        return ServiceHolder.INSTANCE;
    }

    <R, T extends R> RegistrationHandle<R, T> register(
        ResourceKey<? extends Registry<R>> registryKey,
        Identifier id,
        Supplier<? extends T> factory
    );

    <T> CustomRegistry<T> customRegistry(ResourceKey<Registry<T>> key, CustomRegistryOptions options);

    <T extends LivingEntity> void registerEntityAttributes(
        RegistrationHandle<EntityType<?>, EntityType<T>> entityType,
        Supplier<AttributeSupplier> attributes
    );

    void populateCreativeTab(ResourceKey<CreativeModeTab> tab, Consumer<CreativeTabOutput> population);

    QueuedValue<WoodType> registerWoodType(Supplier<? extends WoodType> factory);

    void registerStrippable(Supplier<? extends Block> block, Supplier<? extends Block> stripped);

    void registerFlammable(Supplier<? extends Block> block, int igniteOdds, int burnOdds);

    void apply();

    boolean isApplied();

    default <T extends Block> RegistrationHandle<Block, T> registerBlock(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.BLOCK, id, factory);
    }

    default <T extends Item> RegistrationHandle<Item, T> registerItem(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.ITEM, id, factory);
    }

    default <T extends Fluid> RegistrationHandle<Fluid, T> registerFluid(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.FLUID, id, factory);
    }

    default <T extends BlockEntityType<?>> RegistrationHandle<BlockEntityType<?>, T> registerBlockEntityType(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.BLOCK_ENTITY_TYPE, id, factory);
    }

    default <T extends Entity> RegistrationHandle<EntityType<?>, EntityType<T>> registerEntityType(
        Identifier id,
        Supplier<? extends EntityType<T>> factory
    ) {
        return register(Registries.ENTITY_TYPE, id, factory);
    }

    default <T extends Attribute> RegistrationHandle<Attribute, T> registerAttribute(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.ATTRIBUTE, id, factory);
    }

    default <T extends MenuType<?>> RegistrationHandle<MenuType<?>, T> registerMenu(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.MENU, id, factory);
    }

    default <T extends Recipe<?>> RegistrationHandle<RecipeType<?>, RecipeType<T>> registerRecipeType(
        Identifier id,
        Supplier<? extends RecipeType<T>> factory
    ) {
        return register(Registries.RECIPE_TYPE, id, factory);
    }

    default <T extends Recipe<?>> RegistrationHandle<RecipeSerializer<?>, RecipeSerializer<T>> registerRecipeSerializer(
        Identifier id,
        Supplier<? extends RecipeSerializer<T>> factory
    ) {
        return register(Registries.RECIPE_SERIALIZER, id, factory);
    }

    default <T extends RecipeBookCategory> RegistrationHandle<RecipeBookCategory, T> registerRecipeBookCategory(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.RECIPE_BOOK_CATEGORY, id, factory);
    }

    default <T> RegistrationHandle<DataComponentType<?>, DataComponentType<T>> registerDataComponentType(
        Identifier id,
        Supplier<? extends DataComponentType<T>> factory
    ) {
        return register(Registries.DATA_COMPONENT_TYPE, id, factory);
    }

    default <T extends ConsumeEffect> RegistrationHandle<ConsumeEffect.Type<?>, ConsumeEffect.Type<T>>
    registerConsumeEffectType(Identifier id, Supplier<? extends ConsumeEffect.Type<T>> factory) {
        return register(Registries.CONSUME_EFFECT_TYPE, id, factory);
    }

    default <T extends PositionSource> RegistrationHandle<PositionSourceType<?>, PositionSourceType<T>>
    registerPositionSourceType(Identifier id, Supplier<? extends PositionSourceType<T>> factory) {
        return register(Registries.POSITION_SOURCE_TYPE, id, factory);
    }

    default <T extends Feature<?>> RegistrationHandle<Feature<?>, T> registerFeature(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.FEATURE, id, factory);
    }

    default <T extends TrunkPlacer> RegistrationHandle<TrunkPlacerType<?>, TrunkPlacerType<T>> registerTrunkPlacerType(
        Identifier id,
        Supplier<? extends TrunkPlacerType<T>> factory
    ) {
        return register(Registries.TRUNK_PLACER_TYPE, id, factory);
    }

    default <T extends CreativeModeTab> RegistrationHandle<CreativeModeTab, T> registerCreativeTab(
        Identifier id,
        Supplier<? extends T> factory
    ) {
        return register(Registries.CREATIVE_MODE_TAB, id, factory);
    }

    default <T> CustomRegistry<T> customRegistry(Identifier id) {
        return customRegistry(ResourceKey.createRegistryKey(id), CustomRegistryOptions.DEFAULT);
    }

    default <T> CustomRegistry<T> customRegistry(Identifier id, CustomRegistryOptions options) {
        return customRegistry(ResourceKey.createRegistryKey(id), options);
    }

    final class ServiceHolder {
        private static final RegistryService INSTANCE = ServiceLoader.load(
                RegistryService.class,
                RegistryService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No registry service is available"));

        private ServiceHolder() {
        }
    }
}
