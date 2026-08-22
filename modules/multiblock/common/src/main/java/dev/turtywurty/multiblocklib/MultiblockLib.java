package dev.turtywurty.multiblocklib;

import dev.turtywurty.multiblocklib.block.MultiblockControllerBlock;
import dev.turtywurty.multiblocklib.block.MultiblockPartBlock;
import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.data.MultiblockDefinitionManager;
import dev.turtywurty.multiblocklib.pattern.MultiblockPatternRegistry;
import dev.turtywurty.multiblocklib.pattern.factory.*;
import dev.turtywurty.multiblocklib.platform.MultiblockPlatformService;
import dev.turtywurty.multiblocklib.port.MultiblockPortLookups;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MultiblockLib {
    public static final String MOD_ID = "multiblocklib";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final MultiblockDefinitionManager DEFINITION_MANAGER = new MultiblockDefinitionManager();

    public static final ResourceKey<Block> MULTIBLOCK_PART_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(MOD_ID + ":multiblock_part"));
    public static MultiblockPartBlock MULTIBLOCK_PART;
    public static final ResourceKey<Block> MULTIBLOCK_CONTROLLER_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(MOD_ID + ":multiblock_controller"));
    public static MultiblockControllerBlock MULTIBLOCK_CONTROLLER;
    private static final Set<Block> CONTROLLER_BLOCKS = new HashSet<>();
    public static RegistrationHandle<Block, MultiblockPartBlock> MULTIBLOCK_PART_HANDLE;
    public static RegistrationHandle<Block, MultiblockControllerBlock> MULTIBLOCK_CONTROLLER_HANDLE;
    public static RegistrationHandle<BlockEntityType<?>, BlockEntityType<MultiblockControllerBlockEntity>>
        MULTIBLOCK_CONTROLLER_ENTITY_HANDLE;
    private static boolean initialized;

    private static void registerContent() {
        RegistryService registries = RegistryService.get();
        MULTIBLOCK_PART_HANDLE = registries.registerBlock(id("multiblock_part"), () -> {
            MULTIBLOCK_PART = new MultiblockPartBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1.0F).noOcclusion()
                    .setId(MULTIBLOCK_PART_KEY)
            );
            return MULTIBLOCK_PART;
        });
        MULTIBLOCK_CONTROLLER_HANDLE = registries.registerBlock(id("multiblock_controller"), () -> {
            MULTIBLOCK_CONTROLLER = new MultiblockControllerBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1.0F).noOcclusion()
                    .setId(MULTIBLOCK_CONTROLLER_KEY)
            );
            CONTROLLER_BLOCKS.add(MULTIBLOCK_CONTROLLER);
            return MULTIBLOCK_CONTROLLER;
        });
        MULTIBLOCK_CONTROLLER_ENTITY_HANDLE = registries.registerBlockEntityType(
            id("multiblock_controller"),
            MultiblockControllerBlockEntity::new,
            builder -> builder.validBlock(MULTIBLOCK_CONTROLLER_HANDLE)
        );
        registries.apply();
    }

    private static void registerPatterns() {
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":grid"), new GridPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":star"), new StarShapePatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":frame"), new FramePatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":cross"), new CrossPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":ring"), new RingPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":cylinder"), new CylinderPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":pyramid"), new PyramidPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":plus"), new PlusPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":checkerboard"), new CheckerboardPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":layered"), new LayeredPatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":sphere"), new SpherePatternFactory());
        MultiblockPatternRegistry.register(Identifier.parse(MOD_ID + ":hollow_box"), new HollowBoxPatternFactory());
    }

    private static void registerReloaders() {
        MultiblockPlatformService.get().registerServerReloadListener(id("multiblocks"), DEFINITION_MANAGER);
    }

    private static void registerEvents() {
        MultiblockPlatformService.get().registerUseBlock((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            return MultiblockPlacement.tryForm(serverLevel, hitResult.getBlockPos(), stack, serverPlayer);
        });

        MultiblockPortLookups.register();
    }

    public static void registerControllerBlock(final Block block) {
        registerControllerBlock(block, true);
    }

    public static void registerControllerBlock(final Block block, final boolean registerLookups) {
        CONTROLLER_BLOCKS.add(block);
        if (registerLookups) {
            MultiblockPortLookups.registerControllerBlock(block);
        }
    }

    public static boolean isControllerBlock(final Block block) {
        return CONTROLLER_BLOCKS.contains(block);
    }

    public static Set<Block> controllerBlocks() {
        return Collections.unmodifiableSet(CONTROLLER_BLOCKS);
    }

    private static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerContent();
        registerPatterns();
        registerReloaders();
        registerEvents();
    }
}
