package dev.turtywurty.multiblocklib.pattern.factory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.multiblocklib.match.BlockMatcherList;
import dev.turtywurty.multiblocklib.match.BlockMatchers;
import dev.turtywurty.multiblocklib.pattern.GridPattern;
import dev.turtywurty.multiblocklib.pattern.MultiblockPattern;
import dev.turtywurty.multiblocklib.pattern.MultiblockPatternFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

public class StarShapePatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int radius = GsonHelper.getAsInt(json, "radius");
        int height = GsonHelper.getAsInt(json, "size", 1);
        if (radius < 0) {
            throw new IllegalArgumentException("Star radius must be >= 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Star size must be >= 1");
        }
        JsonElement blocks = json.get("blocks");
        if (blocks == null) {
            blocks = json.get("block");
        }

        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Star pattern requires 'block' or 'blocks' to be defined");
        }

        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean onAxis = x == 0 || z == 0;
                    boolean onDiagonal = Math.abs(x) == Math.abs(z);
                    if (onAxis || onDiagonal) {
                        minX = Math.min(minX, x);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxZ = Math.max(maxZ, z);
                        matchers.put(new BlockPos(x, y, z), matcher);
                    }
                }
            }
        }
        if (matchers.isEmpty()) {
            throw new IllegalArgumentException("Star pattern produced no blocks for radius " + radius + " and size " + height);
        }

        int sizeX = maxX - minX + 1;
        int sizeZ = maxZ - minZ + 1;
        Map<BlockPos, BlockMatcherList> shifted = new HashMap<>();
        for (Map.Entry<BlockPos, BlockMatcherList> entry : matchers.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockPos shiftedPos = new BlockPos(pos.getX() - minX, pos.getY(), pos.getZ() - minZ);
            shifted.put(shiftedPos, entry.getValue());
        }

        return new GridPattern(new BlockPos(sizeX, height, sizeZ), shifted);
    }
}
