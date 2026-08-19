package dev.turtywurty.slurryapi.fabric;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.fabricmc.api.ModInitializer;

public final class SlurryApiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SlurryApi.initialize();
        RegistryService.get().apply();
    }
}
