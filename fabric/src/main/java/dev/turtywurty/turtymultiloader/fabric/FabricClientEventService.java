package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.event.client.ClientEventService;
import dev.turtywurty.turtymultiloader.event.client.LevelRenderContext;
import dev.turtywurty.turtymultiloader.event.client.RenderStage;
import dev.turtywurty.turtymultiloader.event.client.TooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class FabricClientEventService implements ClientEventService {
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> levelEnterCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ClientLevel>> levelLeaveCallbacks = new CopyOnWriteArrayList<>();
    private ClientLevel currentLevel;

    public FabricClientEventService() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(this::onLevelChange);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> leaveCurrentLevel());
    }

    @Override
    public void onStartClientTick(Consumer<Minecraft> callback) {
        ClientTickEvents.START_CLIENT_TICK.register(require(callback)::accept);
    }

    @Override
    public void onEndClientTick(Consumer<Minecraft> callback) {
        ClientTickEvents.END_CLIENT_TICK.register(require(callback)::accept);
    }

    @Override
    public void onStartLevelTick(Consumer<ClientLevel> callback) {
        ClientTickEvents.START_LEVEL_TICK.register(require(callback)::accept);
    }

    @Override
    public void onEndLevelTick(Consumer<ClientLevel> callback) {
        ClientTickEvents.END_LEVEL_TICK.register(require(callback)::accept);
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
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(require(callback)::accept);
    }

    @Override
    public void onConnection(Consumer<Minecraft> callback) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> require(callback).accept(client));
    }

    @Override
    public void onDisconnection(Consumer<Minecraft> callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> require(callback).accept(client));
    }

    @Override
    public void onTooltip(TooltipCallback callback) {
        ItemTooltipCallback.EVENT.register(require(callback)::buildTooltip);
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        return KeyMappingHelper.registerKeyMapping(require(keyMapping));
    }

    @Override
    public void registerResourceReloadListener(Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(require(id), require(listener));
    }

    @Override
    public void onRenderStage(RenderStage stage, Consumer<LevelRenderContext> callback) {
        require(callback);
        switch (require(stage)) {
            case COLLECT_SUBMITS -> LevelRenderEvents.COLLECT_SUBMITS.register(context ->
                callback.accept(wrap(context))
            );
            case AFTER_SOLID_FEATURES -> LevelRenderEvents.AFTER_SOLID_FEATURES.register(context ->
                callback.accept(wrap(context))
            );
        }
    }

    private synchronized void onLevelChange(Minecraft client, ClientLevel nextLevel) {
        if (currentLevel == nextLevel)
            return;
        leaveCurrentLevel();
        currentLevel = nextLevel;
        levelEnterCallbacks.forEach(callback -> callback.accept(nextLevel));
    }

    private synchronized void leaveCurrentLevel() {
        if (currentLevel == null)
            return;
        ClientLevel previous = currentLevel;
        currentLevel = null;
        levelLeaveCallbacks.forEach(callback -> callback.accept(previous));
    }

    private static LevelRenderContext wrap(
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context
    ) {
        Minecraft client = Minecraft.getInstance();
        return new LevelRenderContext(
            client,
            Objects.requireNonNull(client.level, "No client level is active during level rendering"),
            context.gameRenderer(),
            context.levelRenderer(),
            context.levelState(),
            context.poseStack(),
            context.bufferSource(),
            context.submitNodeCollector()
        );
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "value");
    }
}
