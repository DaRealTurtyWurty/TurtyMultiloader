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

public class CylinderPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int radius = GsonHelper.getAsInt(json, "radius");
        int height = GsonHelper.getAsInt(json, "height", 1);
        int thickness = GsonHelper.getAsInt(json, "thickness", radius + 1);
        if (radius <= 0) {
            throw new IllegalArgumentException("Cylinder radius must be >= 1");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Cylinder height must be >= 1");
        }
        if (thickness <= 0) {
            throw new IllegalArgumentException("Cylinder thickness must be >= 1");
        }

        JsonElement blocks = readMatcherElement(json, "blocks", "block");
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Cylinder pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        int diameter = radius * 2 + 1;
        int innerRadius = Math.max(0, radius - thickness + 1);
        int outerSquared = radius * radius;
        int innerSquared = innerRadius * innerRadius;
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int distanceSquared = x * x + z * z;
                    if (distanceSquared <= outerSquared && distanceSquared >= innerSquared) {
                        matchers.put(new BlockPos(x + radius, y, z + radius), matcher);
                    }
                }
            }
        }

        return new GridPattern(new BlockPos(diameter, height, diameter), matchers);
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
