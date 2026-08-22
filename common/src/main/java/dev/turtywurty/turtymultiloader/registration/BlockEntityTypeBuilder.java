package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Configures a block entity type while allowing its valid blocks to remain deferred until registry application.
 *
 * @param <T> the block entity created by the type
 */
public final class BlockEntityTypeBuilder<T extends BlockEntity> {
    private final BiFunction<BlockPos, BlockState, T> factory;
    private final Set<Supplier<? extends Block>> validBlocks = new LinkedHashSet<>();

    public BlockEntityTypeBuilder(BiFunction<BlockPos, BlockState, T> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Returns the configured factory. Intended for registry backends.
     */
    public BiFunction<BlockPos, BlockState, T> factory() {
        return factory;
    }

    /**
     * Adds a block which has already been registered or is otherwise immediately available.
     */
    public BlockEntityTypeBuilder<T> validBlock(Block block) {
        Objects.requireNonNull(block, "block");
        return validBlock(() -> block);
    }

    /**
     * Adds a deferred block, such as a {@link RegistrationHandle} returned by
     * {@link RegistryService#registerBlock}.
     */
    public BlockEntityTypeBuilder<T> validBlock(Supplier<? extends Block> block) {
        validBlocks.add(Objects.requireNonNull(block, "block"));
        return this;
    }

    public BlockEntityTypeBuilder<T> validBlocks(Block... blocks) {
        Objects.requireNonNull(blocks, "blocks");
        for (Block block : blocks)
            validBlock(block);

        return this;
    }

    @SafeVarargs
    public final BlockEntityTypeBuilder<T> validBlocks(Supplier<? extends Block>... blocks) {
        Objects.requireNonNull(blocks, "blocks");
        for (Supplier<? extends Block> block : blocks)
            validBlock(block);

        return this;
    }

    /**
     * Resolves the configured blocks. Intended for registry backends.
     */
    public Block[] resolveValidBlocks() {
        Set<Block> resolvedBlocks = new LinkedHashSet<>(validBlocks.size());
        for (Supplier<? extends Block> block : validBlocks)
            resolvedBlocks.add(Objects.requireNonNull(block.get(), "valid block supplier result"));

        return resolvedBlocks.toArray(Block[]::new);
    }
}
