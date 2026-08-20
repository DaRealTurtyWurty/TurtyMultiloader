package dev.turtywurty.multiblocklib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record MultiblockPartEntry(BlockPos offset, BlockState state) {
    public static final Codec<MultiblockPartEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("offset").forGetter(MultiblockPartEntry::offset),
        BlockState.CODEC.fieldOf("state").forGetter(MultiblockPartEntry::state)
    ).apply(instance, MultiblockPartEntry::new));
}
