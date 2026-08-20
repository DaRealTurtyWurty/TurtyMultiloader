package dev.turtywurty.turtymultiloader.menu.client;

import dev.turtywurty.turtymultiloader.menu.ExtendedMenuRegistration;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Client-only facade for connecting common menu and screen classes.
 */
public final class ClientMenus {
    private ClientMenus() {
    }

    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
        ExtendedMenuRegistration<M, ?> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        register((Supplier<MenuType<M>>) Objects.requireNonNull(menuType, "menuType"), screenFactory);
    }

    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
        RegistrationHandle<MenuType<?>, ? extends MenuType<M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        register((Supplier<? extends MenuType<M>>) Objects.requireNonNull(menuType, "menuType"), screenFactory);
    }

    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
        Supplier<? extends MenuType<? extends M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        ClientMenuService.get().registerScreen(
            Objects.requireNonNull(menuType, "menuType"),
            Objects.requireNonNull(screenFactory, "screenFactory")
        );
    }
}
