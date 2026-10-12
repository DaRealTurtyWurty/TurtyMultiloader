package dev.turtywurty.turtymultiloader.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RegistryGameTests {
    public static final Identifier TEST_ID = id("registry_service");
    public static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath("minecraft", "empty");

    private RegistryGameTests() {
    }

    public static void verifyRegistryService(GameTestHelper helper) {
        helper.assertTrue(TestModContent.REGISTRIES.isApplied(), "RegistryService was not applied");
        helper.assertTrue(TestModContent.TEST_LOG.holder().isBound(), "Block holder was not bound");
        helper.assertTrue(TestModContent.TEST_ENTITY.holder().isBound(), "Entity-type holder was not bound");
        helper.assertTrue(TestModContent.TEST_CREATIVE_TAB.holder().isBound(), "Creative-tab holder was not bound");
        helper.assertTrue(TestModContent.TEST_VALUE.holder().isBound(), "Custom-registry holder was not bound");
        helper.assertValueEqual(
            BuiltInRegistries.BLOCK.getValue(TestModContent.TEST_LOG.id()),
            TestModContent.TEST_LOG.get(),
            "Block registry value");
        helper.assertValueEqual(
            BuiltInRegistries.ITEM.getValue(TestModContent.TEST_LOG_ITEM.id()),
            TestModContent.TEST_LOG_ITEM.get(),
            "Item registry value");
        helper.assertValueEqual(
            BuiltInRegistries.ENTITY_TYPE.getValue(TestModContent.TEST_ENTITY.id()),
            TestModContent.TEST_ENTITY.get(),
            "Entity-type registry value");
        helper.assertValueEqual(
            BuiltInRegistries.CREATIVE_MODE_TAB.getValue(TestModContent.TEST_CREATIVE_TAB.id()),
            TestModContent.TEST_CREATIVE_TAB.get(),
            "Creative tab registry value");
        helper.assertTrue(
            TestModContent.TEST_CREATIVE_TAB.get().getIconItem().is(TestModContent.TEST_LOG_ITEM.get()),
            "Creative tab icon was not resolved");
        helper.assertValueEqual(
            TestModContent.TEST_VALUE_REGISTRY.get().getValue(TestModContent.TEST_VALUE.id()),
            TestModContent.TEST_VALUE.get(),
            "Custom registry value");
        helper.assertValueEqual(
            TestModContent.TEST_WOOD_TYPE.get().name(),
            TestModContent.MOD_ID + ":test",
            "Queued wood type");
        helper.assertTrue(
            WoodType.values()
                .anyMatch(type -> type == TestModContent.TEST_WOOD_SET.woodType().get()),
            "Wood-set WoodType was not registered in vanilla's lookup");
        helper.assertTrue(
            BlockEntityTypes.SIGN.isValid(
                TestModContent.TEST_WOOD_SET.sign().get().defaultBlockState()),
            "Wood-set sign was not added to the vanilla sign block-entity type");

        var stack = new ItemStack(TestModContent.TEST_LOG_ITEM.get());
        stack.set(TestModContent.TEST_NUMBER.get(), 42);
        helper.assertValueEqual(stack.get(TestModContent.TEST_NUMBER.get()), 42, "Data component value");

        var testPosition = new BlockPos(1, 1, 1);
        helper.setBlock(testPosition, TestModContent.TEST_LOG.get());
        helper.assertBlockPresent(TestModContent.TEST_LOG.get(), testPosition);

        // Exercise the actual transformer so both the mixin and axis preservation are verified.
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        var absolutePos = helper.absolutePos(testPosition);
        helper.setBlock(testPosition, TestModContent.TEST_WOOD_SET.log().get().defaultBlockState()
            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        var hit = new BlockHitResult(Vec3.atCenterOf(absolutePos),
            Direction.UP, absolutePos, false);
        axe.get(DataComponents.BLOCK_TRANSFORMER).value().transformBlock(
            new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertBlockPresent(TestModContent.TEST_WOOD_SET.strippedLog().get(), testPosition);
        helper.assertTrue(helper.getBlockState(testPosition).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
            "Stripping changed the log axis");
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, path);
    }
}
