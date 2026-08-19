package dev.turtywurty.gasapi.api.storage;

import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;

public final class EmptyGasStorage extends SingleGasStorage {
    public static final EmptyGasStorage INSTANCE = new EmptyGasStorage();

    private EmptyGasStorage() {
        super(0, TransferSupport.NONE, () -> {
        });
    }
}
