package dev.turtywurty.turtymultiloader.transfer.fluid;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loader bridge for the attributes supplied by Fabric or NeoForge for a fluid variant.
 * Mods should normally query {@link FluidVariantAttributes}, which applies common overlays first.
 */
public interface FluidAttributeService {
    static FluidAttributeService get() {
        return ServiceHolder.INSTANCE;
    }

    Component getName(ResourceVariant<Fluid> variant);

    Optional<SoundEvent> getFillSound(ResourceVariant<Fluid> variant);

    Optional<SoundEvent> getEmptySound(ResourceVariant<Fluid> variant);

    int getLuminance(ResourceVariant<Fluid> variant);

    int getTemperature(ResourceVariant<Fluid> variant);

    int getViscosity(ResourceVariant<Fluid> variant, @Nullable Level level);

    int getDensity(ResourceVariant<Fluid> variant);

    boolean isLighterThanAir(ResourceVariant<Fluid> variant);

    final class ServiceHolder {
        private static final FluidAttributeService INSTANCE = ServiceLoader.load(
                FluidAttributeService.class,
                FluidAttributeService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No fluid attribute service is available"));

        private ServiceHolder() {
        }
    }
}
