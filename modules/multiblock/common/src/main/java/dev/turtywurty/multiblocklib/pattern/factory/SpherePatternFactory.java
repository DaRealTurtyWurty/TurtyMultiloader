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

public class SpherePatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int radius = GsonHelper.getAsInt(json, "radius");
        boolean shell = GsonHelper.getAsBoolean(json, "shell", false);
        int thickness = GsonHelper.getAsInt(json, "thickness", 1);
        if (radius <= 0) {
            throw new IllegalArgumentException("Sphere radius must be >= 1");
        }
        if (thickness <= 0) {
            throw new IllegalArgumentException("Sphere thickness must be >= 1");
        }

        JsonElement blocks = readMatcherElement(json, "blocks", "block");
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Sphere pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        int diameter = radius * 2 + 1;
        int outerSquared = radius * radius;
        int innerRadius = shell ? Math.max(0, radius - thickness + 1) : 0;
        int innerSquared = innerRadius * innerRadius;

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                for (int x = -radius; x <= radius; x++) {
                    int distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared <= outerSquared && distanceSquared >= innerSquared) {
                        matchers.put(new BlockPos(x + radius, y + radius, z + radius), matcher);
                    }
                }
            }
        }

        return new GridPattern(new BlockPos(diameter, diameter, diameter), matchers);
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
