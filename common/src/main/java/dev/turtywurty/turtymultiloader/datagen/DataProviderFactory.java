package dev.turtywurty.turtymultiloader.datagen;

import net.minecraft.data.DataProvider;

/**
 * Creates a vanilla data provider from loader-neutral generator inputs.
 */
@FunctionalInterface
public interface DataProviderFactory {
    DataProvider create(DataProviderContext context);
}
