package dev.turtywurty.gasapi.neoforge;

import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.neoforged.fml.common.Mod;

@Mod(GasApi.MOD_ID)
public final class GasApiNeoForge {
    public GasApiNeoForge() {
        GasApi.initialize();
        RegistryService.get().apply();
    }
}
