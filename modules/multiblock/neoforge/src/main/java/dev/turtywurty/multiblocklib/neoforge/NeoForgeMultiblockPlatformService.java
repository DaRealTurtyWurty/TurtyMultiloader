package dev.turtywurty.multiblocklib.neoforge;

import dev.turtywurty.multiblocklib.platform.MultiblockPlatformService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class NeoForgeMultiblockPlatformService implements MultiblockPlatformService {
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
