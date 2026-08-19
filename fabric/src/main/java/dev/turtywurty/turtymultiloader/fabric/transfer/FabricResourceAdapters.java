package dev.turtywurty.turtymultiloader.fabric.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public final class FabricResourceAdapters {
    private FabricResourceAdapters() {
    }

    public static ResourceVariant<Item> fromFabric(ItemVariant variant) {
        return ResourceVariant.of(ResourceTypes.ITEM, variant.typeHolder(), variant.getComponentsPatch());
    }

    public static ItemVariant toFabricItem(ResourceVariant<Item> variant) {
        return ItemVariant.of(variant.value(), variant.components());
    }

    public static ResourceVariant<Fluid> fromFabric(FluidVariant variant) {
        return ResourceVariant.of(ResourceTypes.FLUID, variant.typeHolder(), variant.getComponentsPatch());
    }

    public static FluidVariant toFabricFluid(ResourceVariant<Fluid> variant) {
        return FluidVariant.of(variant.value(), variant.components());
    }
}
