package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.platform.Loader;
import dev.turtywurty.turtymultiloader.platform.PhysicalSide;
import dev.turtywurty.turtymultiloader.platform.PlatformService;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Optional;

public final class FabricPlatformService implements PlatformService {
    private static final FabricLoader LOADER = FabricLoader.getInstance();

    @Override
    public Loader loader() {
        return Loader.FABRIC;
    }

    @Override
    public PhysicalSide physicalSide() {
        return LOADER.getEnvironmentType() == EnvType.CLIENT
            ? PhysicalSide.CLIENT
            : PhysicalSide.DEDICATED_SERVER;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return LOADER.isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LOADER.isModLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return LOADER.getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public Path gameDirectory() {
        return LOADER.getGameDir();
    }

    @Override
    public Path configDirectory() {
        return LOADER.getConfigDir();
    }
}
