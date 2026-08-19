package dev.turtywurty.gasapi.api;

import dev.turtywurty.gasapi.GasApi;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** A registered gas kind. Registry identity, not this field, is authoritative. */
public final class Gas {
    private final Identifier id;

    public Gas(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public Identifier id() {
        return this.id;
    }

    public boolean isEmpty() {
        return this.id.equals(GasApi.EMPTY.id());
    }

    public boolean matchesType(Gas gas) {
        return this == gas;
    }

    @Override
    public String toString() {
        return "Gas[" + this.id + "]";
    }
}
