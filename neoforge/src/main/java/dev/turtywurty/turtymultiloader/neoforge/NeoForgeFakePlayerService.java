package dev.turtywurty.turtymultiloader.neoforge;

import com.mojang.authlib.GameProfile;
import dev.turtywurty.turtymultiloader.player.FakePlayerService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class NeoForgeFakePlayerService implements FakePlayerService {
    @Override
    public ServerPlayer get(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }
}
