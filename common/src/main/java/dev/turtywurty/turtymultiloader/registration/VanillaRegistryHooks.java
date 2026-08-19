package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;

import java.util.HashMap;

/** Internal vanilla hooks shared by the loader implementations. */
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
}
