package dev.turtywurty.turtymultiloader.testmod.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.datagen.NeoForgeDataGeneration;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.testmod.*;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(TestModContent.MOD_ID)
public final class TestModNeoForge {
    public TestModNeoForge(IEventBus modBus) {
        EventSmokeTest.register();
        WorldGenerationGameTests.register();
        TestModContent.initialize();
        RegistryService.get().apply();
        TransferService.get().apply();
        TestModContent.registerLateTransfers();
        TransferService.get().apply();

        modBus.addListener(RegisterGameTestsEvent.class, TestModNeoForge::registerGameTests);
        modBus.addListener(GatherDataEvent.Client.class, event ->
            NeoForgeDataGeneration.run(event, TestDataGeneration.SPEC)
        );
        modBus.addListener(GatherDataEvent.Server.class, event ->
            NeoForgeDataGeneration.run(event, TestDataGeneration.SPEC)
        );
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
        var menuTestData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            MenuGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, menuTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    MenuGameTests.verifyMenuService(helper);
                }
            }
        );
        var attachmentTestData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            AttachmentGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, attachmentTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    AttachmentGameTests.verifyAttachmentService(helper);
                }
            }
        );
        var transferTestData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            TransferGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, transferTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    TransferGameTests.verifyTransferService(helper);
                }
            }
        );
        event.registerTest(
            NeoForgeTransferGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, transferTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    NeoForgeTransferGameTests.verifyNestedTransaction(helper);
                }
            }
        );
        event.registerTest(
            NeoForgeTransferGameTests.LONG_TRANSFER_TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, transferTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    NeoForgeTransferGameTests.verifyLongTransferIsBounded(helper);
                }
            }
        );
        var worldGenerationTestData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            WorldGenerationGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, worldGenerationTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    WorldGenerationGameTests.verifyWorldGenerationService(helper);
                }
            }
        );
        var configTestData = new TestData<>(environment, RegistryGameTests.EMPTY_STRUCTURE, 20, 0, true);
        event.registerTest(
            ConfigurationGameTests.TEST_ID,
            new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, configTestData) {
                @Override
                public void run(GameTestHelper helper) {
                    ConfigurationGameTests.verifyConfigurationService(helper);
                }
            }
        );
    }
}
