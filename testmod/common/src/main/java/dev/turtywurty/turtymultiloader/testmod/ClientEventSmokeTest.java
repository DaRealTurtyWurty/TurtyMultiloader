package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.event.client.ClientEvents;
import dev.turtywurty.turtymultiloader.event.client.RenderStage;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class ClientEventSmokeTest {
    private ClientEventSmokeTest() {
    }

    public static void register() {
        ClientEvents.onStartClientTick(client -> {
        });
        ClientEvents.onEndClientTick(client -> {
        });
        ClientEvents.onStartLevelTick(level -> {
        });
        ClientEvents.onEndLevelTick(level -> {
        });
        ClientEvents.onLevelEnter(level -> {
        });
        ClientEvents.onLevelLeave(level -> {
        });
        ClientEvents.onBlockEntityUnload((blockEntity, level) -> {
        });
        ClientEvents.onConnection(client -> {
        });
        ClientEvents.onDisconnection(client -> {
        });
        ClientEvents.onTooltip((stack, context, flag, lines) -> {
        });
        ClientEvents.registerKeyMapping(new KeyMapping(
            "key.turtymultiloader_testmod.event_smoke_test",
            -1,
            KeyMapping.Category.MISC
        ));
        ClientEvents.registerResourceReloadListener(
            Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, "event_smoke_test"),
            (ResourceManagerReloadListener) resourceManager -> {
            }
        );
        ClientEvents.onRenderStage(RenderStage.COLLECT_SUBMITS, context -> {
        });
        ClientEvents.onRenderStage(RenderStage.AFTER_SOLID_FEATURES, context -> {
        });
    }
}
