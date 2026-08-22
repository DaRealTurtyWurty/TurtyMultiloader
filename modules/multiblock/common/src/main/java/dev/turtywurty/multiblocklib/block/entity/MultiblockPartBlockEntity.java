package dev.turtywurty.multiblocklib.block.entity;

import dev.turtywurty.multiblocklib.MultiblockLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MultiblockPartBlockEntity extends BlockEntity {
    private @Nullable BlockPos controllerPos;

    public MultiblockPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(MultiblockLib.MULTIBLOCK_PART_ENTITY_HANDLE.get(), pos, state);
    }

    public @Nullable BlockPos getControllerPos() {
        return this.controllerPos;
    }

    public void setControllerPos(final BlockPos controllerPos) {
        this.controllerPos = controllerPos.immutable();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(
                this.worldPosition,
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_ALL
            );
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        this.controllerPos = input.read("controller_pos", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(final @NonNull ValueOutput output) {
        if (this.controllerPos != null) {
            output.store("controller_pos", BlockPos.CODEC, this.controllerPos);
        }
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(final HolderLookup.@NonNull Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) ->
            this.saveCustomOnly(registries));
    }
}
