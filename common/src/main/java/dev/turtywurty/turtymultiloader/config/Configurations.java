package dev.turtywurty.turtymultiloader.config;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Optional;

/**
 * Public entry point for registering and inspecting typed configurations.
 */
public final class Configurations {
    private Configurations() {
    }

    public static <T> ConfigHandle<T> register(ConfigurationSpec<T> spec) {
        return ConfigurationService.get().register(spec);
    }

    public static Optional<ConfigHandle<?>> find(Identifier id) {
        return ConfigurationManager.instance().find(id);
    }

    public static Collection<ConfigHandle<?>> all() {
        return ConfigurationManager.instance().all();
    }

    public static void initializeCommon() {
        ConfigurationService.get().initializeCommon();
    }

    public static void initializeClient() {
        ConfigurationService.get().initializeClient();
    }
}
