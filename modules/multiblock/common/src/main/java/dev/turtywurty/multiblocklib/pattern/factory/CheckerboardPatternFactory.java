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

public class CheckerboardPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int size = GsonHelper.getAsInt(json, "size", 3);
        int width = GsonHelper.getAsInt(json, "width", size);
        int height = GsonHelper.getAsInt(json, "height", size);
        int depth = GsonHelper.getAsInt(json, "depth", size);
        boolean invert = GsonHelper.getAsBoolean(json, "invert", false);
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Checkerboard dimensions must be >= 1");
        }

        JsonElement a = readMatcherElement(json, "blocks_a", "block_a");
        JsonElement b = readMatcherElement(json, "blocks_b", "block_b");
        if (a == null || a.isJsonNull() || b == null || b.isJsonNull()) {
            throw new IllegalArgumentException("Checkerboard pattern requires both 'block_a(s)' and 'block_b(s)'");
        }
        BlockMatcherList matcherA = BlockMatchers.parseMatcherList(a);
        BlockMatcherList matcherB = BlockMatchers.parseMatcherList(b);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    boolean useA = ((x + y + z) & 1) == 0;
                    if (invert) {
                        useA = !useA;
                    }
                    matchers.put(new BlockPos(x, y, z), useA ? matcherA : matcherB);
                }
            }
        }

        return new GridPattern(new BlockPos(width, height, depth), matchers);
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
