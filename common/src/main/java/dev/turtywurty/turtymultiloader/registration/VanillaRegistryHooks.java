package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal vanilla hooks shared by the loader implementations.
 */
public final class VanillaRegistryHooks {
    private VanillaRegistryHooks() {
    }

    public static void registerStrippable(Block block, Block stripped) {
        var strippables = new HashMap<>(AxeItem.STRIPPABLES);
        strippables.put(block, stripped);
        AxeItem.STRIPPABLES = strippables;
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
