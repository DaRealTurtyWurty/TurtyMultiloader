package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Registers the complete vanilla-style wood family used by Industria, without loader-specific calls.
 */
public final class WoodSetBuilder {
    private final RegistryService registries;
    private final Identifier id;
    private final TreeGrower treeGrower;
    private final Map<BlockType, BlockFactory> blockFactories = new EnumMap<>(BlockType.class);
    private final Map<BlockType, UnaryOperator<BlockBehaviour.Properties>> propertyOverrides =
        new EnumMap<>(BlockType.class);
    private BiFunction<WoodSetContext, Item.Properties, Item> signItemFactory;
    private BiFunction<WoodSetContext, Item.Properties, Item> hangingSignItemFactory;
    private BiFunction<WoodSetContext, Item.Properties, Item> boatItemFactory;
    private BiFunction<WoodSetContext, Item.Properties, Item> chestBoatItemFactory;
    private UnaryOperator<EntityType.Builder<Boat>> boatType = UnaryOperator.identity();
    private UnaryOperator<EntityType.Builder<ChestBoat>> chestBoatType = UnaryOperator.identity();
    private Function<String, BlockSetType> blockSetTypeFactory = BlockSetType::new;
    private BiFunction<String, BlockSetType, WoodType> woodTypeFactory = WoodType::new;
    private boolean built;

