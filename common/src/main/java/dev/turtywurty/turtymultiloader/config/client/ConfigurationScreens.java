package dev.turtywurty.turtymultiloader.config.client;

import dev.turtywurty.turtymultiloader.config.ConfigurationManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional custom-screen bridge. NeoForge connects these factories to its Mods screen automatically.
 */
public final class ConfigurationScreens {
    private ConfigurationScreens() {
    }

    public static void register(String modId, ConfigScreenFactory factory) {
        ClientConfigurationService.get().registerScreen(
            requireModId(modId),
            Objects.requireNonNull(factory, "factory")
        );
    }

    /**
     * Allows Fabric integrations such as Mod Menu to obtain the registered screen without a hard dependency.
     */
    public static Optional<Screen> create(String modId, Screen parent) {
        return ConfigurationManager.instance().screenFactory(requireModId(modId)).map(factory -> factory.create(parent));
    }

    private static String requireModId(String modId) {
        String value = Objects.requireNonNull(modId, "modId");
        if (value.isBlank())
            throw new IllegalArgumentException("modId cannot be blank");
        return value;
    }
}
