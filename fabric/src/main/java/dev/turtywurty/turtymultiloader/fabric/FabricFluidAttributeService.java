package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.fabric.transfer.FabricResourceAdapters;
import dev.turtywurty.turtymultiloader.transfer.fluid.FluidAttributeService;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FabricFluidAttributeService implements FluidAttributeService {
    @Override
    public Component getName(ResourceVariant<Fluid> variant) {
        return FluidVariantAttributes.getName(nativeVariant(variant));
    }

    @Override
    public Optional<SoundEvent> getFillSound(ResourceVariant<Fluid> variant) {
        return Optional.of(FluidVariantAttributes.getFillSound(nativeVariant(variant)));
    }

    @Override
    public Optional<SoundEvent> getEmptySound(ResourceVariant<Fluid> variant) {
        return Optional.of(FluidVariantAttributes.getEmptySound(nativeVariant(variant)));
    }

    @Override
    public int getLuminance(ResourceVariant<Fluid> variant) {
        return FluidVariantAttributes.getLuminance(nativeVariant(variant));
    }

    @Override
    public int getTemperature(ResourceVariant<Fluid> variant) {
        return FluidVariantAttributes.getTemperature(nativeVariant(variant));
    }

    @Override
    public int getViscosity(ResourceVariant<Fluid> variant, @Nullable Level level) {
        return FluidVariantAttributes.getViscosity(nativeVariant(variant), level);
    }

    @Override
    public int getDensity(ResourceVariant<Fluid> variant) {
        return 1_000;
    }

    @Override
    public boolean isLighterThanAir(ResourceVariant<Fluid> variant) {
        return FluidVariantAttributes.isLighterThanAir(nativeVariant(variant));
    }

    private static FluidVariant nativeVariant(ResourceVariant<Fluid> variant) {
        return FabricResourceAdapters.toFabricFluid(variant);
    }
}
