package dev.turtywurty.turtymultiloader.worldgen;

/**
 * Controls how a built-in datapack appears in the pack repository.
 */
public enum BuiltInDatapackActivation {
    /**
     * The user must explicitly enable the pack.
     */
    NORMAL,
    /**
     * The pack is enabled for new worlds, but the user may disable it.
     */
    DEFAULT_ENABLED,
    /**
     * The pack is always enabled and cannot be disabled.
     */
    ALWAYS_ENABLED
}
