package dev.turtywurty.turtymultiloader.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Loader-neutral networking built directly on vanilla {@link CustomPacketPayload} and {@link StreamCodec}.
 * Payload declarations must be made during mod initialization. Client handlers must be attached from a client
 * initializer so dedicated servers never load client implementation code.
 */
public interface NetworkService {
    static NetworkService get() {
        return ServiceHolder.INSTANCE;
    }

    <B extends FriendlyByteBuf, T extends CustomPacketPayload> void registerPayload(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler
    );

    <T extends CustomPacketPayload> void registerClientHandler(
        PayloadPhase phase,
        CustomPacketPayload.Type<T> type,
        PayloadHandler<T> handler
    );

    default <T extends CustomPacketPayload> void registerPlayClientbound(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options
    ) {
        registerPayload(PayloadPhase.PLAY, PayloadFlow.CLIENTBOUND, type, codec, options, null);
    }

    default <T extends CustomPacketPayload> void registerPlayServerbound(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> handler
    ) {
        registerPayload(PayloadPhase.PLAY, PayloadFlow.SERVERBOUND, type, codec, options,
            Objects.requireNonNull(handler, "handler"));
    }

    default <T extends CustomPacketPayload> void registerPlayBidirectional(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler
    ) {
        registerPayload(PayloadPhase.PLAY, PayloadFlow.BIDIRECTIONAL, type, codec, options,
            Objects.requireNonNull(serverHandler, "serverHandler"));
    }

    default <T extends CustomPacketPayload> void registerConfigurationClientbound(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super FriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options
    ) {
        registerPayload(PayloadPhase.CONFIGURATION, PayloadFlow.CLIENTBOUND, type, codec, options, null);
    }

    default <T extends CustomPacketPayload> void registerConfigurationServerbound(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super FriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> handler
    ) {
        registerPayload(PayloadPhase.CONFIGURATION, PayloadFlow.SERVERBOUND, type, codec, options,
            Objects.requireNonNull(handler, "handler"));
    }

    default <T extends CustomPacketPayload> void registerConfigurationBidirectional(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super FriendlyByteBuf, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler
    ) {
        registerPayload(PayloadPhase.CONFIGURATION, PayloadFlow.BIDIRECTIONAL, type, codec, options,
            Objects.requireNonNull(serverHandler, "serverHandler"));
    }

    void sendToServer(CustomPacketPayload payload);

    boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    int sendToPlayer(ServerPlayer player, Iterable<? extends CustomPacketPayload> payloads);

    void sendToAll(MinecraftServer server, CustomPacketPayload payload);

    void sendToTracking(Entity entity, CustomPacketPayload payload);

    void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

    void sendToDimension(ServerLevel level, CustomPacketPayload payload);

    void sendToNearby(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload);

    boolean canSendToServer(CustomPacketPayload.Type<?> type);

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void onConnection(Consumer<ServerConnectionContext> callback);

    void onJoin(Consumer<ServerPlayer> callback);

    void onDisconnect(Consumer<ServerPlayer> callback);

    void onClientJoin(Runnable callback);

    void onClientDisconnect(Runnable callback);

    /**
     * Registers an ordered server configuration task. Declarations must be made during mod initialization.
     */
    void registerConfigurationTask(ServerConfigurationTask task);

    void addLoginSync(LoginSyncProvider provider);

    final class ServiceHolder {
        private static final NetworkService INSTANCE = ServiceLoader.load(
                NetworkService.class,
                NetworkService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No networking service is available"));

        private ServiceHolder() {
        }
    }
}
