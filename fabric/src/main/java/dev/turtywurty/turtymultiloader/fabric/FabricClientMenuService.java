package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.menu.client.ClientMenuService;
import dev.turtywurty.turtymultiloader.menu.client.MenuScreenFactory;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

public final class FabricClientMenuService implements ClientMenuService {
    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
        Supplier<? extends MenuType<? extends M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        MenuScreens.register(
            Objects.requireNonNull(menuType, "menuType").get(),
            Objects.requireNonNull(screenFactory, "screenFactory")::create
        );
    }
}
