package dev.turtywurty.multiblocklib.fabric;

import dev.turtywurty.multiblocklib.MultiblockLib;
import net.fabricmc.api.ModInitializer;

public final class MultiblockLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MultiblockLib.initialize();
    }
}
