package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.menu.ExtendedMenuFactory;
import dev.turtywurty.turtymultiloader.menu.MenuService;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.OptionalInt;

public final class FabricMenuService implements MenuService {
    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createExtendedMenuType(
        ExtendedMenuFactory<M, D> factory,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    ) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(openingDataCodec, "openingDataCodec");
        return new ExtendedMenuType<>(factory::create, openingDataCodec);
    }

    @Override
    public <D> OptionalInt openExtendedMenu(
        ServerPlayer player,
        MenuProvider provider,
        D openingData,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(openingData, "openingData");
        Objects.requireNonNull(openingDataCodec, "openingDataCodec");

        return player.openMenu(new ExtendedMenuProvider<D>() {
            @Override
            public D getScreenOpeningData(ServerPlayer ignored) {
                return openingData;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
                return provider.createMenu(containerId, inventory, menuPlayer);
            }
        });
    }
}
