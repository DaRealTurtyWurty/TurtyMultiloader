package dev.turtywurty.turtymultiloader.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The active server configuration task and its connection.
 */
public interface ServerConfigurationTaskContext {
    ServerConnectionContext connection();

    /**
     * Returns whether the client negotiated the given registered clientbound configuration payload.
     */
    boolean canSend(CustomPacketPayload.Type<?> type);

    /**
     * Sends a registered clientbound configuration payload.
     *
     * @return {@code true} when sent; {@code false} for an unsupported optional payload
     * @throws IllegalStateException if the payload is undeclared, has the wrong phase/direction, or a required
     *                               payload was not negotiated
     */
    boolean send(CustomPacketPayload payload);

    /**
     * Completes this task immediately and allows the next configuration task to start.
     */
    void complete();

    /**
     * Disconnects the client and terminates configuration with the supplied reason.
     */
    void fail(Component reason);
}
