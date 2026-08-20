package dev.turtywurty.turtymultiloader;

import dev.turtywurty.turtymultiloader.config.Configurations;
import dev.turtywurty.turtymultiloader.integration.OptionalModIntegrations;
import dev.turtywurty.turtymultiloader.platform.Platform;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CommonClient {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private CommonClient() {
    }

    public static void init() {
        if (!Platform.physicalSide().isClient()) {
            TurtyMultiloader.LOGGER.warn("Ignoring client initialization on a dedicated server");
            return;
        }

        CommonMod.init();
        if (INITIALIZED.compareAndSet(false, true)) {
            Configurations.initializeClient();
            OptionalModIntegrations.initializeClient();
        }
    }
}
