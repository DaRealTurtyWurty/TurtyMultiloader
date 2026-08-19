package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.CommonClient;
import net.fabricmc.api.ClientModInitializer;

public final class TurtyMultiloaderFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommonClient.init();
    }
}
