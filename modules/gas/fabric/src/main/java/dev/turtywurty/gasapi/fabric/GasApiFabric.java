package dev.turtywurty.gasapi.fabric;

import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.fabricmc.api.ModInitializer;

public final class GasApiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GasApi.initialize();
        RegistryService.get().apply();
    }
}
