package dev.turtywurty.turtymultiloader.network;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * One ordered server-side task in the login configuration phase.
 *
 * <p>A task may send any number of registered configuration payloads. It must eventually call
 * {@link ServerConfigurationTaskContext#complete()}, or arrange for a serverbound configuration payload handler to
 * call {@link PayloadContext#completeConfigurationTask(Identifier)} after the client has acknowledged the work.</p>
 */
public record ServerConfigurationTask(
    Identifier id,
    Consumer<ServerConfigurationTaskContext> handler
) {
    public ServerConfigurationTask {
        id = Objects.requireNonNull(id, "id");
        handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Starts this task. The task remains active until it is completed or failed.
     */
    public void run(ServerConfigurationTaskContext context) {
        handler.accept(Objects.requireNonNull(context, "context"));
    }
}
