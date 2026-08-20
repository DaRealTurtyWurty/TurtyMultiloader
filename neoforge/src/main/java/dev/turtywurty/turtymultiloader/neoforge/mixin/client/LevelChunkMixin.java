package dev.turtywurty.turtymultiloader.neoforge.mixin.client;

import dev.turtywurty.turtymultiloader.neoforge.NeoForgeClientEventService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Shadow
    public abstract Level getLevel();

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void turtymultiloader$beforeSetBlockEntity(BlockEntity blockEntity, CallbackInfo callbackInfo) {
        if (!(getLevel() instanceof ClientLevel level))
            return;
        BlockEntity previous = ((LevelChunk) (Object) this).getBlockEntities().get(blockEntity.getBlockPos());
        if (previous != null && previous != blockEntity)
            NeoForgeClientEventService.fireBlockEntityUnload(previous, level);
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void turtymultiloader$beforeRemoveBlockEntity(BlockPos pos, CallbackInfo callbackInfo) {
        if (!(getLevel() instanceof ClientLevel level))
            return;
        BlockEntity blockEntity = ((LevelChunk) (Object) this).getBlockEntities().get(pos);
        if (blockEntity != null)
            NeoForgeClientEventService.fireBlockEntityUnload(blockEntity, level);
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void turtymultiloader$beforeClearBlockEntities(CallbackInfo callbackInfo) {
        if (!(getLevel() instanceof ClientLevel level))
            return;
        List.copyOf(((LevelChunk) (Object) this).getBlockEntities().values())
            .forEach(blockEntity -> NeoForgeClientEventService.fireBlockEntityUnload(blockEntity, level));
    }
}
