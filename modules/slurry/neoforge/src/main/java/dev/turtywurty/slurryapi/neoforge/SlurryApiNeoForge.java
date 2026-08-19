package dev.turtywurty.slurryapi.neoforge;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(SlurryApi.MOD_ID)
public final class SlurryApiNeoForge {
    public SlurryApiNeoForge(IEventBus modBus) {
        SlurryApi.initialize();
        RegistryService.get().apply();
        if (FMLEnvironment.getDist() == Dist.CLIENT)
            SlurryApiNeoForgeClient.register(modBus);
    }
}
