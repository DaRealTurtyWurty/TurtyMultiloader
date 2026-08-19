package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.network.*;
import dev.turtywurty.turtymultiloader.platform.LogicalSide;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class FabricNetworkService implements NetworkService {
    private final Map<PayloadKey, PayloadDeclaration<?, ?>> payloads = new LinkedHashMap<>();
    private final List<Consumer<ServerConnectionContext>> connectionCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joinCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> disconnectCallbacks = new ArrayList<>();
    private final List<Runnable> clientJoinCallbacks = new ArrayList<>();
    private final List<Runnable> clientDisconnectCallbacks = new ArrayList<>();
    private final List<LoginSyncProvider> loginSyncProviders = new ArrayList<>();
    private final Set<PayloadKey> clientProbes = new HashSet<>();
    private boolean serverEventsRegistered;
    private boolean clientEventsRegistered;

    @Override
    public synchronized <B extends FriendlyByteBuf, T extends CustomPacketPayload> void registerPayload(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler
    ) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(options, "options");
        if (flow != PayloadFlow.CLIENTBOUND)
            Objects.requireNonNull(serverHandler, "serverHandler");

        PayloadKey key = new PayloadKey(phase, type.id());
        if (payloads.values().stream().anyMatch(existing -> existing.type().id().equals(type.id())))
            throw new IllegalStateException("Payload " + type.id() + " is already registered");

        VersionProbe probe = VersionProbe.create(phase, flow, type.id(), options.protocolVersion());
        PayloadDeclaration<B, T> declaration = new PayloadDeclaration<>(
            phase, flow, type, codec, options, serverHandler, probe
        );
        payloads.put(key, declaration);
        registerTypes(declaration);
        registerServerHandler(declaration);
        if (clientEventsRegistered)
            registerClientProbe(declaration);
        registerServerEvents();
    }

    @Override
    public synchronized <T extends CustomPacketPayload> void registerClientHandler(
        PayloadPhase phase,
        CustomPacketPayload.Type<T> type,
        PayloadHandler<T> handler
    ) {
        PayloadDeclaration<?, ?> untyped = payloads.get(new PayloadKey(
            Objects.requireNonNull(phase, "phase"),
            Objects.requireNonNull(type, "type").id()
        ));
        if (untyped == null)
            throw new IllegalStateException("Payload " + type.id() + " is not registered for " + phase);
        if (untyped.flow() == PayloadFlow.SERVERBOUND)
            throw new IllegalStateException("Cannot attach a client handler to serverbound payload " + type.id());

        FabricClientNetworkHooks.registerHandler(phase, type, Objects.requireNonNull(handler, "handler"));
    }

    public synchronized void initializeClient() {
        if (clientEventsRegistered)
            return;
        clientEventsRegistered = true;
        payloads.values().forEach(this::registerClientProbe);
        FabricClientNetworkHooks.initialize(this);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (!canSendToServer(payload.type()))
            throw new IllegalStateException("The server does not accept payload " + payload.type().id());
        FabricClientNetworkHooks.sendToServer(payload);
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(payload, "payload");
        if (!canSend(player, payload.type()))
            return false;
        ServerPlayNetworking.send(player, payload);
        return true;
    }

    @Override
    public int sendToPlayer(ServerPlayer player, Iterable<? extends CustomPacketPayload> payloads) {
        int sent = 0;
        for (CustomPacketPayload payload : Objects.requireNonNull(payloads, "payloads")) {
            if (sendToPlayer(player, Objects.requireNonNull(payload, "payload")))
                sent++;
        }
        return sent;
    }

    @Override
    public void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        send(PlayerLookup.all(Objects.requireNonNull(server, "server")), payload);
    }

    @Override
    public void sendToTracking(Entity entity, CustomPacketPayload payload) {
        send(PlayerLookup.tracking(Objects.requireNonNull(entity, "entity")), payload);
    }

    @Override
    public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        send(PlayerLookup.tracking(
            Objects.requireNonNull(level, "level"),
            Objects.requireNonNull(pos, "pos")
        ), payload);
    }

    @Override
    public void sendToDimension(ServerLevel level, CustomPacketPayload payload) {
        send(PlayerLookup.level(Objects.requireNonNull(level, "level")), payload);
    }

    @Override
    public void sendToNearby(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload) {
        if (radius < 0)
            throw new IllegalArgumentException("radius must be non-negative");
        send(PlayerLookup.around(
            Objects.requireNonNull(level, "level"),
            Objects.requireNonNull(position, "position"),
            radius
        ), payload);
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(type, "type");
        PayloadDeclaration<?, ?> declaration = declaration(PayloadPhase.PLAY, type.id());
        return declaration != null
            && declaration.flow() != PayloadFlow.CLIENTBOUND
            && FabricClientNetworkHooks.canSend(PayloadPhase.PLAY, type)
            && FabricClientNetworkHooks.canSend(PayloadPhase.PLAY, declaration.probe().type());
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");
        PayloadDeclaration<?, ?> declaration = declaration(PayloadPhase.PLAY, type.id());
        return declaration != null
            && declaration.flow() != PayloadFlow.SERVERBOUND
            && ServerPlayNetworking.canSend(player, type)
            && ServerPlayNetworking.canSend(player, declaration.probe().type());
    }

    @Override
    public synchronized void onConnection(Consumer<ServerConnectionContext> callback) {
        connectionCallbacks.add(Objects.requireNonNull(callback, "callback"));
        registerServerEvents();
    }

    @Override
    public synchronized void onJoin(Consumer<ServerPlayer> callback) {
        joinCallbacks.add(Objects.requireNonNull(callback, "callback"));
        registerServerEvents();
    }

    @Override
    public synchronized void onDisconnect(Consumer<ServerPlayer> callback) {
        disconnectCallbacks.add(Objects.requireNonNull(callback, "callback"));
        registerServerEvents();
    }

    @Override
    public synchronized void onClientJoin(Runnable callback) {
        clientJoinCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public synchronized void onClientDisconnect(Runnable callback) {
        clientDisconnectCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public synchronized void addLoginSync(LoginSyncProvider provider) {
        loginSyncProviders.add(Objects.requireNonNull(provider, "provider"));
        registerServerEvents();
    }

    private synchronized void registerServerEvents() {
        if (serverEventsRegistered)
            return;
        serverEventsRegistered = true;
        ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
            ServerConnectionContext connectionContext = new ServerConnectionContext(
                server,
                listener.getPacketContext().orElseThrow(PacketContext.CONNECTION),
                listener.getOwner()
            );
            server.execute(() -> connectionCallbacks.forEach(callback -> callback.accept(connectionContext)));
            validateServerRequired(PayloadPhase.CONFIGURATION, listener);
        });
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            ServerPlayer player = listener.getPlayer();
            if (!validateServerRequired(PayloadPhase.PLAY, player))
                return;
            joinCallbacks.forEach(callback -> callback.accept(player));
            loginSyncProviders.forEach(provider -> sendToPlayer(player, provider.createPayloads(player)));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) ->
            disconnectCallbacks.forEach(callback -> callback.accept(listener.getPlayer()))
        );
    }

    private void send(Iterable<ServerPlayer> players, CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "payload");
        for (ServerPlayer player : players) {
            sendToPlayer(player, payload);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerTypes(PayloadDeclaration<?, ?> declaration) {
        PayloadTypeRegistry clientbound = declaration.phase() == PayloadPhase.PLAY
            ? PayloadTypeRegistry.clientboundPlay()
            : PayloadTypeRegistry.clientboundConfiguration();
        PayloadTypeRegistry serverbound = declaration.phase() == PayloadPhase.PLAY
            ? PayloadTypeRegistry.serverboundPlay()
            : PayloadTypeRegistry.serverboundConfiguration();
        if (declaration.flow() != PayloadFlow.SERVERBOUND) {
            clientbound.register(declaration.type(), declaration.codec());
            clientbound.register(declaration.probe().type(), declaration.probe().codec());
        }
        if (declaration.flow() != PayloadFlow.CLIENTBOUND) {
            serverbound.register(declaration.type(), declaration.codec());
            serverbound.register(declaration.probe().type(), declaration.probe().codec());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerServerHandler(PayloadDeclaration<?, ?> declaration) {
        if (declaration.flow() == PayloadFlow.CLIENTBOUND)
            return;
        PayloadHandler handler = declaration.serverHandler();
        if (declaration.phase() == PayloadPhase.PLAY) {
            ServerPlayNetworking.registerGlobalReceiver(declaration.probe().type(), (payload, context) -> {
            });
            ServerPlayNetworking.registerGlobalReceiver(declaration.type(), (payload, context) -> handler.handle(
                payload,
                new FabricPayloadContext(
                    PayloadPhase.PLAY,
                    LogicalSide.SERVER,
                    context.server(),
                    context.player(),
                    context.player(),
                    context.responseSender()::sendPacket,
                    context.player().connection::disconnect,
                    context.server()
                )
            ));
        } else {
            ServerConfigurationNetworking.registerGlobalReceiver(declaration.probe().type(), (payload, context) -> {
            });
            ServerConfigurationNetworking.registerGlobalReceiver(declaration.type(), (payload, context) -> {
                FabricPayloadContext payloadContext = new FabricPayloadContext(
                    PayloadPhase.CONFIGURATION,
                    LogicalSide.SERVER,
                    context.server(),
                    null,
                    null,
                    context.responseSender()::sendPacket,
                    context.packetListener()::disconnect,
                    context.server()
                );
                context.server().execute(() -> handler.handle(payload, payloadContext));
            });
        }
    }

    private synchronized void registerClientProbe(PayloadDeclaration<?, ?> declaration) {
        if (declaration.flow() == PayloadFlow.SERVERBOUND)
            return;
        PayloadKey key = new PayloadKey(declaration.phase(), declaration.probe().type().id());
        if (!clientProbes.add(key))
            return;
        if (declaration.phase() == PayloadPhase.PLAY) {
            FabricClientNetworkHooks.registerProbe(PayloadPhase.PLAY, declaration.probe().type());
        } else {
            FabricClientNetworkHooks.registerProbe(PayloadPhase.CONFIGURATION, declaration.probe().type());
        }
    }

    private boolean validateServerRequired(PayloadPhase phase, Object listener) {
        for (PayloadDeclaration<?, ?> declaration : payloads.values()) {
            if (declaration.phase() != phase || declaration.options().support() != PayloadSupport.REQUIRED)
                continue;
            boolean supported = true;
            if (listener instanceof ServerPlayer player) {
                if (declaration.flow() != PayloadFlow.SERVERBOUND)
                    supported &= ServerPlayNetworking.canSend(player, declaration.type())
                        && ServerPlayNetworking.canSend(player, declaration.probe().type());
                if (declaration.flow() != PayloadFlow.CLIENTBOUND)
                    supported &= ServerPlayNetworking.getReceived(player).contains(declaration.type().id())
                        && ServerPlayNetworking.getReceived(player).contains(declaration.probe().type().id());
                if (!supported) {
                    player.connection.disconnect(incompatible(declaration));
                    return false;
                }
            } else if (listener instanceof net.minecraft.server.network.ServerConfigurationPacketListenerImpl config) {
                if (declaration.flow() != PayloadFlow.SERVERBOUND)
                    supported &= ServerConfigurationNetworking.canSend(config, declaration.type())
                        && ServerConfigurationNetworking.canSend(config, declaration.probe().type());
                if (declaration.flow() != PayloadFlow.CLIENTBOUND)
                    supported &= ServerConfigurationNetworking.getReceived(config).contains(declaration.type().id())
                        && ServerConfigurationNetworking.getReceived(config).contains(declaration.probe().type().id());
                if (!supported) {
                    config.disconnect(incompatible(declaration));
                    return false;
                }
            }
        }
        return true;
    }

    boolean validateClientRequired(PayloadPhase phase) {
        for (PayloadDeclaration<?, ?> declaration : payloads.values()) {
            if (declaration.phase() != phase || declaration.flow() == PayloadFlow.CLIENTBOUND
                || declaration.options().support() != PayloadSupport.REQUIRED)
                continue;
            boolean supported = phase == PayloadPhase.PLAY
                ? FabricClientNetworkHooks.canSend(PayloadPhase.PLAY, declaration.type())
                    && FabricClientNetworkHooks.canSend(PayloadPhase.PLAY, declaration.probe().type())
                : FabricClientNetworkHooks.canSend(PayloadPhase.CONFIGURATION, declaration.type())
                    && FabricClientNetworkHooks.canSend(PayloadPhase.CONFIGURATION, declaration.probe().type());
            if (!supported) {
                FabricClientNetworkHooks.disconnect(incompatible(declaration));
                return false;
            }
        }
        return true;
    }

    void fireClientJoin() {
        clientJoinCallbacks.forEach(Runnable::run);
    }

    void fireClientDisconnect() {
        clientDisconnectCallbacks.forEach(Runnable::run);
    }

    private static Component incompatible(PayloadDeclaration<?, ?> declaration) {
        return Component.literal("Incompatible network payload " + declaration.type().id()
            + " (required protocol " + declaration.options().protocolVersion() + ")");
    }

    private PayloadDeclaration<?, ?> declaration(PayloadPhase phase, Identifier id) {
        return payloads.get(new PayloadKey(phase, id));
    }

    private record PayloadKey(PayloadPhase phase, Identifier id) {
    }

    private record PayloadDeclaration<B extends FriendlyByteBuf, T extends CustomPacketPayload>(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler,
        VersionProbe probe
    ) {
    }

    private record VersionProbe(
        CustomPacketPayload.Type<VersionProbePayload> type,
        StreamCodec<FriendlyByteBuf, VersionProbePayload> codec
    ) {
        private static VersionProbe create(
            PayloadPhase phase,
            PayloadFlow flow,
            Identifier payloadId,
            String version
        ) {
            String fingerprintSource = phase.name() + '\0' + flow.name() + '\0' + payloadId + '\0' + version;
            String fingerprint = UUID.nameUUIDFromBytes(
                fingerprintSource.getBytes(StandardCharsets.UTF_8)
            ).toString();
            Identifier id = Identifier.fromNamespaceAndPath(
                payloadId.getNamespace(),
                "_tml_protocol/" + payloadId.getPath() + "/" + fingerprint
            );
            CustomPacketPayload.Type<VersionProbePayload> type = new CustomPacketPayload.Type<>(id);
            VersionProbePayload payload = new VersionProbePayload(type);
            return new VersionProbe(type, StreamCodec.unit(payload));
        }
    }

    private record VersionProbePayload(
        CustomPacketPayload.Type<VersionProbePayload> type
    ) implements CustomPacketPayload {
    }

    private record FabricPayloadContext(
        PayloadPhase phase,
        LogicalSide receptionSide,
        MinecraftServer rawServer,
        Player rawPlayer,
        ServerPlayer rawSender,
        Consumer<CustomPacketPayload> reply,
        Consumer<Component> disconnector,
        Executor executor
    ) implements PayloadContext {
        @Override
        public Optional<MinecraftServer> server() {
            return Optional.ofNullable(rawServer);
        }

        @Override
        public Optional<Player> player() {
            return Optional.ofNullable(rawPlayer);
        }

        @Override
        public Optional<ServerPlayer> sender() {
            return Optional.ofNullable(rawSender);
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
