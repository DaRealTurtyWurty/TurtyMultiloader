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

public class CrossPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int radius = GsonHelper.getAsInt(json, "radius");
        int height = GsonHelper.getAsInt(json, "size", 1);
        if (radius < 0) {
            throw new IllegalArgumentException("Cross radius must be >= 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Cross size must be >= 1");
        }

        JsonElement blocks = json.get("blocks");
        if (blocks == null) {
            blocks = json.get("block");
        }
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Cross pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        int diameter = radius * 2 + 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < diameter; x++) {
                matchers.put(new BlockPos(x, y, radius), matcher);
            }
            for (int z = 0; z < diameter; z++) {
                matchers.put(new BlockPos(radius, y, z), matcher);
            }
        }

        return new GridPattern(new BlockPos(diameter, height, diameter), matchers);
    }
}
