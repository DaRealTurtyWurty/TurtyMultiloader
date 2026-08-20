package dev.turtywurty.multiblocklib.pattern;

import com.google.gson.JsonObject;

public interface MultiblockPatternFactory {
    MultiblockPattern create(JsonObject json);
}
