package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.config.ConfigurationManager;
import dev.turtywurty.turtymultiloader.config.client.ClientConfigurationService;
import dev.turtywurty.turtymultiloader.config.client.ConfigScreenFactory;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class NeoForgeClientConfigurationService implements ClientConfigurationService {
    @Override
    public void registerScreen(String modId, ConfigScreenFactory factory) {
        ConfigurationManager.instance().registerScreen(modId, factory);
        var container = ModList.get().getModContainerById(modId).orElseThrow(() ->
            new IllegalArgumentException("No loaded mod has id " + modId)
        );
        IConfigScreenFactory extension = (ignored, parent) -> factory.create(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, extension);
    }
}
