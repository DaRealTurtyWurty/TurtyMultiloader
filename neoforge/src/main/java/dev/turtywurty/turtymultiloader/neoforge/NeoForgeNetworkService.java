package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.network.*;
import dev.turtywurty.turtymultiloader.platform.LogicalSide;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.Consumer;

public final class NeoForgeNetworkService implements NetworkService {
    private static volatile IEventBus modBus;
    private static boolean clientBound;

    private final Map<Identifier, PayloadDeclaration<?, ?>> payloads = new LinkedHashMap<>();
    private final Map<Identifier, PayloadHandler<?>> clientHandlers = new LinkedHashMap<>();
    private final List<Consumer<ServerConnectionContext>> connectionCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joinCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> disconnectCallbacks = new ArrayList<>();
    private final List<Runnable> clientJoinCallbacks = new ArrayList<>();
    private final List<Runnable> clientDisconnectCallbacks = new ArrayList<>();
    private final List<LoginSyncProvider> loginSyncProviders = new ArrayList<>();

    public static synchronized void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge networking is already bound to a mod event bus");
        if (modBus != null)
            return;

        modBus = Objects.requireNonNull(bus, "bus");
        NeoForgeNetworkService service = instance();
        bus.addListener(RegisterPayloadHandlersEvent.class, service::registerPayloads);
        bus.addListener(RegisterConfigurationTasksEvent.class, service::onConfiguration);
        NeoForge.EVENT_BUS.addListener(service::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(service::onPlayerDisconnect);
    }

