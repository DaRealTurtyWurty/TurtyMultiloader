package dev.turtywurty.multiblocklib.data;

import net.minecraft.core.BlockPos;

import java.util.Set;

public record PortDefinition(BlockPos position, Set<String> types, PortIO io) {
}
