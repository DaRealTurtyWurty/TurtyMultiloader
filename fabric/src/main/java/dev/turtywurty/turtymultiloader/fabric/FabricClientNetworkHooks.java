package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.network.PayloadContext;
import dev.turtywurty.turtymultiloader.network.PayloadHandler;
import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.platform.LogicalSide;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class FabricClientNetworkHooks {
    private FabricClientNetworkHooks() {
    }

    static void initialize(FabricNetworkService service) {
        ClientConfigurationConnectionEvents.START.register((listener, client) ->
            service.validateClientRequired(PayloadPhase.CONFIGURATION)
        );
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            if (service.validateClientRequired(PayloadPhase.PLAY))
                service.fireClientJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> service.fireClientDisconnect());
    }

    static <T extends CustomPacketPayload> void registerHandler(
        PayloadPhase phase,
        CustomPacketPayload.Type<T> type,
        PayloadHandler<T> handler
    ) {
        if (phase == PayloadPhase.PLAY) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(
                payload,
                new ClientPayloadContext(
                    PayloadPhase.PLAY,
                    context.player(),
                    context.responseSender()::sendPacket,
                    context.client().getConnection().getConnection()::disconnect,
                    context.client()
                )
            ));
        } else {
            ClientConfigurationNetworking.registerGlobalReceiver(type, (payload, context) -> {
                ClientPayloadContext payloadContext = new ClientPayloadContext(
                    PayloadPhase.CONFIGURATION,
                    null,
                    context.responseSender()::sendPacket,
                    context.packetContext().orElseThrow(PacketContext.CONNECTION)::disconnect,
                    context.client()
                );
                context.client().execute(() -> handler.handle(payload, payloadContext));
            });
        }
    }

    static void registerProbe(PayloadPhase phase, CustomPacketPayload.Type<?> type) {
        registerProbeTyped(phase, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerProbeTyped(PayloadPhase phase, CustomPacketPayload.Type type) {
        if (phase == PayloadPhase.PLAY) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            });
        } else {
            ClientConfigurationNetworking.registerGlobalReceiver(type, (payload, context) -> {
            });
        }
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    static boolean canSend(PayloadPhase phase, CustomPacketPayload.Type<?> type) {
        return phase == PayloadPhase.PLAY
            ? ClientPlayNetworking.canSend(type)
            : ClientConfigurationNetworking.canSend(type);
    }

    static void disconnect(Component reason) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener != null)
            listener.getConnection().disconnect(reason);
    }

    private record ClientPayloadContext(
        PayloadPhase phase,
        Player rawPlayer,
        java.util.function.Consumer<CustomPacketPayload> reply,
        java.util.function.Consumer<Component> disconnector,
        java.util.concurrent.Executor executor
    ) implements PayloadContext {
        @Override
        public LogicalSide receptionSide() {
            return LogicalSide.CLIENT;
        }

        @Override
        public Optional<MinecraftServer> server() {
            return Optional.empty();
        }

        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(rawPlayer);
        }

        @Override
        public Optional<ServerPlayer> sender() {
            return Optional.empty();
        }

        @Override
        public void reply(CustomPacketPayload payload) {
            reply.accept(Objects.requireNonNull(payload, "payload"));
        }

        @Override
        public void disconnect(Component reason) {
            disconnector.accept(Objects.requireNonNull(reason, "reason"));
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable work) {
            return CompletableFuture.runAsync(Objects.requireNonNull(work, "work"), executor);
        }
    }
}
