package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.event.Events;

public final class EventSmokeTest {
    private EventSmokeTest() {
    }

    public static void register() {
        Events.onServerStarting(server -> {
        });
        Events.onServerStarted(server -> {
        });
        Events.onServerStopping(server -> {
        });
        Events.onServerStopped(server -> {
        });
        Events.onLevelLoad(level -> {
        });
        Events.onLevelUnload(level -> {
        });
        Events.onStartServerTick(server -> {
        });
        Events.onEndServerTick(server -> {
        });
        Events.onStartLevelTick(level -> {
        });
        Events.onEndLevelTick(level -> {
        });
        Events.onPlayerJoin(player -> {
        });
        Events.onPlayerDisconnect(player -> {
        });
        Events.onPlayerRespawn(player -> {
        });
        Events.onBlockBroken((level, player, pos, state, blockEntity) -> {
        });
        Events.onLivingDamaged((entity, source, damageTaken) -> {
        });
        Events.onLivingKilled((entity, source) -> {
        });
        Events.onCommandRegistration(dispatcher -> {
        });
        Events.onDatapackReload(server -> {
        });
    }
}
