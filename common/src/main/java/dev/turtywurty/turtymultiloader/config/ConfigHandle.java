package dev.turtywurty.turtymultiloader.config;

import com.google.gson.JsonElement;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A live, typed configuration registered with {@link Configurations}.
 */
public interface ConfigHandle<T> {
    ConfigurationSpec<T> spec();

    T value();

    boolean isLoaded();

    Optional<Path> path();

    Path path(MinecraftServer server);

    JsonElement encode();

    void load();

    void load(MinecraftServer server);

    void reload();

    void reload(MinecraftServer server);

    void save();

    void save(MinecraftServer server);

    /**
     * Sends the current value to connected clients when this is a {@link ConfigScope#SERVER} config.
     */
    void synchronize(MinecraftServer server);

    /**
     * Validates and installs a value in memory. Call {@link #save()} or {@link #save(MinecraftServer)} to persist it.
     */
    void set(T value);

    default void setAndSave(T value, MinecraftServer server) {
        set(value);
        save(server);
        synchronize(server);
    }
}
