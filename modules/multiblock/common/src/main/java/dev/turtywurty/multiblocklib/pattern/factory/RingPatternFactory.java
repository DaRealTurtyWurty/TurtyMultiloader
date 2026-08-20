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

public class RingPatternFactory implements MultiblockPatternFactory {
    @Override
    public MultiblockPattern create(final JsonObject json) {
        int radius = GsonHelper.getAsInt(json, "radius");
        int thickness = GsonHelper.getAsInt(json, "thickness", 1);
        int height = GsonHelper.getAsInt(json, "size", 1);
        if (radius <= 0) {
            throw new IllegalArgumentException("Ring radius must be >= 1");
        }
        if (thickness <= 0) {
            throw new IllegalArgumentException("Ring thickness must be >= 1");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Ring size must be >= 1");
        }

        int outer = radius;
        int inner = Math.max(0, radius - thickness + 1);
        JsonElement blocks = json.get("blocks");
        if (blocks == null) {
            blocks = json.get("block");
        }
        if (blocks == null || blocks.isJsonNull()) {
            throw new IllegalArgumentException("Ring pattern requires 'block' or 'blocks' to be defined");
        }
        BlockMatcherList matcher = BlockMatchers.parseMatcherList(blocks);

        Map<BlockPos, BlockMatcherList> matchers = new HashMap<>();
        int diameter = radius * 2 + 1;
        for (int y = 0; y < height; y++) {
            for (int x = -outer; x <= outer; x++) {
                for (int z = -outer; z <= outer; z++) {
                    int distance = Math.max(Math.abs(x), Math.abs(z));
                    if (distance <= outer && distance >= inner) {
                        matchers.put(new BlockPos(x + outer, y, z + outer), matcher);
                    }
                }
            }
        }

        return new GridPattern(new BlockPos(diameter, height, diameter), matchers);
    }
}
