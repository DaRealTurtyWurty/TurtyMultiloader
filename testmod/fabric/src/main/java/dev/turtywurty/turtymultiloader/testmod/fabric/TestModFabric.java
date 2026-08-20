package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.testmod.EventSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import dev.turtywurty.turtymultiloader.testmod.WorldGenerationGameTests;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import net.fabricmc.api.ModInitializer;

public final class TestModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EventSmokeTest.register();
        WorldGenerationGameTests.register();
        TestModContent.initialize();
        RegistryService.get().apply();
        TransferService.get().apply();
        TestModContent.registerLateTransfers();
    }
}
