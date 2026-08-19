package dev.turtywurty.turtymultiloader.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RegistryGameTests {
    public static final Identifier TEST_ID = id("registry_service");
    public static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath("minecraft", "empty");

    private RegistryGameTests() {
    }

    public static void verifyRegistryService(GameTestHelper helper) {
        helper.assertTrue(TestModContent.REGISTRIES.isApplied(), "RegistryService was not applied");
        helper.assertTrue(TestModContent.TEST_LOG.holder().isBound(), "Block holder was not bound");
        helper.assertTrue(TestModContent.TEST_VALUE.holder().isBound(), "Custom-registry holder was not bound");
        helper.assertValueEqual(
            BuiltInRegistries.BLOCK.getValue(TestModContent.TEST_LOG.id()),
            TestModContent.TEST_LOG.get(),
            "Block registry value"
        );
        helper.assertValueEqual(
            BuiltInRegistries.ITEM.getValue(TestModContent.TEST_LOG_ITEM.id()),
            TestModContent.TEST_LOG_ITEM.get(),
            "Item registry value"
        );
        helper.assertValueEqual(
            TestModContent.TEST_VALUE_REGISTRY.get().getValue(TestModContent.TEST_VALUE.id()),
            TestModContent.TEST_VALUE.get(),
            "Custom registry value"
        );
        helper.assertValueEqual(
            TestModContent.TEST_WOOD_TYPE.get().name(),
            TestModContent.MOD_ID + ":test",
            "Queued wood type"
        );

        ItemStack stack = new ItemStack(TestModContent.TEST_LOG_ITEM.get());
        stack.set(TestModContent.TEST_NUMBER.get(), 42);
        helper.assertValueEqual(stack.get(TestModContent.TEST_NUMBER.get()), 42, "Data component value");

        BlockPos testPosition = new BlockPos(1, 1, 1);
        helper.setBlock(testPosition, TestModContent.TEST_LOG.get());
        helper.assertBlockPresent(TestModContent.TEST_LOG.get(), testPosition);
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, path);
    }
}
