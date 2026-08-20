package dev.turtywurty.turtymultiloader.config.client;

import java.util.ServiceLoader;

/**
 * Client-only loader service for optional config screen integration.
 */
public interface ClientConfigurationService {
    static ClientConfigurationService get() {
        return ServiceHolder.INSTANCE;
    }

    void registerScreen(String modId, ConfigScreenFactory factory);

    final class ServiceHolder {
        private static final ClientConfigurationService INSTANCE = ServiceLoader.load(
                ClientConfigurationService.class,
                ClientConfigurationService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client configuration service is available"));

        private ServiceHolder() {
        }
    }
}
