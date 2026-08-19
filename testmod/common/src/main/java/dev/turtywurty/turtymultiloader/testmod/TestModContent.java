package dev.turtywurty.turtymultiloader.testmod;

import com.mojang.serialization.Codec;
import dev.turtywurty.turtymultiloader.registration.CustomRegistry;
import dev.turtywurty.turtymultiloader.registration.PayloadFlow;
import dev.turtywurty.turtymultiloader.registration.PayloadPhase;
import dev.turtywurty.turtymultiloader.registration.QueuedValue;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class TestModContent {
    public static final String MOD_ID = "turtymultiloader_testmod";
    public static final RegistryService REGISTRIES = RegistryService.get();

    public static final RegistrationHandle<Block, Block> TEST_LOG = REGISTRIES.registerBlock(
        id("test_log"),
        TestModContent::createTestLog
    );
    public static final RegistrationHandle<Block, Block> STRIPPED_TEST_LOG = REGISTRIES.registerBlock(
        id("stripped_test_log"),
        TestModContent::createStrippedTestLog
    );
    public static final RegistrationHandle<Item, BlockItem> TEST_LOG_ITEM = REGISTRIES.registerItem(
        id("test_log"),
        TestModContent::createTestLogItem
    );
    public static final RegistrationHandle<DataComponentType<?>, DataComponentType<Integer>> TEST_NUMBER =
        REGISTRIES.registerDataComponentType(
            id("test_number"),
            () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build()
        );
    public static final RegistrationHandle<RecipeBookCategory, RecipeBookCategory> TEST_RECIPE_BOOK_CATEGORY =
        REGISTRIES.registerRecipeBookCategory(id("test_recipe_book_category"), RecipeBookCategory::new);

    public static final CustomRegistry<TestValue> TEST_VALUE_REGISTRY = REGISTRIES.customRegistry(id("test_value"));
    public static final RegistrationHandle<TestValue, TestValue> TEST_VALUE = TEST_VALUE_REGISTRY.register(
        id("registered_value"),
        () -> new TestValue("registered through RegistryService")
    );

    public static final QueuedValue<WoodType> TEST_WOOD_TYPE = REGISTRIES.registerWoodType(
        () -> new WoodType(MOD_ID + ":test", BlockSetType.OAK)
    );

    public static final CustomPacketPayload.Type<TestPayload> TEST_PAYLOAD_TYPE =
        new CustomPacketPayload.Type<>(id("test_payload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TestPayload> TEST_PAYLOAD_CODEC =
        ByteBufCodecs.VAR_INT.map(TestPayload::new, TestPayload::value).cast();

    static {
        REGISTRIES.populateCreativeTab(CreativeModeTabs.BUILDING_BLOCKS, output -> output.accept(TEST_LOG_ITEM.get()));
        REGISTRIES.registerStrippable(TEST_LOG, STRIPPED_TEST_LOG);
        REGISTRIES.registerFlammable(TEST_LOG, 5, 5);
        REGISTRIES.registerPayloadType(
            PayloadPhase.PLAY,
            PayloadFlow.CLIENTBOUND,
            TEST_PAYLOAD_TYPE,
            TEST_PAYLOAD_CODEC
        );
    }

    private TestModContent() {
    }

    public static void initialize() {
        if (REGISTRIES.isApplied())
            throw new IllegalStateException("Test mod declarations were loaded after registration was applied");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static Block createTestLog() {
        return new Block(BlockBehaviour.Properties.of().strength(1.0F).setId(TEST_LOG.key()));
    }

    private static Block createStrippedTestLog() {
        return new Block(BlockBehaviour.Properties.of().strength(1.0F).setId(STRIPPED_TEST_LOG.key()));
    }

    private static BlockItem createTestLogItem() {
        return new BlockItem(TEST_LOG.get(), new Item.Properties().setId(TEST_LOG_ITEM.key()));
    }

    public record TestValue(String description) {
    }

    public record TestPayload(int value) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TEST_PAYLOAD_TYPE;
        }
    }
}
