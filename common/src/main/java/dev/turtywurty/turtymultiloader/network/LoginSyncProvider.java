package dev.turtywurty.turtymultiloader.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface LoginSyncProvider {
    Iterable<? extends CustomPacketPayload> createPayloads(ServerPlayer player);
}
