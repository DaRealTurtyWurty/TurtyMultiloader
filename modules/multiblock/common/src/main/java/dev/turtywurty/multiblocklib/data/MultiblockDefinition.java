package dev.turtywurty.multiblocklib.data;

import dev.turtywurty.multiblocklib.pattern.MultiblockPattern;
import dev.turtywurty.multiblocklib.pattern.MultiblockRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;

public record MultiblockDefinition(
    Identifier id,
    Item triggerItem,
    Set<MultiblockRotation> allowedRotations,
    boolean allowMirroring,
    Identifier controllerBlockId,
    BlockPos controllerPos,
    MultiblockPattern pattern,
    List<PortDefinition> ports
) {
}
