package dev.turtywurty.turtymultiloader.menu;

import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Loader-neutral menu registration and opening helpers.
 */
public final class Menus {
    private Menus() {
    }

    public static <M extends AbstractContainerMenu, D> ExtendedMenuRegistration<M, D> registerExtended(
        Identifier id,
        ExtendedMenuFactory<M, D> factory,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(openingDataCodec, "openingDataCodec");

        RegistrationHandle<MenuType<?>, MenuType<M>> handle = RegistryService.get().registerMenu(
            id,
            () -> MenuService.get().createExtendedMenuType(factory, openingDataCodec)
        );
        return new ExtendedMenuRegistration<>(handle, openingDataCodec);
    }

    public static OptionalInt open(ServerPlayer player, MenuProvider provider) {
        return Objects.requireNonNull(player, "player").openMenu(Objects.requireNonNull(provider, "provider"));
    }

    public static <D> OptionalInt open(
        ServerPlayer player,
        ExtendedMenuProvider<D> provider,
        ExtendedMenuRegistration<?, D> menuType
    ) {
        Objects.requireNonNull(menuType, "menuType");
        return menuType.open(player, provider);
    }

    public static <D> OptionalInt open(
        ServerPlayer player,
        MenuProvider provider,
        ExtendedMenuRegistration<?, D> menuType,
        D openingData
    ) {
        Objects.requireNonNull(menuType, "menuType");
        return menuType.open(player, provider, openingData);
    }
}
