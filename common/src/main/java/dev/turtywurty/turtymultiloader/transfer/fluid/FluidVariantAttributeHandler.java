package dev.turtywurty.turtymultiloader.transfer.fluid;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Common, variant-aware overrides for fluid attributes.
 *
 * <p>Every default method delegates to the active loader's native attributes. Implementations may therefore
 * override only the values they own without replacing the other native properties.</p>
 */
public interface FluidVariantAttributeHandler {
    default Component getName(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getName(variant);
    }

    default Optional<SoundEvent> getFillSound(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getFillSound(variant);
    }

    default Optional<SoundEvent> getEmptySound(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getEmptySound(variant);
    }

    default int getLuminance(ResourceVariant<Fluid> variant) {
        return getLightEmission(variant);
    }

    /**
     * Fabric-compatible name for {@link #getLuminance(ResourceVariant)}.
     */
    default int getLightEmission(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getLuminance(variant);
    }

    /**
     * Returns the temperature in kelvin.
     */
    default int getTemperature(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getTemperature(variant);
    }

    /**
     * Returns viscosity in the conventional water-at-1000 scale. The level may be {@code null}.
     */
    default int getViscosity(ResourceVariant<Fluid> variant, @Nullable Level level) {
        return FluidAttributeService.get().getViscosity(variant, level);
    }

    default int getDensity(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().getDensity(variant);
    }

    default boolean isLighterThanAir(ResourceVariant<Fluid> variant) {
        return FluidAttributeService.get().isLighterThanAir(variant);
    }
}
