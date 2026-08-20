package dev.turtywurty.multiblocklib.pattern.factory;

import com.google.gson.JsonArray;
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

public class GridPatternFactory implements MultiblockPatternFactory {
    public static GridPattern parseGridPattern(final JsonObject json) {
        JsonArray layers = GsonHelper.getAsJsonArray(json, "layers");
        int height = layers.size();
        if (height == 0) {
            throw new IllegalArgumentException("Grid pattern must have at least one layer");
        }

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        int depth = -1;
        int width = -1;
        for (int y = 0; y < height; y++) {
            JsonArray layer = GsonHelper.convertToJsonArray(layers.get(y), "layer");
            if (depth == -1) {
                depth = layer.size();
            } else if (depth != layer.size()) {
                throw new IllegalArgumentException("All layers must have the same depth");
            }

            for (int z = 0; z < layer.size(); z++) {
                JsonArray row = GsonHelper.convertToJsonArray(layer.get(z), "row");
                if (width == -1) {
                    width = row.size();
                } else if (width != row.size()) {
                    throw new IllegalArgumentException("All rows must have the same width");
                }

                for (int x = 0; x < row.size(); x++) {
                    JsonElement entry = row.get(x);
                    if (entry == null || entry.isJsonNull()) {
                        continue;
                    }
                    BlockMatcherList matcher = BlockMatchers.parseMatcherList(entry);
                    matchers.put(new BlockPos(x, y, z), matcher);
                }
            }
        }

        return new GridPattern(new BlockPos(width, height, depth), matchers);
    }

    @Override
    public MultiblockPattern create(final JsonObject json) {
        return parseGridPattern(json);
    }
}