    public static synchronized void bindClient(IEventBus bus) {
        if (clientBound)
            return;
        clientBound = true;
        NeoForgeNetworkService service = instance();
        bus.addListener(RegisterClientPayloadHandlersEvent.class, service::registerClientHandlers);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, service::handleClientJoin);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, service::handleClientDisconnect);
    }

    private static NeoForgeNetworkService instance() {
        return (NeoForgeNetworkService) NetworkService.get();
    }

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
        if (payloads.putIfAbsent(type.id(), new PayloadDeclaration<>(
            phase, flow, type, codec, options, serverHandler
        )) != null)
            throw new IllegalStateException("Payload " + type.id() + " is already registered");
    }

    @Override
    public synchronized <T extends CustomPacketPayload> void registerClientHandler(
        PayloadPhase phase,
        CustomPacketPayload.Type<T> type,
        PayloadHandler<T> handler
    ) {
        Objects.requireNonNull(phase, "phase");
        PayloadDeclaration<?, ?> declaration = payloads.get(Objects.requireNonNull(type, "type").id());
        if (declaration == null || declaration.phase() != phase)
            throw new IllegalStateException("Payload " + type.id() + " is not registered for " + phase);
        if (declaration.flow() == PayloadFlow.SERVERBOUND)
            throw new IllegalStateException("Cannot attach a client handler to serverbound payload " + type.id());
        if (clientHandlers.putIfAbsent(type.id(), Objects.requireNonNull(handler, "handler")) != null)
            throw new IllegalStateException("A client handler is already registered for " + type.id());
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (!canSendToServer(payload.type()))
            throw new IllegalStateException("The server does not accept payload " + payload.type().id());
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(payload, "payload");
        if (!canSend(player, payload.type()))
            return false;
        PacketDistributor.sendToPlayer(player, payload);
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
        Objects.requireNonNull(server, "server").getPlayerList().getPlayers()
            .forEach(player -> sendToPlayer(player, payload));
    }

    @Override
    public void sendToTracking(Entity entity, CustomPacketPayload payload) {
        Objects.requireNonNull(entity, "entity");
        if (entity.level().isClientSide())
            throw new IllegalStateException("Cannot send clientbound payloads on the client");
        if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache)
            send(chunkCache.chunkMap.getPlayersWatching(entity), payload);
    }

    @Override
    public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        Objects.requireNonNull(level, "level");
        send(level.getChunkSource().chunkMap.getPlayers(
            ChunkPos.containing(Objects.requireNonNull(pos, "pos")),
            false
        ), payload);
    }

    @Override
    public void sendToDimension(ServerLevel level, CustomPacketPayload payload) {
        send(Objects.requireNonNull(level, "level").players(), payload);
    }

    @Override
    public void sendToNearby(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload) {
        if (radius < 0)
            throw new IllegalArgumentException("radius must be non-negative");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(payload, "payload");
        double radiusSquared = radius * radius;
        for (ServerPlayer player : Objects.requireNonNull(level, "level").players()) {
            if (player.position().distanceToSqr(position) < radiusSquared)
                sendToPlayer(player, payload);
        }
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        var listener = net.minecraft.client.Minecraft.getInstance().getConnection();
        return listener != null && ((ICommonPacketListener) listener).hasChannel(Objects.requireNonNull(type, "type"));
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ((ICommonPacketListener) Objects.requireNonNull(player, "player").connection)
            .hasChannel(Objects.requireNonNull(type, "type"));
    }

    private void send(Iterable<ServerPlayer> players, CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "payload");
        for (ServerPlayer player : players)
            sendToPlayer(player, payload);
    }

    @Override
    public synchronized void onConnection(Consumer<ServerConnectionContext> callback) {
        connectionCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public synchronized void onJoin(Consumer<ServerPlayer> callback) {
        joinCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    @Override
    public synchronized void onDisconnect(Consumer<ServerPlayer> callback) {
        disconnectCallbacks.add(Objects.requireNonNull(callback, "callback"));
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
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private synchronized void registerPayloads(RegisterPayloadHandlersEvent event) {
        for (PayloadDeclaration declaration : payloads.values()) {
            PayloadRegistrar registrar = event.registrar(declaration.options().protocolVersion());
            if (declaration.options().support() == PayloadSupport.OPTIONAL)
                registrar = registrar.optional();

            var handler = (net.neoforged.neoforge.network.handling.IPayloadHandler) (payload, context) ->
                declaration.serverHandler().handle(payload, neoContext(declaration.phase(), context));
            if (declaration.phase() == PayloadPhase.PLAY) {
                switch (declaration.flow()) {
                    case CLIENTBOUND -> registrar.playToClient(declaration.type(), declaration.codec());
                    case SERVERBOUND -> registrar.playToServer(declaration.type(), declaration.codec(), handler);
                    case BIDIRECTIONAL -> registrar.playBidirectional(declaration.type(), declaration.codec(), handler);
                }
            } else {
                switch (declaration.flow()) {
                    case CLIENTBOUND -> registrar.configurationToClient(declaration.type(), declaration.codec());
                    case SERVERBOUND ->
                        registrar.configurationToServer(declaration.type(), declaration.codec(), handler);
                    case BIDIRECTIONAL -> registrar.configurationBidirectional(
                        declaration.type(), declaration.codec(), handler
                    );
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private synchronized void registerClientHandlers(RegisterClientPayloadHandlersEvent event) {
        for (Map.Entry<Identifier, PayloadHandler<?>> entry : clientHandlers.entrySet()) {
            PayloadHandler handler = entry.getValue();
            PayloadDeclaration declaration = payloads.get(entry.getKey());
            event.register(declaration.type(), (payload, context) ->
                handler.handle(payload, neoContext(declaration.phase(), context))
            );
        }
    }

    private void onConfiguration(RegisterConfigurationTasksEvent event) {
        ServerConfigurationPacketListenerImpl listener = (ServerConfigurationPacketListenerImpl) event.getListener();
        ICommonPacketListener commonListener = (ICommonPacketListener) listener;
        MinecraftServer server = Objects.requireNonNull(
            ServerLifecycleHooks.getCurrentServer(),
            "No server is active during configuration"
        );
        ServerConnectionContext context = new ServerConnectionContext(
            server,
            commonListener.getConnection(),
            listener.getOwner()
        );
        server.execute(() -> connectionCallbacks.forEach(callback -> callback.accept(context)));
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        joinCallbacks.forEach(callback -> callback.accept(player));
        loginSyncProviders.forEach(provider -> sendToPlayer(player, provider.createPayloads(player)));
    }

    private void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            disconnectCallbacks.forEach(callback -> callback.accept(player));
    }

    private void handleClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        clientJoinCallbacks.forEach(Runnable::run);
    }

    private void handleClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clientDisconnectCallbacks.forEach(Runnable::run);
    }

    private static PayloadContext neoContext(PayloadPhase phase, IPayloadContext context) {
        boolean serverbound = context.flow() == PacketFlow.SERVERBOUND;
        Player player = phase == PayloadPhase.PLAY ? context.player() : null;
        ServerPlayer sender = serverbound && player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        MinecraftServer server = serverbound ? ServerLifecycleHooks.getCurrentServer() : null;
        return new NeoForgePayloadContext(phase, serverbound ? LogicalSide.SERVER : LogicalSide.CLIENT,
            server, player, sender, context);
    }

    private record PayloadDeclaration<B extends FriendlyByteBuf, T extends CustomPacketPayload>(
        PayloadPhase phase,
        PayloadFlow flow,
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super B, T> codec,
        PayloadRegistrationOptions options,
        PayloadHandler<T> serverHandler
    ) {
    }

    private record NeoForgePayloadContext(
        PayloadPhase phase,
        LogicalSide receptionSide,
        MinecraftServer rawServer,
        Player rawPlayer,
        ServerPlayer rawSender,
        IPayloadContext delegate
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
            delegate.reply(Objects.requireNonNull(payload, "payload"));
        }

        @Override
        public void disconnect(net.minecraft.network.chat.Component reason) {
            delegate.disconnect(Objects.requireNonNull(reason, "reason"));
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> enqueueWork(Runnable work) {
            return delegate.enqueueWork(Objects.requireNonNull(work, "work"));
        }
    }
}
