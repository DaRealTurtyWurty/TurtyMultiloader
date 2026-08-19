package dev.turtywurty.turtymultiloader.neoforge.transfer;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;

public final class NeoForgeResourceAdapters {
    private NeoForgeResourceAdapters() {
    }

    public static ResourceVariant<Item> fromNeoForge(ItemResource resource) {
        return ResourceVariant.of(ResourceTypes.ITEM, resource.typeHolder(), resource.getComponentsPatch());
    }

    public static ItemResource toNeoForgeItem(ResourceVariant<Item> variant) {
        return ItemResource.of(variant.holder(), variant.components());
    }

    public static ResourceVariant<Fluid> fromNeoForge(FluidResource resource) {
        return ResourceVariant.of(ResourceTypes.FLUID, resource.typeHolder(), resource.getComponentsPatch());
    }

    public static FluidResource toNeoForgeFluid(ResourceVariant<Fluid> variant) {
        return FluidResource.of(variant.holder(), variant.components());
    }

    public record NeutralResource<V extends ResourceVariant<?>>(V variant) implements Resource {
        @Override
        public boolean isEmpty() {
            return this.variant.isBlank();
        }
    }
}
