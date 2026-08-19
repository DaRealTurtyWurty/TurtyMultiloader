package dev.turtywurty.turtymultiloader.registration;

/** Options supported by both loader implementations when creating a custom registry. */
public record CustomRegistryOptions(boolean synced, boolean intrusiveHolders) {
    public static final CustomRegistryOptions DEFAULT = new CustomRegistryOptions(false, false);

    public CustomRegistryOptions withSync() {
        return new CustomRegistryOptions(true, intrusiveHolders);
    }

    public CustomRegistryOptions withIntrusiveHolders() {
        return new CustomRegistryOptions(synced, true);
    }
}
