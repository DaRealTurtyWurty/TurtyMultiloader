package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.menu.ExtendedMenuFactory;
import dev.turtywurty.turtymultiloader.menu.MenuService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import java.util.Objects;
import java.util.OptionalInt;

public final class NeoForgeMenuService implements MenuService {
    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createExtendedMenuType(
        ExtendedMenuFactory<M, D> factory,
        StreamCodec<? super RegistryFriendlyByteBuf, D> openingDataCodec
    ) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(openingDataCodec, "openingDataCodec");
        return IMenuTypeExtension.create((containerId, inventory, buffer) ->
            factory.create(containerId, inventory, openingDataCodec.decode(buffer))
        );
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
        return player.openMenu(provider, buffer -> openingDataCodec.encode(buffer, openingData));
    }
}
