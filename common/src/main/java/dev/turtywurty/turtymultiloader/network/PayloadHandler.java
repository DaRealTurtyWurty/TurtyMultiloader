package dev.turtywurty.turtymultiloader.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@FunctionalInterface
public interface PayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, PayloadContext context);
}
