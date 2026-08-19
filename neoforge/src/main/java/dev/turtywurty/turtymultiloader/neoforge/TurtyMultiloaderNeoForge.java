package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.CommonMod;
import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TurtyMultiloader.MOD_ID)
public final class TurtyMultiloaderNeoForge {
    public TurtyMultiloaderNeoForge(IEventBus modBus) {
        NeoForgeRegistryService.bind(modBus);
        NeoForgeTransferService.bind(modBus);
        CommonMod.init();
        TransferService.get().apply();
    }
}
