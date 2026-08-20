package dev.turtywurty.turtymultiloader.transfer.fluid;

import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loader-neutral fluid attributes with common, variant-aware overlays.
 *
 * <p>A registered handler takes precedence for its fluid. Unmodified handler methods continue to use the
 * loader's native attributes, so an overlay can safely replace only viscosity, luminance, or another individual
 * property. Register still and flowing fluids separately when both must share an overlay.</p>
 */
public final class FluidVariantAttributes {
    private static final Map<Fluid, FluidVariantAttributeHandler> HANDLERS = new IdentityHashMap<>();
    private static final FluidVariantAttributeHandler NATIVE_HANDLER = new FluidVariantAttributeHandler() {
    };

    private FluidVariantAttributes() {
    }

    public static synchronized void register(Fluid fluid, FluidVariantAttributeHandler handler) {
        if (HANDLERS.putIfAbsent(
            Objects.requireNonNull(fluid, "fluid"),
            Objects.requireNonNull(handler, "handler")
        ) != null)
            throw new IllegalArgumentException("Duplicate handler registration for fluid " + fluid);
    }

    public static synchronized @Nullable FluidVariantAttributeHandler getHandler(Fluid fluid) {
        return HANDLERS.get(Objects.requireNonNull(fluid, "fluid"));
    }

    public static synchronized FluidVariantAttributeHandler getHandlerOrDefault(Fluid fluid) {
        return HANDLERS.getOrDefault(Objects.requireNonNull(fluid, "fluid"), NATIVE_HANDLER);
    }

    public static Component getName(ResourceVariant<Fluid> variant) {
        return handler(variant).getName(variant);
    }

    public static SoundEvent getFillSound(ResourceVariant<Fluid> variant) {
        return handler(variant).getFillSound(variant).orElse(SoundEvents.BUCKET_FILL);
    }

    public static SoundEvent getEmptySound(ResourceVariant<Fluid> variant) {
        return handler(variant).getEmptySound(variant).orElse(SoundEvents.BUCKET_EMPTY);
    }

    public static int getLuminance(ResourceVariant<Fluid> variant) {
        int luminance = handler(variant).getLuminance(variant);
        if (luminance >= 0 && luminance <= 15)
            return luminance;
        TurtyMultiloader.LOGGER.warn("Invalid fluid luminance {} for {}; using the loader-native value", luminance,
            variant);
        return FluidAttributeService.get().getLuminance(variant);
    }

    public static int getTemperature(ResourceVariant<Fluid> variant) {
        int temperature = handler(variant).getTemperature(variant);
        if (temperature >= 0)
            return temperature;
        TurtyMultiloader.LOGGER.warn("Invalid fluid temperature {} for {}; using the loader-native value",
            temperature, variant);
        return FluidAttributeService.get().getTemperature(variant);
    }

    public static int getViscosity(ResourceVariant<Fluid> variant, @Nullable Level level) {
        int viscosity = handler(variant).getViscosity(variant, level);
        if (viscosity > 0)
            return viscosity;
        TurtyMultiloader.LOGGER.warn("Invalid fluid viscosity {} for {}; using the loader-native value", viscosity,
            variant);
        return FluidAttributeService.get().getViscosity(variant, level);
    }

    public static int getDensity(ResourceVariant<Fluid> variant) {
        return handler(variant).getDensity(variant);
    }

    public static boolean isLighterThanAir(ResourceVariant<Fluid> variant) {
        return handler(variant).isLighterThanAir(variant);
    }

    private static FluidVariantAttributeHandler handler(ResourceVariant<Fluid> variant) {
        Objects.requireNonNull(variant, "variant");
        return getHandlerOrDefault(variant.value());
    }
}
