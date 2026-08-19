package dev.turtywurty.turtymultiloader;

import dev.turtywurty.turtymultiloader.integration.OptionalModIntegrations;
import dev.turtywurty.turtymultiloader.platform.Platform;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CommonMod {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private CommonMod() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true))
            return;

        TurtyMultiloader.LOGGER.info(
            "Initializing {} on {} ({})",
            TurtyMultiloader.MOD_NAME,
            Platform.loader().displayName(),
            Platform.physicalSide().displayName()
        );
        OptionalModIntegrations.initializeCommon();
    }
}
