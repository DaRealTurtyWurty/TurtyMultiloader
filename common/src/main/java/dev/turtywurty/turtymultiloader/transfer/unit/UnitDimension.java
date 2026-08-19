package dev.turtywurty.turtymultiloader.transfer.unit;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Identifies a physical amount dimension; mods may define additional dimensions by identifier. */
public record UnitDimension(Identifier id) {
    public static final UnitDimension ITEM = standard("item");
    public static final UnitDimension FLUID = standard("fluid");
    public static final UnitDimension ENERGY = standard("energy");
    public UnitDimension {
        Objects.requireNonNull(id, "id");
    }

    public static UnitDimension of(Identifier id) {
        return new UnitDimension(id);
    }

    private static UnitDimension standard(String path) {
        return of(Identifier.fromNamespaceAndPath("turtymultiloader", path));
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
