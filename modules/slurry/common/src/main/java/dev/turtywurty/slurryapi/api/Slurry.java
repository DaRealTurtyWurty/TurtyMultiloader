package dev.turtywurty.slurryapi.api;

import dev.turtywurty.slurryapi.SlurryApi;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A registered slurry kind.
 */
public final class Slurry {
    private final Identifier id;

    public Slurry(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public Identifier id() {
        return this.id;
    }

    public boolean isEmpty() {
        return this.id.equals(SlurryApi.EMPTY.id());
    }

    public boolean matchesType(Slurry slurry) {
        return this == slurry;
    }

    @Override
    public String toString() {
        return "Slurry[" + this.id + "]";
    }
}
