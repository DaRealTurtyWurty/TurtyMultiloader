package dev.turtywurty.turtymultiloader.mixin;

import dev.turtywurty.turtymultiloader.registration.VanillaRegistryHooks;
import net.minecraft.core.component.BlockTransformer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(BlockTransformer.class)
public abstract class BlockTransformerMixin {
    @Shadow
    @Final
    private List<BlockTransformer.BlockTransformData> transforms;

    @Redirect(method = "transformBlock*", at = @At(value = "FIELD", target = "Lnet/minecraft/core/component/BlockTransformer;transforms:Ljava/util/List;"))
    private List<BlockTransformer.BlockTransformData> turtymultiloader$includeStrippables(
        BlockTransformer transformer
    ) {
        return VanillaRegistryHooks.strippingTransforms(transforms);
    }
}
