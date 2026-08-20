package dev.turtywurty.multiblocklib.fabric;

import dev.turtywurty.multiblocklib.MultiblockLibClient;
import net.fabricmc.api.ClientModInitializer;

public final class MultiblockLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MultiblockLibClient.initialize();
    }
}
