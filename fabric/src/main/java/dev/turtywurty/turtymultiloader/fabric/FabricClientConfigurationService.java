package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.config.ConfigurationManager;
import dev.turtywurty.turtymultiloader.config.client.ClientConfigurationService;
import dev.turtywurty.turtymultiloader.config.client.ConfigScreenFactory;

public final class FabricClientConfigurationService implements ClientConfigurationService {
    @Override
    public void registerScreen(String modId, ConfigScreenFactory factory) {
        ConfigurationManager.instance().registerScreen(modId, factory);
    }
}
