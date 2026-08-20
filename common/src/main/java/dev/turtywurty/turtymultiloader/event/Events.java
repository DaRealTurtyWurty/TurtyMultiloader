package dev.turtywurty.turtymultiloader.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Loader-neutral callbacks for the common lifecycle events used by Industria.
 */
public final class Events {
    private Events() {
    }

    public static void onServerStarting(Consumer<MinecraftServer> callback) {
        EventService.get().onServerStarting(require(callback));
    }

    public static void onServerStarted(Consumer<MinecraftServer> callback) {
        EventService.get().onServerStarted(require(callback));
    }

    public static void onServerStopping(Consumer<MinecraftServer> callback) {
        EventService.get().onServerStopping(require(callback));
    }

    public static void onServerStopped(Consumer<MinecraftServer> callback) {
        EventService.get().onServerStopped(require(callback));
    }

    public static void onLevelLoad(Consumer<ServerLevel> callback) {
        EventService.get().onLevelLoad(require(callback));
    }

    public static void onLevelUnload(Consumer<ServerLevel> callback) {
        EventService.get().onLevelUnload(require(callback));
    }

    public static void onStartServerTick(Consumer<MinecraftServer> callback) {
        EventService.get().onStartServerTick(require(callback));
    }

    public static void onEndServerTick(Consumer<MinecraftServer> callback) {
        EventService.get().onEndServerTick(require(callback));
    }

    public static void onStartLevelTick(Consumer<ServerLevel> callback) {
        EventService.get().onStartLevelTick(require(callback));
    }

    public static void onEndLevelTick(Consumer<ServerLevel> callback) {
        EventService.get().onEndLevelTick(require(callback));
    }

    public static void onPlayerJoin(Consumer<ServerPlayer> callback) {
        EventService.get().onPlayerJoin(require(callback));
    }

    public static void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        EventService.get().onPlayerDisconnect(require(callback));
    }

    public static void onPlayerRespawn(Consumer<ServerPlayer> callback) {
        EventService.get().onPlayerRespawn(require(callback));
    }

    /**
     * Registers a callback that runs after a server player has entered another dimension.
     */
    public static void onPlayerDimensionChange(PlayerDimensionChangeCallback callback) {
        EventService.get().onPlayerDimensionChange(require(callback));
    }

    public static void onBlockBroken(BlockBreakCallback callback) {
        EventService.get().onBlockBroken(require(callback));
    }

    public static void onLivingDamaged(LivingDamageCallback callback) {
        EventService.get().onLivingDamaged(require(callback));
    }

    public static void onLivingKilled(BiConsumer<LivingEntity, DamageSource> callback) {
        EventService.get().onLivingKilled(require(callback));
    }

    public static void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        EventService.get().onCommandRegistration(require(callback));
    }

    public static void onDatapackReload(Consumer<MinecraftServer> callback) {
        EventService.get().onDatapackReload(require(callback));
    }

    private static <T> T require(T callback) {
        return Objects.requireNonNull(callback, "callback");
    }
}
