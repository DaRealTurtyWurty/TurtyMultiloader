package dev.turtywurty.multiblocklib.neoforge;

import dev.turtywurty.multiblocklib.platform.MultiblockPlatformService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;
import java.util.function.BiFunction;

public final class NeoForgeMultiblockPlatformService implements MultiblockPlatformService {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(
        BiFunction<BlockPos, BlockState, T> factory,
        Block... blocks
    ) {
        return new BlockEntityType<>(factory::apply, Set.of(blocks));
    }

    @Override
    public void registerUseBlock(UseBlockHandler handler) {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            InteractionResult result = handler.use(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec()
            );
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    @Override
    public void registerServerReloadListener(Identifier id, PreparableReloadListener listener) {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, event -> event.addListener(id, listener));
    }
}
