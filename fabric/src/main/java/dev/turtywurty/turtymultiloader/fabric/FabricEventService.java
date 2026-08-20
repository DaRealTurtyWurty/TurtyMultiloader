package dev.turtywurty.turtymultiloader.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.turtywurty.turtymultiloader.event.BlockBreakCallback;
import dev.turtywurty.turtymultiloader.event.EventService;
import dev.turtywurty.turtymultiloader.event.LivingDamageCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class FabricEventService implements EventService {
    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTING.register(require(callback)::accept);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTED.register(require(callback)::accept);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STOPPING.register(require(callback)::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STOPPED.register(require(callback)::accept);
    }

    @Override
    public void onLevelLoad(Consumer<ServerLevel> callback) {
        ServerLevelEvents.LOAD.register((server, level) -> require(callback).accept(level));
    }

    @Override
    public void onLevelUnload(Consumer<ServerLevel> callback) {
        ServerLevelEvents.UNLOAD.register((server, level) -> require(callback).accept(level));
    }

    @Override
    public void onStartServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.START_SERVER_TICK.register(require(callback)::accept);
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.END_SERVER_TICK.register(require(callback)::accept);
    }

    @Override
    public void onStartLevelTick(Consumer<ServerLevel> callback) {
        ServerTickEvents.START_LEVEL_TICK.register(require(callback)::accept);
    }

    @Override
    public void onEndLevelTick(Consumer<ServerLevel> callback) {
        ServerTickEvents.END_LEVEL_TICK.register(require(callback)::accept);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        ServerPlayerEvents.JOIN.register(require(callback)::accept);
    }

    @Override
    public void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        ServerPlayerEvents.LEAVE.register(require(callback)::accept);
    }

    @Override
    public void onPlayerRespawn(Consumer<ServerPlayer> callback) {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            require(callback).accept(newPlayer)
        );
    }

    @Override
    public void onBlockBroken(BlockBreakCallback callback) {
        require(callback);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer)
                callback.afterBlockBreak(serverLevel, serverPlayer, pos, state, blockEntity);
        });
    }

    @Override
    public void onLivingDamaged(LivingDamageCallback callback) {
        require(callback);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
            callback.afterDamage(entity, source, damageTaken)
        );
    }

    @Override
    public void onLivingKilled(BiConsumer<LivingEntity, DamageSource> callback) {
        ServerLivingEntityEvents.AFTER_DEATH.register(require(callback)::accept);
    }

    @Override
    public void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
            require(callback).accept(dispatcher)
        );
    }

    @Override
    public void onDatapackReload(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success)
                require(callback).accept(server);
        });
    }

    private static <T> T require(T callback) {
        return Objects.requireNonNull(callback, "callback");
    }
}
