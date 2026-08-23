package dev.turtywurty.turtymultiloader.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ServiceLoader;

/**
 * Loader-neutral access to a server-side fake player.
 */
public interface FakePlayerService {
    static FakePlayerService get() {
        return ServiceHolder.INSTANCE;
    }

    ServerPlayer get(ServerLevel level, GameProfile profile);

    final class ServiceHolder {
        private static final FakePlayerService INSTANCE = ServiceLoader.load(
                FakePlayerService.class,
                FakePlayerService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No fake-player service is available"));

        private ServiceHolder() {
        }
    }
}
