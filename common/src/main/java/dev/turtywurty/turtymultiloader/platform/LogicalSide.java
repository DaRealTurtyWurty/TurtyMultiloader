package dev.turtywurty.turtymultiloader.platform;

import net.minecraft.world.level.Level;

public enum LogicalSide {
    CLIENT,
    SERVER;

    public static LogicalSide from(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isServer() {
        return this == SERVER;
    }
}
