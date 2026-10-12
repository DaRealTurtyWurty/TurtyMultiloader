package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.CommonClient;
import net.fabricmc.api.ClientModInitializer;
import dev.turtywurty.turtymultiloader.network.NetworkService;

public final class TurtyMultiloaderFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ((FabricNetworkService) NetworkService.get()).initializeClient();
        CommonClient.init();
    }
}
