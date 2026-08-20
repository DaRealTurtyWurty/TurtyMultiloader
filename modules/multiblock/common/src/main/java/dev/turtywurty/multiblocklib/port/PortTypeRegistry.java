package dev.turtywurty.multiblocklib.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PortTypeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiblockLib/PortTypes");
    private static final Map<String, PortType> TYPES = new HashMap<>();

    private PortTypeRegistry() {
    }

    public static void register(final String id, final PortType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Port type id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Port type must not be null");
        }
        String normalizedId = normalize(id);
        if (TYPES.putIfAbsent(normalizedId, type) != null) {
            throw new IllegalStateException("Port type already registered: " + normalizedId);
        }
        LOGGER.info("Registered port type {}", normalizedId);
    }

    public static PortType get(final String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return TYPES.get(normalize(id));
    }

    public static Map<String, PortType> getAll() {
        return Collections.unmodifiableMap(TYPES);
    }

    private static String normalize(final String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
