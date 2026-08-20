package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.menu.client.ClientMenus;
import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.testmod.ClientEventSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.ClientRegistrationSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.TestMenuScreen;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = TestModContent.MOD_ID, dist = Dist.CLIENT)
public final class TestModNeoForgeClient {
    public TestModNeoForgeClient() {
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
