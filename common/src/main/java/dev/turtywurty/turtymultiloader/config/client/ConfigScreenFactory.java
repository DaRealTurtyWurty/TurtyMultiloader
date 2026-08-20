package dev.turtywurty.turtymultiloader.config.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * Client-only factory for a mod's custom configuration screen.
 */
@FunctionalInterface
public interface ConfigScreenFactory {
    Screen create(Screen parent);
}
