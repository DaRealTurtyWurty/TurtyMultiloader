package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.testmod.RegistryGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class TestModFabricGameTests {
    @GameTest(structure = "minecraft:empty")
    public void registryService(GameTestHelper helper) {
        RegistryGameTests.verifyRegistryService(helper);
    }
}
