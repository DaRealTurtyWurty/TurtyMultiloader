package dev.turtywurty.turtymultiloader.config;

/**
 * Defines where and when a configuration is used. The semantics mirror NeoForge's config types without exposing a
 * loader-specific config specification.
 */
public enum ConfigScope {
    /**
     * Loaded as soon as it is registered on both physical sides. Never synchronized.
     */
    STARTUP("startup", false),
    /**
     * Loaded only on a physical client. Never synchronized.
     */
    CLIENT("client", false),
    /**
     * Loaded on both physical sides during normal initialization. Never synchronized.
     */
    COMMON("common", false),
    /**
     * Loaded for a server world and synchronized from that server to its clients.
     */
    SERVER("server", true);

    private final String fileSuffix;
    private final boolean synchronizedToClient;

    ConfigScope(String fileSuffix, boolean synchronizedToClient) {
        this.fileSuffix = fileSuffix;
        this.synchronizedToClient = synchronizedToClient;
    }

    public String fileSuffix() {
        return fileSuffix;
    }

    public boolean synchronizedToClient() {
        return synchronizedToClient;
    }
}
