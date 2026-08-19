package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.NeoForgeRegistryService;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.testmod.RegistryGameTests;
import dev.turtywurty.turtymultiloader.testmod.TestModContent;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(TestModContent.MOD_ID)
public final class TestModNeoForge {
    public TestModNeoForge(IEventBus modBus) {
        NeoForgeRegistryService.bind(modBus);
        TestModContent.initialize();
        RegistryService.get().apply();

        modBus.addListener(RegisterGameTestsEvent.class, TestModNeoForge::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, "default"),
            new TestEnvironmentDefinition.AllOf()
        );
        var testData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            RegistryGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, testData) {
                @Override
                public void run(GameTestHelper helper) {
                    RegistryGameTests.verifyRegistryService(helper);
                }
            }
        );
    }
}
