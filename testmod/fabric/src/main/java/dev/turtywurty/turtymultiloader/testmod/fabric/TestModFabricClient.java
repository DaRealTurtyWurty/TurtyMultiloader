package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.testmod.ClientEventSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import net.fabricmc.api.ClientModInitializer;

public final class TestModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEventSmokeTest.register();
        TestModContent.NETWORK.registerClientHandler(
            PayloadPhase.PLAY,
            TestModContent.TEST_PAYLOAD_TYPE,
            (payload, context) -> {
            }
        );
    }
}
