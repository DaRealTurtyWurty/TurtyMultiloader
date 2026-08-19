package dev.turtywurty.turtymultiloader.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;

/** A server-side connection entering the configuration phase. */
public record ServerConnectionContext(
    MinecraftServer server,
    Connection connection,
    GameProfile profile
) {
    public ServerConnectionContext {
        server = Objects.requireNonNull(server, "server");
        connection = Objects.requireNonNull(connection, "connection");
        profile = Objects.requireNonNull(profile, "profile");
    }

    public void disconnect(Component reason) {
        connection.disconnect(Objects.requireNonNull(reason, "reason"));
    }
}
