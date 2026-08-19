package dev.turtywurty.turtymultiloader.transfer.unit;

import net.minecraft.resources.Identifier;

/**
 * Core neutral units. Optional resource modules register their own dimensions and units here.
 */
public final class Units {
    public static final UnitRegistry REGISTRY = new UnitRegistry();

    public static final TransferUnit ITEM = unit("item", UnitDimension.ITEM, "item", 1);
    public static final TransferUnit FLUID_DROPLET = unit("fluid_droplet", UnitDimension.FLUID, "d", 1);
    public static final TransferUnit FLUID_MILLIBUCKET = unit("fluid_millibucket", UnitDimension.FLUID, "mB", 81);
    public static final TransferUnit FLUID_BUCKET = unit("fluid_bucket", UnitDimension.FLUID, "B", 81_000);
    public static final TransferUnit ENERGY = unit("energy", UnitDimension.ENERGY, "E", 1);
    public static final TransferUnit KILOENERGY = unit("kiloenergy", UnitDimension.ENERGY, "kE", 1_000);

    private Units() {
    }

    private static TransferUnit unit(String path, UnitDimension dimension, String symbol, long numerator) {
        return REGISTRY.register(
            Identifier.fromNamespaceAndPath("turtymultiloader", path),
            dimension,
            symbol,
            numerator
        );
    }
}
