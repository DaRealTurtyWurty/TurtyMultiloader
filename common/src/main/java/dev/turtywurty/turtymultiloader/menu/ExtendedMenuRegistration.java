package dev.turtywurty.turtymultiloader.menu;

import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * The registry handle and opening-data contract for one extended menu type.
 */
public final class ExtendedMenuRegistration<M extends AbstractContainerMenu, D> implements Supplier<MenuType<M>> {
    private final RegistrationHandle<MenuType<?>, MenuType<M>> handle;
    private final StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec;

    ExtendedMenuRegistration(
        RegistrationHandle<MenuType<?>, MenuType<M>> handle,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    ) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.openingDataCodec = Objects.requireNonNull(openingDataCodec, "openingDataCodec");
    }

    public Identifier id() {
        return handle.id();
    }

    public RegistrationHandle<MenuType<?>, MenuType<M>> handle() {
        return handle;
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec() {
        return openingDataCodec;
    }

    @Override
    public MenuType<M> get() {
        return handle.get();
    }

    public OptionalInt open(ServerPlayer player, ExtendedMenuProvider<D> provider) {
        Objects.requireNonNull(provider, "provider");
        return open(player, provider, provider.getMenuOpeningData(player));
    }

    public OptionalInt open(ServerPlayer player, net.minecraft.world.MenuProvider provider, D openingData) {
        return MenuService.get().openExtendedMenu(
            Objects.requireNonNull(player, "player"),
            Objects.requireNonNull(provider, "provider"),
            Objects.requireNonNull(openingData, "openingData"),
            openingDataCodec
        );
    }
}
