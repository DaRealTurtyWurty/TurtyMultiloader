package dev.turtywurty.gasapi.client;

import dev.turtywurty.gasapi.api.Gas;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class GasRenderHandlerRegistry {
    private static final Map<Gas, GasRenderHandler> HANDLERS = new IdentityHashMap<>();

    private GasRenderHandlerRegistry() {
    }

    public static synchronized GasRenderHandler get(Gas gas) {
        return HANDLERS.get(gas);
    }

    public static synchronized void register(Gas gas, GasRenderHandler handler) {
        if (HANDLERS.putIfAbsent(Objects.requireNonNull(gas, "gas"), Objects.requireNonNull(handler, "handler")) != null)
            throw new IllegalStateException("Duplicate handler for gas: " + gas);
    }
}
