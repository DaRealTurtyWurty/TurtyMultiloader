package dev.turtywurty.turtymultiloader.network;

import dev.turtywurty.turtymultiloader.platform.LogicalSide;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Loader-neutral receive context. Registered handlers are invoked on the appropriate main game thread.
 */
public interface PayloadContext {
    PayloadPhase phase();

    LogicalSide receptionSide();

    Optional<MinecraftServer> server();

    /** The receiving local player or sending server player during play; empty during configuration. */
    Optional<Player> player();

    /** The authenticated sender of a serverbound play payload. */
    Optional<ServerPlayer> sender();

    default PermissionSet permissions() {
        return sender().map(ServerPlayer::permissions).orElse(PermissionSet.NO_PERMISSIONS);
    }

    default boolean hasPermission(Permission permission) {
        return permissions().hasPermission(permission);
    }

    default boolean hasPermission(PermissionCheck permission) {
        return permission.check(permissions());
    }

    void reply(CustomPacketPayload payload);

    void disconnect(Component reason);

    /** Schedules follow-up work on the same main game thread used for this context. */
    CompletableFuture<Void> enqueueWork(Runnable work);
}
