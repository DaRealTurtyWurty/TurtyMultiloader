package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.authlib.GameProfile;
import dev.turtywurty.turtymultiloader.player.FakePlayerService;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricFakePlayerService implements FakePlayerService {
    @Override
    public ServerPlayer get(ServerLevel level, GameProfile profile) {
        return FakePlayer.get(level, profile);
    }
}
