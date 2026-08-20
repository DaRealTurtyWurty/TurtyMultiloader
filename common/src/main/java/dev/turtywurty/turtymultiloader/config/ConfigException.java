package dev.turtywurty.turtymultiloader.config;

/**
 * Indicates that a config could not be resolved, decoded, validated, or persisted.
 */
public final class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
