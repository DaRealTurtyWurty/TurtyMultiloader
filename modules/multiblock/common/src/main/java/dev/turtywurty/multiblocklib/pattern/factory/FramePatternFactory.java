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

public class FramePatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int size = GsonHelper.getAsInt(json, "size", 3);
        int width = GsonHelper.getAsInt(json, "width", size);
        int height = GsonHelper.getAsInt(json, "height", size);
        int depth = GsonHelper.getAsInt(json, "depth", size);
        int edgeThickness = GsonHelper.getAsInt(json, "edge_thickness", 1);
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Frame dimensions must be >= 1");
        }
        if (edgeThickness <= 0) {
            throw new IllegalArgumentException("Frame edge_thickness must be >= 1");
        }

        JsonElement blocks = readMatcherElement(json, "blocks", "block");
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Frame pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    boolean onX = x < edgeThickness || x >= width - edgeThickness;
                    boolean onY = y < edgeThickness || y >= height - edgeThickness;
                    boolean onZ = z < edgeThickness || z >= depth - edgeThickness;
                    int boundaryAxes = (onX ? 1 : 0) + (onY ? 1 : 0) + (onZ ? 1 : 0);
                    if (boundaryAxes >= 2) {
                        matchers.put(new BlockPos(x, y, z), matcher);
                    }
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
