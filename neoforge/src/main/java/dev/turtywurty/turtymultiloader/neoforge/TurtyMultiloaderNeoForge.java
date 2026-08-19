package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import net.neoforged.fml.common.Mod;

@Mod(TurtyMultiloader.MOD_ID)
public final class TurtyMultiloaderNeoForge {
    public TurtyMultiloaderNeoForge() {
        TurtyMultiloader.init();
    }
}
