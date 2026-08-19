package dev.turtywurty.turtymultiloader.platform;

import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public final class Platform {
    private Platform() {
    }

    public static Loader loader() {
        return service().loader();
    }

    public static PhysicalSide physicalSide() {
        return service().physicalSide();
    }

    public static LogicalSide logicalSide(Level level) {
        return LogicalSide.from(Objects.requireNonNull(level, "level"));
    }

    public static boolean isDevelopmentEnvironment() {
        return service().isDevelopmentEnvironment();
    }

    public static boolean isModLoaded(String modId) {
        return service().isModLoaded(requireModId(modId));
    }

    public static Optional<String> modVersion(String modId) {
        return service().modVersion(requireModId(modId));
    }

    public static Path gameDirectory() {
        return service().gameDirectory();
    }

    public static Path configDirectory() {
        return service().configDirectory();
    }

    public static Path savesDirectory() {
        return service().savesDirectory();
    }

    public static Path exportDirectory() {
        return service().exportDirectory();
    }

    private static PlatformService service() {
        return ServiceHolder.INSTANCE;
    }

    private static String requireModId(String modId) {
        if (Objects.requireNonNull(modId, "modId").isBlank())
            throw new IllegalArgumentException("modId must not be blank");

        return modId;
    }

    private static final class ServiceHolder {
        private static final PlatformService INSTANCE = load();

        private ServiceHolder() {
        }

        private static PlatformService load() {
            return ServiceLoader.load(PlatformService.class, Platform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No platform service is available"));
        }
    }
}
