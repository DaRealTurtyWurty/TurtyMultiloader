package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface EntityStorageProvider<E extends Entity, V extends ResourceVariant<?>> {
    ResourceStorage<V> find(E entity, EntityStorageContext context);
}
