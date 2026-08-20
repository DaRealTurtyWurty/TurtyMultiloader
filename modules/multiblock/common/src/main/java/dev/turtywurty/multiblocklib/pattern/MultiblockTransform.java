package dev.turtywurty.multiblocklib.pattern;

import net.minecraft.core.BlockPos;

public final class MultiblockTransform {
    private MultiblockTransform() {
    }

    public static BlockPos transform(final BlockPos pos, final BlockPos size, final MultiblockRotation rotation, final boolean mirrorX) {
        BlockPos mirrored = mirrorX ? new BlockPos(size.getX() - 1 - pos.getX(), pos.getY(), pos.getZ()) : pos;
        return switch (rotation) {
            case NONE -> mirrored;
            case CW_90 -> new BlockPos(size.getZ() - 1 - mirrored.getZ(), mirrored.getY(), mirrored.getX());
            case CW_180 ->
                new BlockPos(size.getX() - 1 - mirrored.getX(), mirrored.getY(), size.getZ() - 1 - mirrored.getZ());
            case CW_270 -> new BlockPos(mirrored.getZ(), mirrored.getY(), size.getX() - 1 - mirrored.getX());
        };
    }

    public static BlockPos transformedSize(final BlockPos size, final MultiblockRotation rotation) {
        return switch (rotation) {
            case NONE, CW_180 -> size;
            case CW_90, CW_270 -> new BlockPos(size.getZ(), size.getY(), size.getX());
        };
    }
}
