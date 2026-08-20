package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.config.ConfigException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

public final class ConfigurationGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "configuration_service"
    );

    private ConfigurationGameTests() {
    }

    public static void verifyConfigurationService(GameTestHelper helper) {
        helper.assertTrue(TestModContent.TEST_CONFIG.isLoaded(), "Server config was not loaded during startup");
        helper.assertValueEqual(
            TestModContent.TEST_CONFIG.value(),
            new TestModContent.TestConfiguration(true, 1_000),
            "Server config defaults"
        );
        helper.assertTrue(
            TestModContent.TEST_CONFIG.path().orElseThrow().endsWith("config/turtymultiloader-test.json"),
            "Custom world-relative config path was not retained"
        );

        boolean rejected = false;
        try {
            TestModContent.TEST_CONFIG.set(new TestModContent.TestConfiguration(true, 0));
        } catch (ConfigException ignored) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Config validator accepted an invalid value");
        helper.succeed();
    }
}
