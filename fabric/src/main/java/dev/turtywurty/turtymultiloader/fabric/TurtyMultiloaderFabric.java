package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import net.fabricmc.api.ModInitializer;

public final class TurtyMultiloaderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TurtyMultiloader.init();
    }
}
