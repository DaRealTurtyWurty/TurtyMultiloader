package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.CommonMod;
import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TurtyMultiloader.MOD_ID)
public final class TurtyMultiloaderNeoForge {
    public TurtyMultiloaderNeoForge(IEventBus modBus) {
        CommonMod.init();
    }
}
