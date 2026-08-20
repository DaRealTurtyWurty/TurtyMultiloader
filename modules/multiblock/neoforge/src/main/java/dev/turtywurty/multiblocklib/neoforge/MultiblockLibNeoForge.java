package dev.turtywurty.multiblocklib.neoforge;

import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.MultiblockLibClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MultiblockLib.MOD_ID)
public final class MultiblockLibNeoForge {
    public MultiblockLibNeoForge(IEventBus modBus) {
        MultiblockLib.initialize();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            MultiblockLibClient.initialize();
        }
    }
}
