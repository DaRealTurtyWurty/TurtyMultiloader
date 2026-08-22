package dev.turtywurty.turtymultiloader.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import java.util.OptionalInt;

/**
 * A menu provider that owns its full loader-specific open path.
 */
public interface SelfOpeningMenuProvider extends MenuProvider {
    OptionalInt openMenu(ServerPlayer player);
}
