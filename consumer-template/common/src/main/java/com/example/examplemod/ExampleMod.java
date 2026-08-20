package com.example.examplemod;

import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.transfer.TransferService;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ExampleMod {
    public static final String MOD_ID = "examplemod";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private ExampleMod() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true))
            return;
        // Declare common content before these lifecycle handoffs.
        RegistryService.get().apply();
        TransferService.get().apply();
    }
}

