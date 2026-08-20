package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.neoforge.transfer.NeoForgeResourceAdapters;
import dev.turtywurty.turtymultiloader.transfer.fluid.FluidAttributeService;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class NeoForgeFluidAttributeService implements FluidAttributeService {
    @Override
    public Component getName(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return type(variant).getDescription(stack);
    }

    @Override
    public Optional<SoundEvent> getFillSound(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return Optional.ofNullable(type(variant).getSound(stack, SoundActions.BUCKET_FILL));
    }

    @Override
    public Optional<SoundEvent> getEmptySound(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return Optional.ofNullable(type(variant).getSound(stack, SoundActions.BUCKET_EMPTY));
    }

    @Override
    public int getLuminance(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return type(variant).getLightLevel(stack);
    }

    @Override
    public int getTemperature(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return type(variant).getTemperature(stack);
    }

    @Override
    public int getViscosity(ResourceVariant<Fluid> variant, @Nullable Level level) {
        FluidStack stack = stack(variant);
        return type(variant).getViscosity(stack);
    }

    @Override
    public int getDensity(ResourceVariant<Fluid> variant) {
        FluidStack stack = stack(variant);
        return type(variant).getDensity(stack);
    }

    @Override
    public boolean isLighterThanAir(ResourceVariant<Fluid> variant) {
        return type(variant).isLighterThanAir();
    }

    private static FluidStack stack(ResourceVariant<Fluid> variant) {
        return NeoForgeResourceAdapters.toNeoForgeFluid(variant).toStack(1);
    }

    private static FluidType type(ResourceVariant<Fluid> variant) {
        return NeoForgeResourceAdapters.toNeoForgeFluid(variant).getFluidType();
    }
}
