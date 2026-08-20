package dev.turtywurty.turtymultiloader.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.turtywurty.turtymultiloader.event.BlockBreakCallback;
import dev.turtywurty.turtymultiloader.event.EventService;
import dev.turtywurty.turtymultiloader.event.LivingDamageCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NeoForgeEventService implements EventService {
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> serverStartingCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> serverStartedCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> serverStoppingCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> serverStoppedCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerLevel>> levelLoadCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerLevel>> levelUnloadCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> startServerTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> endServerTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerLevel>> startLevelTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerLevel>> endLevelTickCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerPlayer>> playerJoinCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerPlayer>> playerDisconnectCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ServerPlayer>> playerRespawnCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BlockBreakCallback> blockBreakCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<LivingDamageCallback> livingDamageCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BiConsumer<LivingEntity, DamageSource>> livingKillCallbacks =
        new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<CommandDispatcher<CommandSourceStack>>> commandCallbacks =
        new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MinecraftServer>> datapackReloadCallbacks =
        new CopyOnWriteArrayList<>();

    public NeoForgeEventService() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ServerStartingEvent.class, this::handleServerStarting);
        bus.addListener(ServerStartedEvent.class, this::handleServerStarted);
        bus.addListener(ServerStoppingEvent.class, this::handleServerStopping);
        bus.addListener(ServerStoppedEvent.class, this::handleServerStopped);
        bus.addListener(LevelEvent.Load.class, this::handleLevelLoad);
        bus.addListener(LevelEvent.Unload.class, this::handleLevelUnload);
        bus.addListener(ServerTickEvent.Pre.class, this::handleStartServerTick);
        bus.addListener(ServerTickEvent.Post.class, this::handleEndServerTick);
        bus.addListener(LevelTickEvent.Pre.class, this::handleStartLevelTick);
        bus.addListener(LevelTickEvent.Post.class, this::handleEndLevelTick);
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, this::handlePlayerJoin);
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, this::handlePlayerDisconnect);
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, this::handlePlayerRespawn);
        bus.addListener(BlockDropsEvent.class, this::handleBlockDrops);
        bus.addListener(LivingDamageEvent.Post.class, this::handleLivingDamage);
        bus.addListener(LivingDeathEvent.class, this::handleLivingDeath);
        bus.addListener(RegisterCommandsEvent.class, this::handleCommandRegistration);
        bus.addListener(OnDatapackSyncEvent.class, this::handleDatapackSync);
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        serverStartingCallbacks.add(require(callback));
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        serverStartedCallbacks.add(require(callback));
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        serverStoppingCallbacks.add(require(callback));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        serverStoppedCallbacks.add(require(callback));
    }

    @Override
    public void onLevelLoad(Consumer<ServerLevel> callback) {
        levelLoadCallbacks.add(require(callback));
    }

    @Override
    public void onLevelUnload(Consumer<ServerLevel> callback) {
        levelUnloadCallbacks.add(require(callback));
    }

    @Override
    public void onStartServerTick(Consumer<MinecraftServer> callback) {
        startServerTickCallbacks.add(require(callback));
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> callback) {
        endServerTickCallbacks.add(require(callback));
    }

    @Override
    public void onStartLevelTick(Consumer<ServerLevel> callback) {
        startLevelTickCallbacks.add(require(callback));
    }

    @Override
    public void onEndLevelTick(Consumer<ServerLevel> callback) {
        endLevelTickCallbacks.add(require(callback));
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        playerJoinCallbacks.add(require(callback));
    }

    @Override
    public void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        playerDisconnectCallbacks.add(require(callback));
    }

    @Override
    public void onPlayerRespawn(Consumer<ServerPlayer> callback) {
        playerRespawnCallbacks.add(require(callback));
    }

    @Override
    public void onBlockBroken(BlockBreakCallback callback) {
        blockBreakCallbacks.add(require(callback));
    }

    @Override
    public void onLivingDamaged(LivingDamageCallback callback) {
        livingDamageCallbacks.add(require(callback));
    }

    @Override
    public void onLivingKilled(BiConsumer<LivingEntity, DamageSource> callback) {
        livingKillCallbacks.add(require(callback));
    }

    @Override
    public void onCommandRegistration(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
        commandCallbacks.add(require(callback));
    }

    @Override
    public void onDatapackReload(Consumer<MinecraftServer> callback) {
        datapackReloadCallbacks.add(require(callback));
    }

    private void handleServerStarting(ServerStartingEvent event) {
        serverStartingCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleServerStarted(ServerStartedEvent event) {
        serverStartedCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleServerStopping(ServerStoppingEvent event) {
        serverStoppingCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleServerStopped(ServerStoppedEvent event) {
        serverStoppedCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level)
            levelLoadCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level)
            levelUnloadCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleStartServerTick(ServerTickEvent.Pre event) {
        startServerTickCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleEndServerTick(ServerTickEvent.Post event) {
        endServerTickCallbacks.forEach(callback -> callback.accept(event.getServer()));
    }

    private void handleStartLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level)
            startLevelTickCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handleEndLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level)
            endLevelTickCallbacks.forEach(callback -> callback.accept(level));
    }

    private void handlePlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            playerJoinCallbacks.forEach(callback -> callback.accept(player));
    }

    private void handlePlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            playerDisconnectCallbacks.forEach(callback -> callback.accept(player));
    }

    private void handlePlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            playerRespawnCallbacks.forEach(callback -> callback.accept(player));
    }

    private void handleBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player))
            return;
        blockBreakCallbacks.forEach(callback -> callback.afterBlockBreak(
            event.getLevel(),
            player,
            event.getPos(),
            event.getState(),
            event.getBlockEntity()
        ));
    }

    private void handleLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide())
            return;
        livingDamageCallbacks.forEach(callback -> callback.afterDamage(
            event.getEntity(),
            event.getSource(),
            event.getNewDamage()
        ));
    }

    private void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        livingKillCallbacks.forEach(callback -> callback.accept(event.getEntity(), event.getSource()));
    }

    private void handleCommandRegistration(RegisterCommandsEvent event) {
        commandCallbacks.forEach(callback -> callback.accept(event.getDispatcher()));
    }

    private void handleDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            MinecraftServer server = event.getPlayerList().getServer();
            datapackReloadCallbacks.forEach(callback -> callback.accept(server));
        }
    }

    private static <T> T require(T callback) {
        return Objects.requireNonNull(callback, "callback");
    }
}
