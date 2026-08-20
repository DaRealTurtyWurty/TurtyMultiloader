package dev.turtywurty.turtymultiloader.config;

import dev.turtywurty.turtymultiloader.platform.Platform;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Standard, loader-neutral config file locations.
 */
public final class ConfigPaths {
    private ConfigPaths() {
    }

    /**
     * Uses {@code config/<namespace>-<path>-<scope>.json} for non-server scopes and
     * {@code <world>/serverconfig/<namespace>-<path>-server.json} for server scope.
     */
    public static ConfigPath defaults() {
        return (id, scope, server) -> {
            String fileName = id.getNamespace() + '-' + id.getPath().replace('/', '-') + '-'
                + scope.fileSuffix() + ".json";
            if (scope == ConfigScope.SERVER) {
                MinecraftServerAccess access = new MinecraftServerAccess(server.orElseThrow(() ->
                    new IllegalStateException("A server is required to resolve server config " + id)
                ));
                return access.root().resolve("serverconfig").resolve(fileName);
            }
            return Platform.configDirectory().resolve(fileName);
        };
    }

    /**
     * Resolves a relative path below the loader's global config directory.
     */
    public static ConfigPath global(String relativePath) {
        Path relative = safeRelative(relativePath);
        return (id, scope, server) -> Platform.configDirectory().resolve(relative);
    }

    /**
     * Resolves a relative path below the active world directory. This is useful for retaining Industria's existing
     * {@code config/industria.json} format.
     */
    public static ConfigPath world(String relativePath) {
        Path relative = safeRelative(relativePath);
        return (id, scope, server) -> new MinecraftServerAccess(server.orElseThrow(() ->
            new IllegalStateException("A server is required to resolve world config " + id)
        )).root().resolve(relative);
    }

    /**
     * Resolves a relative path below the active world's {@code serverconfig} directory.
     */
    public static ConfigPath serverConfig(String relativePath) {
        Path relative = safeRelative(relativePath);
        return (id, scope, server) -> new MinecraftServerAccess(server.orElseThrow(() ->
            new IllegalStateException("A server is required to resolve server config " + id)
        )).root().resolve("serverconfig").resolve(relative);
    }

    private static Path safeRelative(String value) {
        Objects.requireNonNull(value, "relativePath");
        if (value.isBlank())
            throw new IllegalArgumentException("Config path must be a non-empty relative path");
        Path path = Path.of(value).normalize();
        if (path.isAbsolute() || path.getNameCount() == 0 || path.startsWith(".."))
            throw new IllegalArgumentException("Config path must be a non-empty relative path: " + value);
        return path;
    }

    private record MinecraftServerAccess(net.minecraft.server.MinecraftServer server) {
        private Path root() {
            return server.getWorldPath(LevelResource.ROOT);
        }
    }
}
