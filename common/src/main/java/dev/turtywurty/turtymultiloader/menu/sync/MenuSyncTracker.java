package dev.turtywurty.turtymultiloader.menu.sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Change detector for a {@link MenuSyncChannel}. Invoke {@link #broadcastChanges()} after the menu's vanilla
 * {@code broadcastChanges()} implementation.
 */
public final class MenuSyncTracker<M extends AbstractContainerMenu, T> {
    private final ServerPlayer player;
    private final M menu;
    private final MenuSyncChannel<M, T> channel;
    private final Supplier<? extends T> value;
    private final Function<? super T, ? extends T> snapshot;
    private T lastValue;
    private boolean initialized;

    private MenuSyncTracker(
        ServerPlayer player,
        M menu,
        MenuSyncChannel<M, T> channel,
        Supplier<? extends T> value,
        Function<? super T, ? extends T> snapshot
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.value = Objects.requireNonNull(value, "value");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    /**
     * Creates a tracker for immutable values such as records, strings, boxed numbers, or immutable collections.
     */
    public static <M extends AbstractContainerMenu, T> MenuSyncTracker<M, T> immutable(
        ServerPlayer player,
        M menu,
        MenuSyncChannel<M, T> channel,
        Supplier<? extends T> value
    ) {
        return new MenuSyncTracker<>(player, menu, channel, value, Function.identity());
    }

    /**
     * Creates a tracker whose snapshot function must return a stable copy when values can mutate in place.
     */
    public static <M extends AbstractContainerMenu, T> MenuSyncTracker<M, T> copying(
        ServerPlayer player,
        M menu,
        MenuSyncChannel<M, T> channel,
        Supplier<? extends T> value,
        Function<? super T, ? extends T> snapshot
    ) {
        return new MenuSyncTracker<>(player, menu, channel, value, snapshot);
    }

    public boolean broadcastChanges() {
        T current = Objects.requireNonNull(value.get(), "synchronized menu value");
        if (initialized && Objects.equals(lastValue, current))
            return false;

        if (!channel.send(player, menu, current))
            return false;

        lastValue = Objects.requireNonNull(snapshot.apply(current), "menu value snapshot");
        initialized = true;
        return true;
    }

    public boolean force() {
        initialized = false;
        return broadcastChanges();
    }
}
