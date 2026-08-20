package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.event.client.ClientEventService;
import dev.turtywurty.turtymultiloader.event.client.LevelRenderContext;
import dev.turtywurty.turtymultiloader.event.client.RenderStage;
import dev.turtywurty.turtymultiloader.event.client.TooltipCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NeoForgeClientEventService implements ClientEventService {
    private static IEventBus modBus;

    private final CopyOnWriteArrayList<Consumer<Minecraft>> startClientTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Minecraft>> endClientTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> startLevelTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> endLevelTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> levelEnterCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> levelLeaveCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BiConsumer<BlockEntity, ClientLevel>> blockEntityUnloadCallbacks =
        new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Minecraft>> connectionCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Minecraft>> disconnectionCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TooltipCallback> tooltipCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<LevelRenderContext>> collectSubmitsCallbacks =
        new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<LevelRenderContext>> afterSolidFeaturesCallbacks =
        new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<KeyMapping> keyMappings = new CopyOnWriteArrayList<>();
    private final Map<Identifier, PreparableReloadListener> reloadListeners = new LinkedHashMap<>();
    private boolean keyMappingRegistrationFinished;
    private boolean reloadListenerRegistrationFinished;

    public NeoForgeClientEventService() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Pre.class, this::handleStartClientTick);
        bus.addListener(ClientTickEvent.Post.class, this::handleEndClientTick);
        bus.addListener(LevelTickEvent.Pre.class, this::handleStartLevelTick);
        bus.addListener(LevelTickEvent.Post.class, this::handleEndLevelTick);
        bus.addListener(LevelEvent.Load.class, this::handleLevelEnter);
        bus.addListener(LevelEvent.Unload.class, this::handleLevelLeave);
        bus.addListener(ClientPlayerNetworkEvent.LoggingIn.class, this::handleConnection);
        bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, this::handleDisconnection);
        bus.addListener(ItemTooltipEvent.class, this::handleTooltip);
        bus.addListener(SubmitCustomGeometryEvent.class, this::handleCollectSubmits);
        bus.addListener(RenderLevelStageEvent.AfterOpaqueFeatures.class, this::handleAfterSolidFeatures);
    }

    public static synchronized void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge client events are already bound to a mod event bus");
        if (modBus != null)
            return;
        modBus = Objects.requireNonNull(bus, "bus");
        NeoForgeClientEventService service = instance();
        bus.addListener(RegisterKeyMappingsEvent.class, service::registerKeyMappings);
        bus.addListener(AddClientReloadListenersEvent.class, service::registerReloadListeners);
    }

    public static void fireBlockEntityUnload(BlockEntity blockEntity, ClientLevel level) {
        instance().blockEntityUnloadCallbacks.forEach(callback -> callback.accept(blockEntity, level));
    }

    @Override
    public void onStartClientTick(Consumer<Minecraft> callback) {
        startClientTickCallbacks.add(require(callback));
    }

    @Override
    public void onEndClientTick(Consumer<Minecraft> callback) {
        endClientTickCallbacks.add(require(callback));
    }

    @Override
    public void onStartLevelTick(Consumer<ClientLevel> callback) {
        startLevelTickCallbacks.add(require(callback));
    }

    @Override
    public void onEndLevelTick(Consumer<ClientLevel> callback) {
        endLevelTickCallbacks.add(require(callback));
    }

    @Override
    public void onLevelEnter(Consumer<ClientLevel> callback) {
        levelEnterCallbacks.add(require(callback));
    }

    @Override
    public void onLevelLeave(Consumer<ClientLevel> callback) {
        levelLeaveCallbacks.add(require(callback));
    }

    @Override
    public void onBlockEntityUnload(BiConsumer<BlockEntity, ClientLevel> callback) {
        blockEntityUnloadCallbacks.add(require(callback));
    }

    @Override
    public void onConnection(Consumer<Minecraft> callback) {
        connectionCallbacks.add(require(callback));
    }

    @Override
    public void onDisconnection(Consumer<Minecraft> callback) {
        disconnectionCallbacks.add(require(callback));
    }

    @Override
    public void onTooltip(TooltipCallback callback) {
        tooltipCallbacks.add(require(callback));
    }

    @Override
    public synchronized KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        if (keyMappingRegistrationFinished)
            throw new IllegalStateException("Key mappings must be registered during client initialization");
        keyMappings.add(require(keyMapping));
        return keyMapping;
    }

    @Override
    public synchronized void registerResourceReloadListener(Identifier id, PreparableReloadListener listener) {
        if (reloadListenerRegistrationFinished)
            throw new IllegalStateException("Resource reload listeners must be registered during client initialization");
        if (reloadListeners.putIfAbsent(require(id), require(listener)) != null)
            throw new IllegalStateException("A resource reload listener is already registered as " + id);
    }

    @Override
    public void onRenderStage(RenderStage stage, Consumer<LevelRenderContext> callback) {
        switch (require(stage)) {
            case COLLECT_SUBMITS -> collectSubmitsCallbacks.add(require(callback));
            case AFTER_SOLID_FEATURES -> afterSolidFeaturesCallbacks.add(require(callback));
        }
    }

    private void handleStartClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        startClientTickCallbacks.forEach(callback -> callback.accept(client));
    }

    private void handleEndClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        endClientTickCallbacks.forEach(callback -> callback.accept(client));
    }

    private void handleStartLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ClientLevel level)
            startLevelTickCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleEndLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ClientLevel level)
            endLevelTickCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleLevelEnter(LevelEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level)
            levelEnterCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleLevelLeave(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level)
            levelLeaveCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleConnection(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft client = Minecraft.getInstance();
        connectionCallbacks.forEach(callback -> callback.accept(client));
    }

    private void handleDisconnection(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft client = Minecraft.getInstance();
        disconnectionCallbacks.forEach(callback -> callback.accept(client));
    }

    private void handleTooltip(ItemTooltipEvent event) {
        tooltipCallbacks.forEach(callback -> callback.buildTooltip(
            event.getItemStack(),
            event.getContext(),
            event.getFlags(),
            event.getToolTip()
        ));
    }

    private void handleCollectSubmits(SubmitCustomGeometryEvent event) {
        LevelRenderContext context = wrap(
            event.getLevelRenderState(),
            event.getPoseStack(),
            event.getSubmitNodeCollector()
        );
        collectSubmitsCallbacks.forEach(callback -> callback.accept(context));
    }

    private void handleAfterSolidFeatures(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        LevelRenderContext context = wrap(event.getLevelRenderState(), event.getPoseStack(), null);
        afterSolidFeaturesCallbacks.forEach(callback -> callback.accept(context));
    }

    private synchronized void registerKeyMappings(RegisterKeyMappingsEvent event) {
        keyMappings.forEach(event::register);
        keyMappingRegistrationFinished = true;
    }

    private synchronized void registerReloadListeners(AddClientReloadListenersEvent event) {
        reloadListeners.forEach(event::addListener);
        reloadListenerRegistrationFinished = true;
    }

    private static LevelRenderContext wrap(
        net.minecraft.client.renderer.state.level.LevelRenderState levelState,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector
    ) {
        Minecraft client = Minecraft.getInstance();
        return new LevelRenderContext(
            client,
            Objects.requireNonNull(client.level, "No client level is active during level rendering"),
            client.gameRenderer,
            client.levelRenderer,
            levelState,
            poseStack,
            client.renderBuffers().bufferSource(),
            submitNodeCollector
        );
    }

    private static NeoForgeClientEventService instance() {
        return (NeoForgeClientEventService) ClientEventService.get();
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
