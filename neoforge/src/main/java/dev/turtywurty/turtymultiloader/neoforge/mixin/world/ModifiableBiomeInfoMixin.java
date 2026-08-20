package dev.turtywurty.turtymultiloader.neoforge.mixin.world;

import dev.turtywurty.turtymultiloader.neoforge.NeoForgeWorldGenerationService;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = ModifiableBiomeInfo.class, remap = false)
public abstract class ModifiableBiomeInfoMixin {
    @ModifyVariable(method = "applyBiomeModifiers", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<BiomeModifier> turtymultiloader$appendCodeModifiers(
        List<BiomeModifier> modifiers,
        Holder<Biome> biome,
        List<BiomeModifier> originalModifiers,
        RegistryAccess registryAccess
    ) {
        return NeoForgeWorldGenerationService.appendCodeModifiers(modifiers, registryAccess);
    }
}
