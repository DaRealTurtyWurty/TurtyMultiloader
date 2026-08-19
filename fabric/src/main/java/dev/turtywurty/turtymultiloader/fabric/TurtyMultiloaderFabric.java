package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.CommonMod;
import net.fabricmc.api.ModInitializer;

public final class TurtyMultiloaderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonMod.init();
    }
}
