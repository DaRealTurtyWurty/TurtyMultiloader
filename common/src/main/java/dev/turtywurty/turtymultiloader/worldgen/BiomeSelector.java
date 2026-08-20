package dev.turtywurty.turtymultiloader.worldgen;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Selects biomes for a code-based modification.
 *
 * <p>Prefer biome tags in datapack biome-modifier resources when the selection is static. This interface is intended
 * for selections which genuinely depend on code, such as configuration values.</p>
 */
@FunctionalInterface
public interface BiomeSelector extends Predicate<BiomeSelectionContext> {
    @Override
    default BiomeSelector and(Predicate<? super BiomeSelectionContext> other) {
        Objects.requireNonNull(other, "other");
        return context -> test(context) && other.test(context);
    }

    @Override
    default BiomeSelector negate() {
        return context -> !test(context);
    }

    @Override
    default BiomeSelector or(Predicate<? super BiomeSelectionContext> other) {
        Objects.requireNonNull(other, "other");
        return context -> test(context) || other.test(context);
    }
}
