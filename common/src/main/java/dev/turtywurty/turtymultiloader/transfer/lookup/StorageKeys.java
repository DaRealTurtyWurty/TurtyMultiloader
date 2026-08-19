package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public final class StorageKeys {
    public static final StorageKey<ResourceVariant<Item>> ITEM = key("item", ResourceTypes.ITEM, Units.ITEM);
    public static final StorageKey<ResourceVariant<Fluid>> FLUID = key("fluid", ResourceTypes.FLUID, Units.FLUID_DROPLET);
    public static final StorageKey<ResourceVariant<UnitResource>> ENERGY = key("energy", ResourceTypes.ENERGY, Units.ENERGY);

    private StorageKeys() {
    }

    public static <T> StorageKey<ResourceVariant<T>> key(
        String path,
        ResourceType<T> type,
        TransferUnit unit
    ) {
        return key(Identifier.fromNamespaceAndPath("turtymultiloader", path), type, unit);
    }

    public static <T> StorageKey<ResourceVariant<T>> key(
        Identifier id,
        ResourceType<T> type,
        TransferUnit unit
    ) {
        return new StorageKey<>(id, type, unit);
    }
}
