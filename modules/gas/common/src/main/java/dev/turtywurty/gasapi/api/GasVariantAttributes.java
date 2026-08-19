package dev.turtywurty.gasapi.api;

import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Loader-independent variant display attributes. */
public final class GasVariantAttributes {
    private static final Map<Gas, GasVariantAttributeHandler> HANDLERS = new IdentityHashMap<>();
    private static final GasVariantAttributeHandler DEFAULT_HANDLER = variant -> {
        Identifier id = variant.holder().unwrapKey()
            .map(key -> key.identifier())
            .orElse(GasApi.id("unknown"));
        return Component.translatable("gas." + id.getNamespace() + "." + id.getPath());
    };

    private GasVariantAttributes() {
    }

    public static synchronized void register(Gas gas, GasVariantAttributeHandler handler) {
        if (HANDLERS.putIfAbsent(Objects.requireNonNull(gas, "gas"), Objects.requireNonNull(handler, "handler")) != null)
            throw new IllegalArgumentException("Duplicate handler registration for gas " + gas);
    }

    public static synchronized GasVariantAttributeHandler getHandler(Gas gas) {
        return HANDLERS.get(gas);
    }

    public static synchronized GasVariantAttributeHandler getHandlerOrDefault(Gas gas) {
        return HANDLERS.getOrDefault(gas, DEFAULT_HANDLER);
    }

    public static Component getName(ResourceVariant<Gas> gasVariant) {
        return getHandlerOrDefault(gasVariant.value()).getName(gasVariant);
    }
}
