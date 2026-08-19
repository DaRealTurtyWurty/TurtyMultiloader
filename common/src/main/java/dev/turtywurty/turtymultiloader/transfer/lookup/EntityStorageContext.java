package dev.turtywurty.turtymultiloader.transfer.lookup;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/**
 * Neutral context for entity capability queries. Both fields may be null.
 */
public record EntityStorageContext(Direction side, Entity actor) {
    public static final EntityStorageContext EMPTY = new EntityStorageContext(null, null);
}
