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

public class LayeredPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        JsonArray layers = GsonHelper.getAsJsonArray(json, "layers");
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("Layered pattern requires at least one layer");
        }

        int size = GsonHelper.getAsInt(json, "size", 3);
        int width = GsonHelper.getAsInt(json, "width", size);
        int depth = GsonHelper.getAsInt(json, "depth", size);
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Layered pattern width/depth must be >= 1");
        }

        JsonElement defaultBlocks = readMatcherElement(json, "blocks", "block");
        BlockMatcherList defaultMatcher = defaultBlocks != null && !defaultBlocks.isJsonNull()
            ? BlockMatchers.parseMatcherList(defaultBlocks)
            : null;

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int y = 0; y < layers.size(); y++) {
            JsonElement layerElement = layers.get(y);
            BlockMatcherList layerMatcher;
            if (layerElement == null || layerElement.isJsonNull()) {
                layerMatcher = defaultMatcher;
            } else {
                layerMatcher = BlockMatchers.parseMatcherList(layerElement);
            }
            if (layerMatcher == null) {
                throw new IllegalArgumentException(
                    "Layered pattern layer " + y + " is null and no default 'block'/'blocks' is defined"
                );
            }

            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    matchers.put(new BlockPos(x, y, z), layerMatcher);
                }
            }
        }

        return new GridPattern(new BlockPos(width, layers.size(), depth), matchers);
    }

    private static JsonElement readMatcherElement(final JsonObject json, final String pluralKey, final String singularKey) {
        JsonElement matcher = json.get(pluralKey);
        if (matcher == null) {
            matcher = json.get(singularKey);
        }
        return matcher;
    }
}
