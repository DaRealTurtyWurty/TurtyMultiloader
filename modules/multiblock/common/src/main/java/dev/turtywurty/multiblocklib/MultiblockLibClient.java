package dev.turtywurty.multiblocklib;

import dev.turtywurty.multiblocklib.client.MultiblockControllerBlockEntityRenderer;
import dev.turtywurty.turtymultiloader.client.registration.ClientRegistrations;

public final class MultiblockLibClient {
    private MultiblockLibClient() {
    }

    public static void initialize() {
        ClientRegistrations.registerBlockEntityRenderer(
            MultiblockLib.MULTIBLOCK_CONTROLLER_ENTITY_HANDLE,
            context -> new MultiblockControllerBlockEntityRenderer()
        );
    }
}
