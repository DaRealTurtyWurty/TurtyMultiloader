package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Common-code access to the reusable block predicates declared privately by {@link Blocks}.
 */
public final class VanillaBlockPredicates {
    private VanillaBlockPredicates() {
    }

    public static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return Blocks.never(state, level, pos);
    }

    public static boolean always(BlockState state, BlockGetter level, BlockPos pos) {
        return Blocks.always(state, level, pos);
    }

    public static boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> entityType) {
        return Blocks.never(state, level, pos, entityType);
    }

    public static boolean always(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> entityType) {
        return Blocks.always(state, level, pos, entityType);
    }

    public static boolean ocelotOrParrot(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        EntityType<?> entityType
    ) {
        return Blocks.ocelotOrParrot(state, level, pos, entityType);
    }
}
