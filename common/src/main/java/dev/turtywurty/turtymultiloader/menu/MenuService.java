package dev.turtywurty.turtymultiloader.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.OptionalInt;
import java.util.ServiceLoader;

/**
 * Loader bridge for extended menu construction and server-side opening.
 */
public interface MenuService {
    static MenuService get() {
        return ServiceHolder.INSTANCE;
    }

    <M extends AbstractContainerMenu, D> MenuType<M> createExtendedMenuType(
        ExtendedMenuFactory<M, D> factory,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    );

    <D> OptionalInt openExtendedMenu(
        ServerPlayer player,
        MenuProvider provider,
        D openingData,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    );

    final class ServiceHolder {
        private static final MenuService INSTANCE = ServiceLoader.load(
                MenuService.class,
                MenuService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No menu service is available"));

        private ServiceHolder() {
        }
    }
}
