package dev.turtywurty.turtymultiloader.integration;

import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import dev.turtywurty.turtymultiloader.platform.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class OptionalModIntegrations {
    private static final Object LOCK = new Object();
    private static final List<Registration> COMMON = new ArrayList<>();
    private static final List<Registration> CLIENT = new ArrayList<>();

    private static boolean commonInitialized;
    private static boolean clientInitialized;

    private OptionalModIntegrations() {
    }

    public static void register(String requiredModId, Supplier<? extends ModIntegration> factory) {
        register(COMMON, Phase.COMMON, requiredModId, factory);
    }

    public static void registerClient(String requiredModId, Supplier<? extends ModIntegration> factory) {
        register(CLIENT, Phase.CLIENT, requiredModId, factory);
    }

    public static void initializeCommon() {
        initialize(COMMON, Phase.COMMON);
    }

    public static void initializeClient() {
        if (!Platform.physicalSide().isClient())
            return;

        initialize(CLIENT, Phase.CLIENT);
    }

    private static void register(
        List<Registration> registrations,
        Phase phase,
        String requiredModId,
        Supplier<? extends ModIntegration> factory
    ) {
        Registration registration = new Registration(requiredModId, factory);
        boolean initializeNow;
        synchronized (LOCK) {
            registrations.add(registration);
            initializeNow = isInitialized(phase);
        }

        if (initializeNow) {
            initialize(registration, phase);
        }
    }

    private static void initialize(List<Registration> registrations, Phase phase) {
        List<Registration> pending;
        synchronized (LOCK) {
            if (isInitialized(phase))
                return;

            setInitialized(phase);
            pending = List.copyOf(registrations);
        }

        pending.forEach(registration -> initialize(registration, phase));
    }

    private static void initialize(Registration registration, Phase phase) {
        if (!Platform.isModLoaded(registration.requiredModId()))
            return;

        try {
            registration.factory().get().initialize();
            TurtyMultiloader.LOGGER.info(
                "Loaded {} integration for {}",
                phase.displayName,
                registration.requiredModId()
            );
        } catch (RuntimeException | LinkageError exception) {
            TurtyMultiloader.LOGGER.error(
                "Failed to load {} integration for {}",
                phase.displayName,
                registration.requiredModId(),
                exception
            );
        }
    }

    private static boolean isInitialized(Phase phase) {
        return phase == Phase.COMMON ? commonInitialized : clientInitialized;
    }

    private static void setInitialized(Phase phase) {
        if (phase == Phase.COMMON) {
            commonInitialized = true;
        } else {
            clientInitialized = true;
        }
    }

    private record Registration(String requiredModId, Supplier<? extends ModIntegration> factory) {
        private Registration {
            if (Objects.requireNonNull(requiredModId, "requiredModId").isBlank())
                throw new IllegalArgumentException("requiredModId must not be blank");

            Objects.requireNonNull(factory, "factory");
        }
    }

    private enum Phase {
        COMMON("common"),
        CLIENT("client");

        private final String displayName;

        Phase(String displayName) {
            this.displayName = displayName;
        }
    }
}
