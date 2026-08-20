package dev.turtywurty.multiblocklib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public record PortInstance(BlockPos offset, Set<String> types, PortIO io) {
    public static final Codec<PortInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("offset").forGetter(PortInstance::offset),
        Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, list -> list.stream().toList())
            .fieldOf("types").forGetter(PortInstance::types),
        PortIO.CODEC.fieldOf("io").forGetter(PortInstance::io)
    ).apply(instance, PortInstance::new));
}
