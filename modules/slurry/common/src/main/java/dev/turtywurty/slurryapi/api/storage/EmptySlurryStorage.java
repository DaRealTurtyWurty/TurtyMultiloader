package dev.turtywurty.slurryapi.api.storage;

import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

public final class EmptySlurryStorage extends SingleSlurryStorage {
    public static final EmptySlurryStorage INSTANCE = new EmptySlurryStorage();

    private EmptySlurryStorage() {
        super(0, TransferSupport.NONE, () -> { });
    }
}
