package dev.turtywurty.turtymultiloader.registration;

import dev.turtywurty.turtymultiloader.worldgen.BiomeSelector;
import dev.turtywurty.turtymultiloader.worldgen.WorldGeneration;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configures a living entity type, its attributes, and its code-based natural spawns as one declaration.
 *
 * @param <T> the living entity created by the type
 */
public final class EntityTypeBuilder<T extends LivingEntity> {
    private final Identifier id;
    private final MobCategory category;
    private final EntityType.Builder<T> vanillaBuilder;
    private final List<SpawnDeclaration> spawns = new ArrayList<>();
    private Supplier<AttributeSupplier> attributes;

    public EntityTypeBuilder(
        Identifier id,
        EntityType.EntityFactory<T> factory,
        MobCategory category
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.category = Objects.requireNonNull(category, "category");
        this.vanillaBuilder = EntityType.Builder.of(Objects.requireNonNull(factory, "factory"), category);
    }

    public EntityTypeBuilder<T> sized(float width, float height) {
        vanillaBuilder.sized(width, height);
        return this;
    }

    public EntityTypeBuilder<T> eyeHeight(float eyeHeight) {
        vanillaBuilder.eyeHeight(eyeHeight);
        return this;
    }

    public EntityTypeBuilder<T> fireImmune() {
        vanillaBuilder.fireImmune();
        return this;
    }

    public EntityTypeBuilder<T> immuneTo(Block... blocks) {
        vanillaBuilder.immuneTo(Objects.requireNonNull(blocks, "blocks"));
        return this;
    }

    public EntityTypeBuilder<T> noSave() {
        vanillaBuilder.noSave();
        return this;
    }

    public EntityTypeBuilder<T> noSummon() {
        vanillaBuilder.noSummon();
        return this;
    }

    public EntityTypeBuilder<T> noLootTable() {
        vanillaBuilder.noLootTable();
        return this;
    }

    public EntityTypeBuilder<T> notInPeaceful() {
        vanillaBuilder.notInPeaceful();
        return this;
    }

    public EntityTypeBuilder<T> canSpawnFarFromPlayer() {
        vanillaBuilder.canSpawnFarFromPlayer();
        return this;
    }

    public EntityTypeBuilder<T> clientTrackingRange(int blocks) {
        vanillaBuilder.clientTrackingRange(blocks);
        return this;
    }

    public EntityTypeBuilder<T> updateInterval(int ticks) {
        vanillaBuilder.updateInterval(ticks);
        return this;
    }

    /**
     * Applies options not surfaced directly by this wrapper to the vanilla builder.
     */
    public EntityTypeBuilder<T> vanilla(Consumer<EntityType.Builder<T>> configuration) {
        Objects.requireNonNull(configuration, "configuration").accept(vanillaBuilder);
        return this;
    }

    public EntityTypeBuilder<T> attributes(Supplier<AttributeSupplier> attributes) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        return this;
    }

    /**
     * Adds a natural-spawn declaration. The modification ID must be unique among all code-based biome changes.
     */
    public EntityTypeBuilder<T> spawn(
        Identifier modificationId,
        BiomeSelector selector,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize
    ) {
        spawns.add(new SpawnDeclaration(
            Objects.requireNonNull(modificationId, "modificationId"),
            Objects.requireNonNull(selector, "selector"),
            weight,
            minimumGroupSize,
            maximumGroupSize
        ));
        return this;
    }

    EntityType<T> build(ResourceKey<EntityType<?>> key) {
        return vanillaBuilder.build(Objects.requireNonNull(key, "key"));
    }

    Supplier<AttributeSupplier> attributes() {
        if (attributes == null)
            throw new IllegalStateException("No attributes configured for living entity type " + id);

        return attributes;
    }

    void registerSpawns(Supplier<? extends EntityType<?>> entityType) {
        spawns.forEach(spawn -> WorldGeneration.addSpawn(
            spawn.modificationId(),
            spawn.selector(),
            category,
            entityType,
            spawn.weight(),
            spawn.minimumGroupSize(),
            spawn.maximumGroupSize()
        ));
    }

    private record SpawnDeclaration(
        Identifier modificationId,
        BiomeSelector selector,
        int weight,
        int minimumGroupSize,
        int maximumGroupSize
    ) {
    }
}
