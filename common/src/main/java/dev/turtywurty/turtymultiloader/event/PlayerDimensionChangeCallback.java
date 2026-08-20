package dev.turtywurty.turtymultiloader.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Runs after a server player has entered a different dimension.
 *
 * <p>The player is already in {@code destination} when this callback runs. This makes the callback suitable for
 * resending dimension-scoped state to the player.</p>
 */
@FunctionalInterface
public interface PlayerDimensionChangeCallback {
    void afterDimensionChange(ServerPlayer player, ServerLevel origin, ServerLevel destination);
}
