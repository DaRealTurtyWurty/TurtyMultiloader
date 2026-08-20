package dev.turtywurty.turtymultiloader.config;

/**
 * Reason a new configuration value became active.
 */
public enum ConfigLifecycle {
    LOAD,
    RELOAD,
    SAVE,
    SET,
    COMMAND,
    SYNCHRONIZE
}
