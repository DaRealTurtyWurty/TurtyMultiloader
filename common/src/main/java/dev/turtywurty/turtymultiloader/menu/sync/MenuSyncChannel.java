package dev.turtywurty.turtymultiloader.menu.sync;

import dev.turtywurty.turtymultiloader.network.NetworkService;
import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.network.PayloadRegistrationOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A typed clientbound synchronization channel scoped to an open menu instance.
 *
 * <p>Declare channels during common initialization and attach their receiver during client initialization.
 */
public final class MenuSyncChannel<M extends AbstractContainerMenu, T> {
    private final Class<M> menuClass;
    private final CustomPacketPayload.Type<SyncPayload<T>> payloadType;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> valueCodec;
    private boolean clientReceiverRegistered;

    private MenuSyncChannel(
        Identifier id,
        Class<M> menuClass,
        StreamCodec<? super RegistryFriendlyByteBuf, T> valueCodec,
        PayloadRegistrationOptions options
    ) {
        this.menuClass = Objects.requireNonNull(menuClass, "menuClass");
        this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
        this.payloadType = new CustomPacketPayload.Type<>(Objects.requireNonNull(id, "id"));
        NetworkService.get().registerPlayClientbound(payloadType, payloadCodec(), Objects.requireNonNull(options, "options"));
    }

    public static <M extends AbstractContainerMenu, T> MenuSyncChannel<M, T> register(
        Identifier id,
        Class<M> menuClass,
        StreamCodec<? super RegistryFriendlyByteBuf, T> valueCodec
    ) {
        return register(id, menuClass, valueCodec, PayloadRegistrationOptions.DEFAULT);
    }

    public static <M extends AbstractContainerMenu, T> MenuSyncChannel<M, T> register(
        Identifier id,
        Class<M> menuClass,
        StreamCodec<? super RegistryFriendlyByteBuf, T> valueCodec,
        PayloadRegistrationOptions options
    ) {
        return new MenuSyncChannel<>(id, menuClass, valueCodec, options);
    }

    public Identifier id() {
        return payloadType.id();
    }

    /**
     * Attaches the client-side state update. Call this exactly once from a client initializer.
     */
    public synchronized void registerClientReceiver(BiConsumer<M, T> receiver) {
        Objects.requireNonNull(receiver, "receiver");
        if (clientReceiverRegistered)
            throw new IllegalStateException("Client receiver for menu sync channel " + id() + " is already registered");

        clientReceiverRegistered = true;
        NetworkService.get().registerClientHandler(PayloadPhase.PLAY, payloadType, (payload, context) ->
            context.player().ifPresent(player -> {
                AbstractContainerMenu openMenu = player.containerMenu;
                if (openMenu.containerId == payload.containerId() && menuClass.isInstance(openMenu))
                    receiver.accept(menuClass.cast(openMenu), payload.value());
            })
        );
    }

    /**
     * Sends a value only if {@code menu} is still the player's active menu.
     */
    public boolean send(ServerPlayer player, M menu, T value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(menu, "menu");
        Objects.requireNonNull(value, "value");
        if (player.containerMenu != menu)
            return false;

        return NetworkService.get().sendToPlayer(player, new SyncPayload<>(payloadType, menu.containerId, value));
    }

    private StreamCodec<RegistryFriendlyByteBuf, SyncPayload<T>> payloadCodec() {
        return new StreamCodec<>() {
            @Override
            public SyncPayload<T> decode(RegistryFriendlyByteBuf buffer) {
                return new SyncPayload<>(payloadType, buffer.readVarInt(), valueCodec.decode(buffer));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SyncPayload<T> payload) {
                buffer.writeVarInt(payload.containerId());
                valueCodec.encode(buffer, payload.value());
            }
        };
    }

    private record SyncPayload<T>(
        CustomPacketPayload.Type<SyncPayload<T>> payloadType,
        int containerId,
        T value
    ) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }
}
