package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A block registration and its same-named item registration.
 */
public record BlockWithItem(
    RegistrationHandle<Block, Block> block,
    RegistrationHandle<Item, Item> item
) implements Supplier<Block> {
    public BlockWithItem {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(item, "item");
    }

    @Override
    public Block get() {
        return block.get();
    }
}