    public WoodSetBuilder(RegistryService registries, Identifier id, TreeGrower treeGrower) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.id = Objects.requireNonNull(id, "id");
        this.treeGrower = Objects.requireNonNull(treeGrower, "treeGrower");
    }

    public WoodSetBuilder block(BlockType type, BlockFactory factory) {
        blockFactories.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(factory, "factory"));
        return this;
    }

    public WoodSetBuilder properties(BlockType type, UnaryOperator<BlockBehaviour.Properties> override) {
        propertyOverrides.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(override, "override"));
        return this;
    }

    public WoodSetBuilder planks(BlockFactory factory) {
        return block(BlockType.PLANKS, factory);
    }

    public WoodSetBuilder log(BlockFactory factory) {
        return block(BlockType.LOG, factory);
    }

    public WoodSetBuilder strippedLog(BlockFactory factory) {
        return block(BlockType.STRIPPED_LOG, factory);
    }

    public WoodSetBuilder wood(BlockFactory factory) {
        return block(BlockType.WOOD, factory);
    }

    public WoodSetBuilder strippedWood(BlockFactory factory) {
        return block(BlockType.STRIPPED_WOOD, factory);
    }

    public WoodSetBuilder leaves(BlockFactory factory) {
        return block(BlockType.LEAVES, factory);
    }

    public WoodSetBuilder sapling(BlockFactory factory) {
        return block(BlockType.SAPLING, factory);
    }

    public WoodSetBuilder stairs(BlockFactory factory) {
        return block(BlockType.STAIRS, factory);
    }

    public WoodSetBuilder slab(BlockFactory factory) {
        return block(BlockType.SLAB, factory);
    }

    public WoodSetBuilder fence(BlockFactory factory) {
        return block(BlockType.FENCE, factory);
    }

    public WoodSetBuilder fenceGate(BlockFactory factory) {
        return block(BlockType.FENCE_GATE, factory);
    }

    public WoodSetBuilder door(BlockFactory factory) {
        return block(BlockType.DOOR, factory);
    }

    public WoodSetBuilder trapdoor(BlockFactory factory) {
        return block(BlockType.TRAPDOOR, factory);
    }

    public WoodSetBuilder pressurePlate(BlockFactory factory) {
        return block(BlockType.PRESSURE_PLATE, factory);
    }

    public WoodSetBuilder button(BlockFactory factory) {
        return block(BlockType.BUTTON, factory);
    }

    public WoodSetBuilder sign(BlockFactory factory) {
        return block(BlockType.SIGN, factory);
    }

    public WoodSetBuilder wallSign(BlockFactory factory) {
        return block(BlockType.WALL_SIGN, factory);
    }

    public WoodSetBuilder hangingSign(BlockFactory factory) {
        return block(BlockType.HANGING_SIGN, factory);
    }

    public WoodSetBuilder wallHangingSign(BlockFactory factory) {
        return block(BlockType.WALL_HANGING_SIGN, factory);
    }

    public WoodSetBuilder signItem(BiFunction<WoodSetContext, Item.Properties, Item> factory) {
        signItemFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSetBuilder hangingSignItem(BiFunction<WoodSetContext, Item.Properties, Item> factory) {
        hangingSignItemFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSetBuilder boatItem(BiFunction<WoodSetContext, Item.Properties, Item> factory) {
        boatItemFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSetBuilder chestBoatItem(BiFunction<WoodSetContext, Item.Properties, Item> factory) {
        chestBoatItemFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSetBuilder boatType(UnaryOperator<EntityType.Builder<Boat>> configuration) {
        boatType = Objects.requireNonNull(configuration, "configuration");
        return this;
    }

    public WoodSetBuilder chestBoatType(UnaryOperator<EntityType.Builder<ChestBoat>> configuration) {
        chestBoatType = Objects.requireNonNull(configuration, "configuration");
        return this;
    }

    public WoodSetBuilder blockSetType(Function<String, BlockSetType> factory) {
        blockSetTypeFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSetBuilder woodType(BiFunction<String, BlockSetType, WoodType> factory) {
        woodTypeFactory = Objects.requireNonNull(factory, "factory");
        return this;
    }

    public WoodSet build() {
        if (built)
            throw new IllegalStateException("This wood-set builder has already been built");
        built = true;

        String base = id.getPath();
        QueuedValue<BlockSetType> blockSetType = registries.registerBlockSetType(
            () -> blockSetTypeFactory.apply(id.toString())
        );
        QueuedValue<WoodType> woodType = registries.registerWoodType(
            () -> woodTypeFactory.apply(id.toString(), blockSetType.get())
        );
        Map<BlockType, RegistrationHandle<Block, Block>> blocks = new EnumMap<>(BlockType.class);
        WoodSetContext context = new WoodSetContext(id, treeGrower, blockSetType, woodType, blocks);

        BlockWithItem planks = blockWithItem(context, BlockType.PLANKS, base + "_planks", Blocks.OAK_PLANKS,
            (ctx, properties) -> new Block(properties));
        BlockWithItem log = blockWithItem(context, BlockType.LOG, base + "_log", Blocks.OAK_LOG,
            (ctx, properties) -> new RotatedPillarBlock(properties));
        BlockWithItem strippedLog = blockWithItem(context, BlockType.STRIPPED_LOG, base + "_stripped_log",
            Blocks.STRIPPED_OAK_LOG, (ctx, properties) -> new RotatedPillarBlock(properties));
        BlockWithItem strippedWood = blockWithItem(context, BlockType.STRIPPED_WOOD, base + "_stripped_wood",
            Blocks.STRIPPED_OAK_WOOD, (ctx, properties) -> new RotatedPillarBlock(properties));
        BlockWithItem wood = blockWithItem(context, BlockType.WOOD, base + "_wood", Blocks.OAK_WOOD,
            (ctx, properties) -> new RotatedPillarBlock(properties));
        BlockWithItem leaves = blockWithItem(context, BlockType.LEAVES, base + "_leaves", Blocks.OAK_LEAVES,
            (ctx, properties) -> new TintedParticleLeavesBlock(0.01F, properties));
        BlockWithItem sapling = blockWithItem(context, BlockType.SAPLING, base + "_sapling", Blocks.OAK_SAPLING,
            (ctx, properties) -> new PublicSaplingBlock(ctx.treeGrower(), properties));
        BlockWithItem stairs = blockWithItem(context, BlockType.STAIRS, base + "_stairs", Blocks.OAK_STAIRS,
            (ctx, properties) -> new PublicStairBlock(ctx.requiredBlock(BlockType.PLANKS).defaultBlockState(), properties));
        BlockWithItem slab = blockWithItem(context, BlockType.SLAB, base + "_slab", Blocks.OAK_SLAB,
            (ctx, properties) -> new SlabBlock(properties));
        BlockWithItem fence = blockWithItem(context, BlockType.FENCE, base + "_fence", Blocks.OAK_FENCE,
            (ctx, properties) -> new FenceBlock(properties));
        BlockWithItem fenceGate = blockWithItem(context, BlockType.FENCE_GATE, base + "_fence_gate", Blocks.OAK_FENCE_GATE,
            (ctx, properties) -> new FenceGateBlock(ctx.woodType().get(), properties));
        BlockWithItem door = blockWithItem(context, BlockType.DOOR, base + "_door", Blocks.OAK_DOOR,
            (ctx, properties) -> new PublicDoorBlock(ctx.blockSetType().get(), properties));
        BlockWithItem trapdoor = blockWithItem(context, BlockType.TRAPDOOR, base + "_trapdoor", Blocks.OAK_TRAPDOOR,
            (ctx, properties) -> new PublicTrapDoorBlock(ctx.blockSetType().get(), properties));
        BlockWithItem pressurePlate = blockWithItem(context, BlockType.PRESSURE_PLATE, base + "_pressure_plate",
            Blocks.OAK_PRESSURE_PLATE, (ctx, properties) -> new PublicPressurePlateBlock(ctx.blockSetType().get(), properties));
        BlockWithItem button = blockWithItem(context, BlockType.BUTTON, base + "_button", Blocks.OAK_BUTTON,
            (ctx, properties) -> new PublicButtonBlock(ctx.blockSetType().get(), 30, properties));

        RegistrationHandle<Block, Block> sign = block(context, BlockType.SIGN, base + "_sign", Blocks.OAK_SIGN,
            (ctx, properties) -> new StandingSignBlock(ctx.woodType().get(), properties));
        RegistrationHandle<Block, Block> wallSign = block(context, BlockType.WALL_SIGN, base + "_wall_sign",
            Blocks.OAK_WALL_SIGN, (ctx, properties) -> new WallSignBlock(ctx.woodType().get(), properties));
        RegistrationHandle<Block, Block> hangingSign = block(context, BlockType.HANGING_SIGN, base + "_hanging_sign",
            Blocks.OAK_HANGING_SIGN, (ctx, properties) -> new CeilingHangingSignBlock(ctx.woodType().get(), properties));
        RegistrationHandle<Block, Block> wallHangingSign = block(context, BlockType.WALL_HANGING_SIGN,
            base + "_wall_hanging_sign", Blocks.OAK_WALL_HANGING_SIGN,
            (ctx, properties) -> new WallHangingSignBlock(ctx.woodType().get(), properties));

        RegistrationHandle<Item, Item> signItem = item(base + "_sign", properties -> signItemFactory == null
            ? new SignItem(sign.get(), wallSign.get(), properties.stacksTo(16))
            : signItemFactory.apply(context, properties));
        RegistrationHandle<Item, Item> hangingSignItem = item(base + "_hanging_sign", properties -> hangingSignItemFactory == null
            ? new HangingSignItem(hangingSign.get(), wallHangingSign.get(), properties.stacksTo(16))
            : hangingSignItemFactory.apply(context, properties));

        AtomicReference<RegistrationHandle<Item, Item>> boatItemRef = new AtomicReference<>();
        AtomicReference<RegistrationHandle<Item, Item>> chestBoatItemRef = new AtomicReference<>();
        Identifier boatId = child(base + "_boat");
        Identifier chestBoatId = child(base + "_chest_boat");
        RegistrationHandle<EntityType<?>, EntityType<Boat>> boatEntity = registries.registerEntityType(boatId, () ->
            boatType.apply(EntityType.Builder.<Boat>of(
                    (type, level) -> new Boat(type, level, () -> boatItemRef.get().get()), MobCategory.MISC
                ).noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10))
                .build(ResourceKey.create(Registries.ENTITY_TYPE, boatId))
        );
        RegistrationHandle<EntityType<?>, EntityType<ChestBoat>> chestBoatEntity = registries.registerEntityType(chestBoatId, () ->
            chestBoatType.apply(EntityType.Builder.<ChestBoat>of(
                    (type, level) -> new ChestBoat(type, level, () -> chestBoatItemRef.get().get()), MobCategory.MISC
                ).noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10))
                .build(ResourceKey.create(Registries.ENTITY_TYPE, chestBoatId))
        );
        RegistrationHandle<Item, Item> boatItem = item(base + "_boat", properties -> boatItemFactory == null
            ? new BoatItem(boatEntity.get(), properties.stacksTo(1)) : boatItemFactory.apply(context, properties));
        boatItemRef.set(boatItem);
        RegistrationHandle<Item, Item> chestBoatItem = item(base + "_chest_boat", properties -> chestBoatItemFactory == null
            ? new BoatItem(chestBoatEntity.get(), properties.stacksTo(1)) : chestBoatItemFactory.apply(context, properties));
        chestBoatItemRef.set(chestBoatItem);

        registerLifecycle(planks, log, strippedLog, strippedWood, wood, leaves, sapling, stairs, slab, fence,
            fenceGate, door, trapdoor, pressurePlate, button, sign, wallSign, hangingSign, wallHangingSign,
            boatEntity, chestBoatEntity, boatItem, chestBoatItem);

        Identifier logsId = child(base + "_logs");
        return new WoodSet(id, blockSetType, woodType, planks, log, strippedLog, strippedWood, wood, leaves,
            sapling, stairs, slab, fence, fenceGate, door, trapdoor, pressurePlate, button, sign, wallSign,
            hangingSign, wallHangingSign, signItem, hangingSignItem, boatEntity, chestBoatEntity, boatItem,
            chestBoatItem, TagKey.create(Registries.BLOCK, logsId), TagKey.create(Registries.ITEM, logsId));
    }

    private BlockWithItem blockWithItem(
        WoodSetContext context, BlockType type, String path, Block copy, BlockFactory fallback
    ) {
        RegistrationHandle<Block, Block> block = block(context, type, path, copy, fallback);
        RegistrationHandle<Item, Item> item = item(path, properties -> new BlockItem(block.get(), properties));
        return new BlockWithItem(block, item);
    }

    private RegistrationHandle<Block, Block> block(
        WoodSetContext context, BlockType type, String path, Block copy, BlockFactory fallback
    ) {
        Identifier blockId = child(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, blockId);
        BlockFactory factory = blockFactories.getOrDefault(type, fallback);
        RegistrationHandle<Block, Block> result = registries.registerBlock(blockId, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofLegacyCopy(copy).setId(key);
            UnaryOperator<BlockBehaviour.Properties> override = propertyOverrides.get(type);
            return factory.create(context, override == null ? properties : override.apply(properties));
        });
        context.blocks().put(type, result);
        return result;
    }

    private RegistrationHandle<Item, Item> item(String path, java.util.function.Function<Item.Properties, Item> factory) {
        Identifier itemId = child(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, itemId);
        return registries.registerItem(itemId, () -> factory.apply(new Item.Properties().setId(key)));
    }

    private void registerLifecycle(
        BlockWithItem planks, BlockWithItem log, BlockWithItem strippedLog, BlockWithItem strippedWood,
        BlockWithItem wood, BlockWithItem leaves, BlockWithItem sapling, BlockWithItem stairs, BlockWithItem slab,
        BlockWithItem fence, BlockWithItem fenceGate, BlockWithItem door, BlockWithItem trapdoor,
        BlockWithItem pressurePlate, BlockWithItem button, RegistrationHandle<Block, Block> sign,
        RegistrationHandle<Block, Block> wallSign, RegistrationHandle<Block, Block> hangingSign,
        RegistrationHandle<Block, Block> wallHangingSign,
        RegistrationHandle<EntityType<?>, EntityType<Boat>> boatEntity,
        RegistrationHandle<EntityType<?>, EntityType<ChestBoat>> chestBoatEntity,
        RegistrationHandle<Item, Item> boatItem, RegistrationHandle<Item, Item> chestBoatItem
    ) {
        registries.registerStrippable(log, strippedLog);
        registries.registerStrippable(wood, strippedWood);
        flammable(planks, 5, 20);
        flammable(log, 5, 5);
        flammable(strippedLog, 5, 5);
        flammable(strippedWood, 5, 5);
        flammable(wood, 5, 5);
        flammable(leaves, 30, 60);
        flammable(sapling, 60, 20);
        for (BlockWithItem block : List.of(stairs, slab, fence, fenceGate, door, trapdoor, pressurePlate, button))
            flammable(block, 5, 20);
        for (RegistrationHandle<Block, Block> block : List.of(sign, wallSign, hangingSign, wallHangingSign))
            registries.registerFlammable(block, 5, 20);
        registries.addBlockEntityValidBlocks(() -> BlockEntityType.SIGN,
            List.of(sign, wallSign));
        registries.addBlockEntityValidBlocks(() -> BlockEntityType.HANGING_SIGN,
            List.of(hangingSign, wallHangingSign));
        registries.registerDispenserBehavior(boatItem, () -> new BoatDispenseItemBehavior(boatEntity.get()));
        registries.registerDispenserBehavior(chestBoatItem, () -> new BoatDispenseItemBehavior(chestBoatEntity.get()));
    }

    private void flammable(BlockWithItem block, int igniteOdds, int burnOdds) {
        registries.registerFlammable(block, igniteOdds, burnOdds);
    }

    private Identifier child(String path) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), path);
    }

    public enum BlockType {
        PLANKS, LOG, STRIPPED_LOG, STRIPPED_WOOD, WOOD, LEAVES, SAPLING, STAIRS, SLAB, FENCE,
        FENCE_GATE, DOOR, TRAPDOOR, PRESSURE_PLATE, BUTTON, SIGN, WALL_SIGN, HANGING_SIGN,
        WALL_HANGING_SIGN
    }

    @FunctionalInterface
    public interface BlockFactory {
        Block create(WoodSetContext context, BlockBehaviour.Properties properties);
    }

    public record WoodSetContext(
        Identifier id,
        TreeGrower treeGrower,
        QueuedValue<BlockSetType> blockSetType,
        QueuedValue<WoodType> woodType,
        Map<BlockType, RegistrationHandle<Block, Block>> blocks
    ) {
        public Block requiredBlock(BlockType type) {
            RegistrationHandle<Block, Block> block = blocks.get(type);
            if (block == null)
                throw new IllegalStateException("Wood-set block " + type + " has not been declared yet");
            return block.get();
        }
    }

    private static final class PublicSaplingBlock extends SaplingBlock {
        private PublicSaplingBlock(TreeGrower grower, BlockBehaviour.Properties properties) {
            super(grower, properties);
        }
    }

    private static final class PublicStairBlock extends StairBlock {
        private PublicStairBlock(net.minecraft.world.level.block.state.BlockState state, BlockBehaviour.Properties properties) {
            super(state, properties);
        }
    }

    private static final class PublicDoorBlock extends DoorBlock {
        private PublicDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    private static final class PublicTrapDoorBlock extends TrapDoorBlock {
        private PublicTrapDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    private static final class PublicPressurePlateBlock extends PressurePlateBlock {
        private PublicPressurePlateBlock(BlockSetType type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }
    }

    private static final class PublicButtonBlock extends ButtonBlock {
        private PublicButtonBlock(BlockSetType type, int ticks, BlockBehaviour.Properties properties) {
            super(type, ticks, properties);
        }
    }
}
