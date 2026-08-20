package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.CommonClient;
import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = TurtyMultiloader.MOD_ID, dist = Dist.CLIENT)
public final class TurtyMultiloaderNeoForgeClient {
    public TurtyMultiloaderNeoForgeClient(IEventBus modBus) {
        NeoForgeClientRegistrationService.bind(modBus);
        NeoForgeClientEventService.bind(modBus);
        NeoForgeClientMenuService.bind(modBus);
        NeoForgeNetworkService.bindClient(modBus);
        CommonClient.init();
    }
}
