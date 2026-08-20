package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.menu.client.ClientMenuService;
import dev.turtywurty.turtymultiloader.menu.client.MenuScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class NeoForgeClientMenuService implements ClientMenuService {
    private static volatile IEventBus modBus;
    private static boolean listenerRegistered;

    private final List<ScreenDeclaration<?, ?>> screens = new ArrayList<>();
    private boolean registrationClosed;

    public static synchronized void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge client menu service is already bound to a mod event bus");

        modBus = Objects.requireNonNull(bus, "bus");
        if (!listenerRegistered) {
            bus.addListener(RegisterMenuScreensEvent.class, NeoForgeClientMenuService::registerScreens);
            listenerRegistered = true;
        }
    }

    @Override
    public synchronized <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
        Supplier<? extends MenuType<? extends M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        if (registrationClosed)
            throw new IllegalStateException("Menu screens must be registered before RegisterMenuScreensEvent");

        screens.add(new ScreenDeclaration<>(
            Objects.requireNonNull(menuType, "menuType"),
            Objects.requireNonNull(screenFactory, "screenFactory")
        ));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        ClientMenuService service = ClientMenuService.get();
        if (!(service instanceof NeoForgeClientMenuService neoForgeService))
            throw new IllegalStateException("NeoForge client menu service provider is not active");

        neoForgeService.apply(event);
    }

    private synchronized void apply(RegisterMenuScreensEvent event) {
        screens.forEach(declaration -> declaration.register(event));
        registrationClosed = true;
    }

    private record ScreenDeclaration<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>(
        Supplier<? extends MenuType<? extends M>> menuType,
        MenuScreenFactory<M, S> screenFactory
    ) {
        private void register(RegisterMenuScreensEvent event) {
            event.register(menuType.get(), screenFactory::create);
        }
    }
}
