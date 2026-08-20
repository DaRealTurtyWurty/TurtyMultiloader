package dev.turtywurty.turtymultiloader.datagen.convention;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

/**
 * Factories for convention tags without a compile-time dependency on either loader's convention-tag constants.
 */
public final class ConventionTags {
    public static final String COMMON_NAMESPACE = "c";

    private ConventionTags() {
    }

    public static ConventionTag<Block> block(String path) {
        return common(Registries.BLOCK, path);
    }

    public static ConventionTag<Item> item(String path) {
        return common(Registries.ITEM, path);
    }

    public static ConventionTag<Fluid> fluid(String path) {
        return common(Registries.FLUID, path);
    }

    public static ConventionTag<EntityType<?>> entityType(String path) {
        return common(Registries.ENTITY_TYPE, path);
    }

    /**
     * Creates a convention tag whose current Fabric and NeoForge/common spellings are both {@code c:<path>}.
     */
    public static <T> ConventionTag<T> common(ResourceKey<? extends Registry<T>> registry, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(COMMON_NAMESPACE, Objects.requireNonNull(path, "path"));
        return new ConventionTag<>(registry, id, id);
    }

    /**
     * Creates a convention tag with explicitly different loader spellings.
     */
    public static <T> ConventionTag<T> mapped(
        ResourceKey<? extends Registry<T>> registry,
        Identifier fabricId,
        Identifier commonId
    ) {
        return new ConventionTag<>(registry, fabricId, commonId);
    }
}
