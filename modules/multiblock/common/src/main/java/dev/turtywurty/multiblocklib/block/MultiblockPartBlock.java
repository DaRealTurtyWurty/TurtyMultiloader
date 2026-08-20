package dev.turtywurty.multiblocklib.block;

import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.world.MultiblockWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public class MultiblockPartBlock extends Block {
    public MultiblockPartBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull RenderShape getRenderShape(final @NonNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(final @NonNull BlockState state, final @NonNull Level level, final @NonNull BlockPos pos, final @NonNull Player player, final @NonNull BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos controllerPos = MultiblockWorldData.get(serverLevel).getControllerFor(pos);
        if (controllerPos == null) {
            return InteractionResult.PASS;
        }

        if (!(serverLevel.getBlockEntity(controllerPos) instanceof MenuProvider menuProvider)) {
            return InteractionResult.PASS;
        }

        player.openMenu(menuProvider);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull BlockState playerWillDestroy(final @NonNull Level level, final @NonNull BlockPos pos, final @NonNull BlockState state, final @NonNull Player player) {
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

    private static void teardown(final ServerLevel level, final BlockPos partPos) {
        MultiblockWorldData data = MultiblockWorldData.get(level);
        BlockPos controllerPos = data.getControllerFor(partPos);
        if (controllerPos == null) {
            return;
        }

        if (level.getBlockEntity(controllerPos) instanceof MultiblockControllerBlockEntity controller) {
            if (!controller.isBreaking()) {
                controller.breakMultiblock();
            }
            return;
        }

        data.restorePartsForController(level, controllerPos);
    }
}
