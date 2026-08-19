package dev.turtywurty.turtymultiloader.transfer.storage;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Side-to-storage exposure map with an optional unsided view.
 */
public final class SidedStorage<V extends ResourceVariant<?>> {
    private final Map<Direction, ResourceStorage<V>> sides;
    private final ResourceStorage<V> unsided;

    public SidedStorage(Map<Direction, ? extends ResourceStorage<V>> sides, ResourceStorage<V> unsided) {
        this.sides = new EnumMap<>(Direction.class);
        this.sides.putAll(sides);
        this.unsided = unsided;
    }

    public static <V extends ResourceVariant<?>> SidedStorage<V> same(ResourceStorage<V> storage) {
        return new SidedStorage<>(Map.of(), Objects.requireNonNull(storage, "storage"));
    }

    public ResourceStorage<V> forSide(Direction side) {
        return side == null ? this.unsided : this.sides.getOrDefault(side, this.unsided);
    }
}
