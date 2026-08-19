package dev.turtywurty.slurryapi.api;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loader-independent slurry display and container-sound attributes.
 */
public final class SlurryVariantAttributes {
    private static final Map<Slurry, SlurryVariantAttributeHandler> HANDLERS = new IdentityHashMap<>();
    private static final SlurryVariantAttributeHandler DEFAULT_HANDLER = variant -> {
        Identifier id = variant.holder().unwrapKey()
            .map(key -> key.identifier())
            .orElse(SlurryApi.id("unknown"));
        return Component.translatable("slurry." + id.getNamespace() + "." + id.getPath());
    };

    private SlurryVariantAttributes() {
    }

    public static synchronized void register(Slurry slurry, SlurryVariantAttributeHandler handler) {
        if (HANDLERS.putIfAbsent(
            Objects.requireNonNull(slurry, "slurry"),
            Objects.requireNonNull(handler, "handler")
        ) != null)
            throw new IllegalArgumentException("Duplicate handler registration for slurry " + slurry);
    }

    public static synchronized SlurryVariantAttributeHandler getHandler(Slurry slurry) {
        return HANDLERS.get(slurry);
    }

    public static synchronized SlurryVariantAttributeHandler getHandlerOrDefault(Slurry slurry) {
        return HANDLERS.getOrDefault(slurry, DEFAULT_HANDLER);
    }

    public static Component getName(ResourceVariant<Slurry> variant) {
        return getHandlerOrDefault(variant.value()).getName(variant);
    }

    public static SoundEvent getFillSound(ResourceVariant<Slurry> variant, Item handItem) {
        return getHandlerOrDefault(variant.value()).getFillSound(variant, handItem).orElse(SoundEvents.BUCKET_FILL);
    }

    public static SoundEvent getEmptySound(ResourceVariant<Slurry> variant, Item handItem) {
        return getHandlerOrDefault(variant.value()).getEmptySound(variant, handItem).orElse(SoundEvents.BUCKET_EMPTY);
    }
}
