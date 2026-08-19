package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceType;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Names a storage capability without hiding its resource family or amount unit.
 */
public record StorageKey<V extends ResourceVariant<?>>(Identifier id, ResourceType<?> resourceType, TransferUnit unit) {
    public StorageKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(unit, "unit");
        if (!resourceType.unitDimension().equals(unit.dimension()))
            throw new IllegalArgumentException("Resource family " + resourceType.id() + " requires "
                + resourceType.unitDimension() + " units, not " + unit.dimension());
    }
}
