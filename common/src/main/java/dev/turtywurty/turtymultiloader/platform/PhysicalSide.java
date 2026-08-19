package dev.turtywurty.turtymultiloader.platform;

public enum PhysicalSide {
    CLIENT("physical client"),
    DEDICATED_SERVER("dedicated server");

    private final String displayName;

    PhysicalSide(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isDedicatedServer() {
        return this == DEDICATED_SERVER;
    }
}
