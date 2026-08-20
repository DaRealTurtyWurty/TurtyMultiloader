package dev.turtywurty.turtymultiloader.event.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Client-only loader implementation for {@link ClientEvents}. Never resolve this service from a dedicated server.
 */
public interface ClientEventService {
    static ClientEventService get() {
        return ServiceHolder.INSTANCE;
    }

    void onStartClientTick(Consumer<Minecraft> callback);

    void onEndClientTick(Consumer<Minecraft> callback);

    void onStartLevelTick(Consumer<ClientLevel> callback);

    void onEndLevelTick(Consumer<ClientLevel> callback);

    void onLevelEnter(Consumer<ClientLevel> callback);

    void onLevelLeave(Consumer<ClientLevel> callback);

    void onBlockEntityUnload(BiConsumer<BlockEntity, ClientLevel> callback);

    void onConnection(Consumer<Minecraft> callback);

    void onDisconnection(Consumer<Minecraft> callback);

    void onTooltip(TooltipCallback callback);

    KeyMapping registerKeyMapping(KeyMapping keyMapping);

    void registerResourceReloadListener(Identifier id, PreparableReloadListener listener);

    void onRenderStage(RenderStage stage, Consumer<LevelRenderContext> callback);

    final class ServiceHolder {
        private static final ClientEventService INSTANCE = ServiceLoader.load(
                ClientEventService.class,
                ClientEventService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client event service is available"));

        private ServiceHolder() {
        }
    }
}
