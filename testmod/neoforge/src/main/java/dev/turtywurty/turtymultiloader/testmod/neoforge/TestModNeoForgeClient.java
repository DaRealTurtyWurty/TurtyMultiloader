package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.testmod.ClientEventSmokeTest;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = TestModContent.MOD_ID, dist = Dist.CLIENT)
public final class TestModNeoForgeClient {
    public TestModNeoForgeClient() {
        ClientEventSmokeTest.register();
        TestModContent.NETWORK.registerClientHandler(
            PayloadPhase.PLAY,
            TestModContent.TEST_PAYLOAD_TYPE,
            (payload, context) -> {
            }
        );
    }
}
