package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.platform.Loader;
import dev.turtywurty.turtymultiloader.platform.PhysicalSide;
import dev.turtywurty.turtymultiloader.platform.PlatformService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Optional;

public final class NeoForgePlatformService implements PlatformService {
    @Override
    public Loader loader() {
        return Loader.NEOFORGE;
    }

    @Override
    public PhysicalSide physicalSide() {
        return FMLEnvironment.getDist() == Dist.CLIENT
            ? PhysicalSide.CLIENT
            : PhysicalSide.DEDICATED_SERVER;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return ModList.get()
            .getModContainerById(modId)
            .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
