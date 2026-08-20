package dev.turtywurty.turtymultiloader.menu.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Client-only loader bridge for menu screen registration.
 */
public interface ClientMenuService {
    static ClientMenuService get() {
        return ServiceHolder.INSTANCE;
    }

    <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
        Supplier<? extends MenuType<? extends M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    );

    final class ServiceHolder {
        private static final ClientMenuService INSTANCE = ServiceLoader.load(
                ClientMenuService.class,
                ClientMenuService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client menu service is available"));

        private ServiceHolder() {
        }
    }
}
