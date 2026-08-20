package dev.turtywurty.turtymultiloader.menu.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Constructs a screen for a client-side menu instance.
 */
@FunctionalInterface
public interface MenuScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M menu, Inventory inventory, Component title);
}
