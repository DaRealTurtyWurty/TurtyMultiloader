package dev.turtywurty.turtymultiloader.testmod;

import com.mojang.serialization.Codec;
import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.gasapi.api.Gas;
import dev.turtywurty.gasapi.api.storage.GasStorage;
import dev.turtywurty.gasapi.api.storage.SingleGasStorage;
import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.slurryapi.api.storage.SingleSlurryStorage;
import dev.turtywurty.slurryapi.api.storage.SlurryStorage;
import dev.turtywurty.turtymultiloader.attachment.AttachmentType;
import dev.turtywurty.turtymultiloader.attachment.Attachments;
import dev.turtywurty.turtymultiloader.attachment.SavedStateType;
import dev.turtywurty.turtymultiloader.menu.ExtendedMenuRegistration;
import dev.turtywurty.turtymultiloader.menu.Menus;
import dev.turtywurty.turtymultiloader.menu.sync.MenuSyncChannel;
import dev.turtywurty.turtymultiloader.network.NetworkService;
import dev.turtywurty.turtymultiloader.network.PayloadRegistrationOptions;
import dev.turtywurty.turtymultiloader.registration.CustomRegistry;
import dev.turtywurty.turtymultiloader.registration.QueuedValue;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleStorage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

public final class TestModContent {
    public static final String MOD_ID = "turtymultiloader_testmod";
    public static final RegistryService REGISTRIES = RegistryService.get();
    public static final NetworkService NETWORK = NetworkService.get();
    public static final TransferService TRANSFERS = TransferService.get();
    public static final AttachmentType<Integer> TEST_COUNTER = Attachments.register(
        id("test_counter"),
        builder -> builder.defaultFactory(() -> 0)
            .persistent(Codec.INT)
            .syncToTrackers(ByteBufCodecs.VAR_INT)
            .copyOnDeath()
    );
    public static final AttachmentType<ArrayList<Integer>> TEST_MUTABLE = Attachments.register(
        id("test_mutable"),
        builder -> builder.defaultFactory(ArrayList::new).transientValue()
    );
    public static final AttachmentType<Integer> TEST_GLOBAL_COUNTER = Attachments.register(
        id("test_global_counter"),
        builder -> builder.defaultFactory(() -> 0).persistent(Codec.INT)
    );
    public static final AttachmentType<Integer> TEST_OWNER_COUNTER = Attachments.register(
        id("test_owner_counter"),
        builder -> builder.defaultFactory(() -> 0).syncToOwner(ByteBufCodecs.VAR_INT)
    );
    public static final SavedStateType<Integer> TEST_WORLD_STATE = SavedStateType.world(
        id("test_world_state"), Codec.INT, () -> 0
    );
    public static final SavedStateType<Integer> TEST_SERVER_STATE = SavedStateType.server(
        id("test_server_state"), Codec.INT, () -> 0
    );

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
    public static final ExtendedMenuRegistration<TestMenu, BlockPos> TEST_MENU = Menus.registerExtended(
        id("test_menu"),
        TestMenu::new,
        BlockPos.STREAM_CODEC
    );
    public static final MenuSyncChannel<TestMenu, Integer> TEST_MENU_SYNC = MenuSyncChannel.register(
        id("test_menu_sync"),
        TestMenu.class,
        ByteBufCodecs.VAR_INT,
        PayloadRegistrationOptions.optional("1")
    );

    public static final CustomRegistry<TestValue> TEST_VALUE_REGISTRY = REGISTRIES.customRegistry(id("test_value"));
    public static final RegistrationHandle<TestValue, TestValue> TEST_VALUE = TEST_VALUE_REGISTRY.register(
        id("registered_value"),
        () -> new TestValue("registered through RegistryService")
    );
    public static final RegistrationHandle<Gas, Gas> TEST_GAS = GasApi.register(id("test_gas"));
    public static final RegistrationHandle<Slurry, Slurry> TEST_SLURRY = SlurryApi.register(id("test_slurry"));

    public static final QueuedValue<WoodType> TEST_WOOD_TYPE = REGISTRIES.registerWoodType(
        () -> new WoodType(MOD_ID + ":test", BlockSetType.OAK)
    );

