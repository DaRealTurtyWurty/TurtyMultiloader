package dev.turtywurty.turtymultiloader.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Constructs the client-side instance of an extended menu from its opening data.
 */
@FunctionalInterface
public interface ExtendedMenuFactory<M extends AbstractContainerMenu, D> {
    M create(int containerId, Inventory inventory, D openingData);
}
