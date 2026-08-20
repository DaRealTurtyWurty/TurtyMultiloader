package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.config.ConfigHandle;
import dev.turtywurty.turtymultiloader.config.ConfigurationManager;
import dev.turtywurty.turtymultiloader.config.ConfigurationService;
import dev.turtywurty.turtymultiloader.config.ConfigurationSpec;

public final class FabricConfigurationService implements ConfigurationService {
    private static final ConfigurationManager MANAGER = ConfigurationManager.instance();

    @Override
    public <T> ConfigHandle<T> register(ConfigurationSpec<T> spec) {
        return MANAGER.register(spec);
    }

    @Override
    public void initializeCommon() {
        MANAGER.initializeCommon();
    }

    @Override
    public void initializeClient() {
        MANAGER.initializeClient();
    }
}
