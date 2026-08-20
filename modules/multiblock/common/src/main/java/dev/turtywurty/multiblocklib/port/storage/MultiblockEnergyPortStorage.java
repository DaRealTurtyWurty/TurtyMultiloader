package dev.turtywurty.multiblocklib.port.storage;

import dev.turtywurty.multiblocklib.data.PortIO;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleEnergyStorage;

public class MultiblockEnergyPortStorage extends SimpleEnergyStorage {
    private final Runnable onChange;

    public MultiblockEnergyPortStorage(final long capacity, final PortIO io, final Runnable onChange) {
        super(capacity, capacity, capacity);
        this.onChange = onChange;
    }

    @Override
    protected void onFinalCommit() {
        this.onChange.run();
    }
}
