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

public class PlusPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int armLength = GsonHelper.getAsInt(json, "arm_length");
        int thickness = GsonHelper.getAsInt(json, "thickness", 1);
        if (armLength <= 0) {
            throw new IllegalArgumentException("Plus arm_length must be >= 1");
        }
        if (thickness <= 0) {
            throw new IllegalArgumentException("Plus thickness must be >= 1");
        }

        JsonElement blocks = readMatcherElement(json, "blocks", "block");
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Plus pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        int halfNeg = thickness / 2;
        int halfPos = (thickness - 1) / 2;
        int extent = armLength + Math.max(halfNeg, halfPos);
        int center = extent;

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int x = -extent; x <= extent; x++) {
            for (int y = -extent; y <= extent; y++) {
                for (int z = -extent; z <= extent; z++) {
                    boolean inXRod = inRange(x, -armLength, armLength)
                        && inRange(y, -halfNeg, halfPos)
                        && inRange(z, -halfNeg, halfPos);
                    boolean inYRod = inRange(y, -armLength, armLength)
                        && inRange(x, -halfNeg, halfPos)
                        && inRange(z, -halfNeg, halfPos);
                    boolean inZRod = inRange(z, -armLength, armLength)
                        && inRange(x, -halfNeg, halfPos)
                        && inRange(y, -halfNeg, halfPos);
                    if (inXRod || inYRod || inZRod) {
                        matchers.put(new BlockPos(x + center, y + center, z + center), matcher);
                    }
                }
            }
        }

        int size = extent * 2 + 1;
        return new GridPattern(new BlockPos(size, size, size), matchers);
    }

    private static boolean inRange(final int value, final int min, final int max) {
        return value >= min && value <= max;
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
