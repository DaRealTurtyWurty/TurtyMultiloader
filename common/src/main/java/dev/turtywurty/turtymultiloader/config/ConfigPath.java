package dev.turtywurty.turtymultiloader.config;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the file used by a configuration.
 */
@FunctionalInterface
public interface ConfigPath {
    Path resolve(Identifier id, ConfigScope scope, Optional<MinecraftServer> server);
}
