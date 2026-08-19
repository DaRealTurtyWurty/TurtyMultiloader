package dev.turtywurty.turtymultiloader.platform;

import java.nio.file.Path;
import java.util.Optional;

public interface PlatformService {
    Loader loader();

    PhysicalSide physicalSide();

    boolean isDevelopmentEnvironment();

    boolean isModLoaded(String modId);

    Optional<String> modVersion(String modId);

    Path gameDirectory();

    Path configDirectory();

    default Path savesDirectory() {
        return gameDirectory().resolve("saves");
    }

    default Path exportDirectory() {
        return gameDirectory().resolve("exports");
    }
}
