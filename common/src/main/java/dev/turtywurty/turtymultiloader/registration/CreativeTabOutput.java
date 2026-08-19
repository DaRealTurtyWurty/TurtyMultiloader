package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Loader-neutral subset of the vanilla creative-tab output contract.
 */
@FunctionalInterface
public interface CreativeTabOutput {
    void accept(ItemStack stack);

    default void accept(ItemLike item) {
        accept(new ItemStack(item));
    }
}
