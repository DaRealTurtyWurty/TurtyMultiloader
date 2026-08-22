package dev.turtywurty.multiblocklib.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ServiceLoader;

public interface MultiblockPlatformService {
    static MultiblockPlatformService get() {
        return ServiceHolder.INSTANCE;
    }

    void registerUseBlock(UseBlockHandler handler);

    void registerServerReloadListener(Identifier id, PreparableReloadListener listener);

    @FunctionalInterface
    interface UseBlockHandler {
        InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hitResult);
    }

    final class ServiceHolder {
        private static final MultiblockPlatformService INSTANCE = ServiceLoader.load(
                MultiblockPlatformService.class,
                MultiblockPlatformService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No multiblock platform service is available"));

        private ServiceHolder() {
        }
    }
}
