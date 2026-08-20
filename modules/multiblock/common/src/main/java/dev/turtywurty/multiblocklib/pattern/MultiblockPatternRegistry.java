package dev.turtywurty.multiblocklib.pattern;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MultiblockPatternRegistry {
    private static final Map<Identifier, MultiblockPatternFactory> FACTORIES = new HashMap<>();

    private MultiblockPatternRegistry() {
    }

    public static void register(final Identifier id, final MultiblockPatternFactory factory) {
        FACTORIES.put(id, factory);
    }

    public static MultiblockPatternFactory get(final Identifier id) {
        return FACTORIES.get(id);
    }

    public static Map<Identifier, MultiblockPatternFactory> factories() {
        return Collections.unmodifiableMap(FACTORIES);
    }
}
