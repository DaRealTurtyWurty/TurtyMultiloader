package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import net.fabricmc.api.ModInitializer;

public final class TestModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TestModContent.initialize();
        RegistryService.get().apply();
    }
}
