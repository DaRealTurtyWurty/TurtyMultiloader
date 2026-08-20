package dev.turtywurty.turtymultiloader.config;

import java.util.ServiceLoader;

/**
 * Loader service responsible for connecting the common config manager to loader initialization.
 */
public interface ConfigurationService {
    static ConfigurationService get() {
        return ServiceHolder.INSTANCE;
    }

    <T> ConfigHandle<T> register(ConfigurationSpec<T> spec);

    void initializeCommon();

    void initializeClient();

    final class ServiceHolder {
        private static final ConfigurationService INSTANCE = ServiceLoader.load(
                ConfigurationService.class,
                ConfigurationService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No configuration service is available"));

        private ServiceHolder() {
        }
    }
}
