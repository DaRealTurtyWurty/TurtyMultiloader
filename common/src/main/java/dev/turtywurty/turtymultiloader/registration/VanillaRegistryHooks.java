package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.CopyPropertiesProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal vanilla hooks shared by the loader implementations.
 */
public final class VanillaRegistryHooks {
    private static final Map<Block, Block> STRIPPABLES = new LinkedHashMap<>();

    private VanillaRegistryHooks() {
    }

    public static void registerStrippable(Block block, Block stripped) {
        STRIPPABLES.put(block, stripped);
    }

    /** Adds registered mod wood to the vanilla axe transformer while retaining its normal effects. */
    public static List<BlockTransformer.BlockTransformData> strippingTransforms(
        List<BlockTransformer.BlockTransformData> transforms
    ) {
        if (STRIPPABLES.isEmpty() || transforms.stream().noneMatch(t -> t.sound().value() == SoundEvents.AXE_STRIP.value()))
            return transforms;
        var result = new ArrayList<>(transforms);
        STRIPPABLES.forEach((block, stripped) -> result.add(BlockTransformer.BlockTransformData.builder(
            RuleBasedStateProvider.ifTrueThenProvide(
                BlockPredicate.matchesBlocks(block), new CopyPropertiesProvider(stripped))
        ).sound(SoundEvents.AXE_STRIP).build()));
        return result;
    }

    public static void registerFlammable(Block block, int igniteOdds, int burnOdds) {
        ((FireBlock) Blocks.FIRE).setFlammable(block, igniteOdds, burnOdds);
    }

    public static BlockSetType registerBlockSetType(BlockSetType type) {
        return BlockSetType.register(type);
    }

    public static WoodType registerWoodType(WoodType type) {
        return WoodType.register(type);
    }

    public static void addBlockEntityValidBlocks(BlockEntityType<?> type, Iterable<? extends Block> blocks) {
        HashSet<Block> validBlocks = new HashSet<>(type.validBlocks);
        blocks.forEach(validBlocks::add);
        type.validBlocks = Set.copyOf(validBlocks);
    }
}
