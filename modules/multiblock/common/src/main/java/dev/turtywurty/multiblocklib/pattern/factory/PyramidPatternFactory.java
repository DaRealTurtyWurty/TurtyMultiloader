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

public class PyramidPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int baseRadius = GsonHelper.getAsInt(json, "base_radius");
        int topRadius = GsonHelper.getAsInt(json, "top_radius", 0);
        int defaultHeight = baseRadius - topRadius + 1;
        int height = GsonHelper.getAsInt(json, "height", defaultHeight);
        if (baseRadius < 0) {
            throw new IllegalArgumentException("Pyramid base_radius must be >= 0");
        }
        if (topRadius < 0) {
            throw new IllegalArgumentException("Pyramid top_radius must be >= 0");
        }
        if (topRadius > baseRadius) {
            throw new IllegalArgumentException("Pyramid top_radius must be <= base_radius");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Pyramid height must be >= 1");
        }

        JsonElement blocks = readMatcherElement(json, "blocks", "block");
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Pyramid pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        int width = baseRadius * 2 + 1;
        int center = baseRadius;
        int delta = baseRadius - topRadius;
        for (int y = 0; y < height; y++) {
            int shrink = height == 1 ? 0 : (delta * y) / (height - 1);
            int layerRadius = baseRadius - shrink;
            int min = center - layerRadius;
            int max = center + layerRadius;
            for (int z = min; z <= max; z++) {
                for (int x = min; x <= max; x++) {
                    matchers.put(new BlockPos(x, y, z), matcher);
                }
            }
        }

        return new GridPattern(new BlockPos(width, height, width), matchers);
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
