package dev.turtywurty.multiblocklib.block;

import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.world.MultiblockWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

public class MultiblockControllerBlock extends Block implements EntityBlock {
    private final BiFunction<BlockPos, BlockState, ? extends MultiblockControllerBlockEntity> factory;

    public MultiblockControllerBlock(final Properties properties) {
        this(properties, MultiblockControllerBlockEntity::new);
    }

    public MultiblockControllerBlock(
        final Properties properties,
        final BiFunction<BlockPos, BlockState, ? extends MultiblockControllerBlockEntity> factory
    ) {
        super(properties);
        this.factory = factory;
    }

    @Override
    protected @NonNull RenderShape getRenderShape(final @NonNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NonNull BlockPos pos, final @NonNull BlockState state) {
        return this.factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final @NonNull BlockState state, final @NonNull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (be instanceof MultiblockControllerBlockEntity controller) {
                MultiblockControllerBlockEntity.tick(lvl, pos, blockState, controller);
            }
        };
    }

    @Override
    public @NonNull BlockState playerWillDestroy(final Level level, final @NonNull BlockPos pos, final @NonNull BlockState state, final @NonNull Player player) {
        if (level instanceof ServerLevel serverLevel) {
            teardown(serverLevel, pos);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final @NonNull BlockState state, final @NonNull ServerLevel level, final @NonNull BlockPos pos, final boolean movedByPiston) {
        teardown(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    private static void teardown(final ServerLevel level, final BlockPos controllerPos) {
        if (level.getBlockEntity(controllerPos) instanceof MultiblockControllerBlockEntity controller) {
            if (!controller.isBreaking()) {
                controller.breakMultiblock();
            }
            return;
        }

        MultiblockWorldData.get(level).restorePartsForController(level, controllerPos);
    }
}
