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

public class HollowBoxPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int size = GsonHelper.getAsInt(json, "size", 3);
        int width = GsonHelper.getAsInt(json, "width", size);
        int height = GsonHelper.getAsInt(json, "height", size);
        int depth = GsonHelper.getAsInt(json, "depth", size);
        int wallThickness = GsonHelper.getAsInt(json, "wall_thickness", 1);
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Hollow box dimensions must be >= 1");
        }
        if (wallThickness <= 0) {
            throw new IllegalArgumentException("Hollow box wall_thickness must be >= 1");
        }

        JsonElement defaultBlocks = readMatcherElement(json, "blocks", "block");
        JsonElement edgeBlocks = readMatcherElement(json, "edge_blocks", "edge_block");
        JsonElement faceBlocks = readMatcherElement(json, "face_blocks", "face_block");
        if ((defaultBlocks == null || defaultBlocks.isJsonNull())
            && (edgeBlocks == null || edgeBlocks.isJsonNull())
            && (faceBlocks == null || faceBlocks.isJsonNull())) {
            throw new IllegalArgumentException(
                "Hollow box pattern requires 'block(s)' or 'edge_block(s)'/'face_block(s)' to be defined"
            );
        }

        BlockMatcherList defaultMatcher = defaultBlocks != null && !defaultBlocks.isJsonNull()
            ? BlockMatchers.parseMatcherList(defaultBlocks)
            : null;
        BlockMatcherList edgeMatcher = edgeBlocks != null && !edgeBlocks.isJsonNull()
            ? BlockMatchers.parseMatcherList(edgeBlocks)
            : defaultMatcher;
        BlockMatcherList faceMatcher = faceBlocks != null && !faceBlocks.isJsonNull()
            ? BlockMatchers.parseMatcherList(faceBlocks)
            : defaultMatcher;

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    boolean onXFace = x < wallThickness || x >= width - wallThickness;
                    boolean onYFace = y < wallThickness || y >= height - wallThickness;
                    boolean onZFace = z < wallThickness || z >= depth - wallThickness;
                    int faceCount = (onXFace ? 1 : 0) + (onYFace ? 1 : 0) + (onZFace ? 1 : 0);
                    if (faceCount >= 2) {
                        matchers.put(new BlockPos(x, y, z), edgeMatcher);
                    } else if (faceCount == 1) {
                        matchers.put(new BlockPos(x, y, z), faceMatcher);
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