    public static final CustomPacketPayload.Type<TestPayload> TEST_PAYLOAD_TYPE =
        new CustomPacketPayload.Type<>(id("test_payload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TestPayload> TEST_PAYLOAD_CODEC =
        ByteBufCodecs.VAR_INT.map(TestPayload::new, TestPayload::value).cast();
    public static final SimpleSingleSlotStorage<ResourceVariant<Item>> TEST_ITEM_STORAGE =
        new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);
    public static final SimpleStorage<ResourceVariant<Item>> TEST_MULTI_ITEM_STORAGE =
        new SimpleStorage<>(ResourceTypes.ITEM, 2, 64);
    public static final SimpleSingleSlotStorage<ResourceVariant<Fluid>> TEST_FLUID_STORAGE =
        new SimpleSingleSlotStorage<>(ResourceTypes.FLUID, 162_000);
    public static final SimpleSingleSlotStorage<ResourceVariant<UnitResource>> TEST_ENERGY_STORAGE =
        new SimpleSingleSlotStorage<>(ResourceTypes.ENERGY, 10_000);
    public static final SimpleSingleSlotStorage<ResourceVariant<UnitResource>> LATE_ENERGY_STORAGE =
        new SimpleSingleSlotStorage<>(ResourceTypes.ENERGY, 10_000);
    public static final SingleGasStorage TEST_GAS_STORAGE = new SingleGasStorage(162_000);
    public static final SingleSlurryStorage TEST_SLURRY_STORAGE = new SingleSlurryStorage(162_000);

    static {
        REGISTRIES.populateCreativeTab(CreativeModeTabs.BUILDING_BLOCKS, output -> output.accept(TEST_LOG_ITEM.get()));
        REGISTRIES.registerStrippable(TEST_LOG, STRIPPED_TEST_LOG);
        REGISTRIES.registerFlammable(TEST_LOG, 5, 5);
        NETWORK.registerPlayClientbound(
            TEST_PAYLOAD_TYPE,
            TEST_PAYLOAD_CODEC,
            PayloadRegistrationOptions.required("1")
        );
        NETWORK.addLoginSync(player -> List.of(new TestPayload(42)));
        TRANSFERS.registerBlockProvider(StorageKeys.ITEM, (level, pos, state, blockEntity, side) -> TEST_ITEM_STORAGE,
            TEST_LOG);
        TRANSFERS.registerItemProvider(StorageKeys.ITEM, (stack, context) -> TEST_ITEM_STORAGE, TEST_LOG_ITEM);
        TRANSFERS.registerBlockProvider(
            StorageKeys.ITEM,
            (level, pos, state, blockEntity, side) -> TEST_MULTI_ITEM_STORAGE,
            STRIPPED_TEST_LOG
        );
        TRANSFERS.registerBlockProvider(
            StorageKeys.FLUID,
            (level, pos, state, blockEntity, side) -> TEST_FLUID_STORAGE,
            TEST_LOG
        );
        TRANSFERS.registerBlockProvider(
            StorageKeys.ENERGY,
            (level, pos, state, blockEntity, side) -> TEST_ENERGY_STORAGE,
            TEST_LOG
        );
        TRANSFERS.registerItemProvider(
            StorageKeys.ENERGY,
            (stack, context) -> TEST_ENERGY_STORAGE,
            TEST_LOG_ITEM
        );
        GasStorage.registerBlockProvider(
            (level, pos, state, blockEntity, side) -> TEST_GAS_STORAGE,
            TEST_LOG
        );
        SlurryStorage.registerBlockProvider(
            (level, pos, state, blockEntity, side) -> TEST_SLURRY_STORAGE,
            TEST_LOG
        );
    }

    private TestModContent() {
    }

    public static void initialize() {
    }

    /**
     * Declares a provider after the first TransferService.apply() call to verify multi-consumer lifecycle support.
     */
    public static void registerLateTransfers() {
        TRANSFERS.registerBlockProvider(
            StorageKeys.ENERGY,
            (level, pos, state, blockEntity, side) -> LATE_ENERGY_STORAGE,
            STRIPPED_TEST_LOG
        );
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
