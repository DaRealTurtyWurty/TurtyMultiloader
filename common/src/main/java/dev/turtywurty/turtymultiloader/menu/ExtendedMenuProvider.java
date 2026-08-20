package dev.turtywurty.turtymultiloader.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/**
 * A vanilla menu provider that also supplies the data used to construct its client-side menu.
 */
public interface ExtendedMenuProvider<D> extends MenuProvider {
    D getMenuOpeningData(ServerPlayer player);
}
