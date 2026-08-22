package dev.turtywurty.multiblocklib.fabric;

import dev.turtywurty.multiblocklib.platform.MultiblockPlatformService;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class FabricMultiblockPlatformService implements MultiblockPlatformService {
    @Override
    public void registerUseBlock(UseBlockHandler handler) {
        UseBlockCallback.EVENT.register(handler::use);
    }

    @Override
    public void registerServerReloadListener(Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener);
    }
}
