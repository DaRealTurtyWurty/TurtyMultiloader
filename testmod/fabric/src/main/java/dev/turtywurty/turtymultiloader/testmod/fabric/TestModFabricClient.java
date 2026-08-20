package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.menu.client.ClientMenus;
import dev.turtywurty.turtymultiloader.testmod.ClientEventSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.ClientRegistrationSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import dev.turtywurty.turtymultiloader.testmod.TestMenuScreen;
import net.fabricmc.api.ClientModInitializer;

public final class TestModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEventSmokeTest.register();
        ClientRegistrationSmokeTest.register();
        ClientMenus.register(TestModContent.TEST_MENU, TestMenuScreen::new);
        TestModContent.TEST_MENU_SYNC.registerClientReceiver((menu, value) -> {
        });
        TestModContent.NETWORK.registerClientHandler(
            PayloadPhase.PLAY,
            TestModContent.TEST_PAYLOAD_TYPE,
            (payload, context) -> {
            }
        );
    }
}
