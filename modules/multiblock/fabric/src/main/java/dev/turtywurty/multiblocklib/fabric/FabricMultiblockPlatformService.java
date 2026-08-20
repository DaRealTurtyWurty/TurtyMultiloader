package dev.turtywurty.multiblocklib.fabric;

import dev.turtywurty.multiblocklib.platform.MultiblockPlatformService;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public final class FabricMultiblockPlatformService implements MultiblockPlatformService {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(
        BiFunction<BlockPos, BlockState, T> factory,
        Block... blocks
    ) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    @Override
    public void registerUseBlock(UseBlockHandler handler) {
        UseBlockCallback.EVENT.register(handler::use);
    }

    @Override
    public void registerServerReloadListener(Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener);
    }
}
