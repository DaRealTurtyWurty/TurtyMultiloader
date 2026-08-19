package dev.turtywurty.turtymultiloader.platform;

public enum Loader {
    FABRIC("Fabric"),
    NEOFORGE("NeoForge");

    private final String displayName;

    Loader(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
