package dev.turtywurty.multiblocklib.match;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BlockMatchers {
    private BlockMatchers() {
    }

    public static BlockMatcherList parseMatcherList(final JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new BlockMatcherList(List.of(state -> true), List.of("*"));
        }

        List<BlockMatcher> matchers = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                String token = GsonHelper.convertToString(entry, "matcher");
                matchers.add(parseMatcher(token));
                tokens.add(token);
            }
        } else {
            String token = GsonHelper.convertToString(element, "matcher");
            matchers.add(parseMatcher(token));
            tokens.add(token);
        }

        return new BlockMatcherList(matchers, tokens);
    }

    public static BlockMatcher parseMatcher(final String token) {
        if (token.equals("*") || token.equalsIgnoreCase("any")) {
            return state -> true;
        }
        if (token.startsWith("#")) {
            Identifier tagId = Identifier.parse(token.substring(1));
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
            return state -> state.is(tag);
        }

        if (token.contains("[")) {
            BlockState state = parseBlockState(token);
            return candidate -> candidate.equals(state);
        }

        Identifier blockId = Identifier.parse(token);
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        return state -> state.is(block);
    }

    public static BlockState parseBlockState(final String token) {
        String idPart = token;
        String propsPart = null;
        int openIndex = token.indexOf('[');
        if (openIndex != -1 && token.endsWith("]")) {
            idPart = token.substring(0, openIndex);
            propsPart = token.substring(openIndex + 1, token.length() - 1);
        }

        Identifier blockId = Identifier.parse(idPart);
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        BlockState state = block.defaultBlockState();
        if (propsPart == null || propsPart.isBlank()) {
            return state;
        }

        StateDefinition<Block, BlockState> definition = block.getStateDefinition();
        String[] parts = propsPart.split(",");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Invalid blockstate property segment: " + part + " in " + token);
            }
            String key = kv[0].trim();
            String value = kv[1].trim();
            Property<?> property = definition.getProperty(key);
            if (property == null) {
                throw new IllegalArgumentException("Unknown property '" + key + "' for block " + blockId);
            }
            state = setProperty(state, property, value, token);
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(
        final BlockState state,
        final Property<T> property,
        final String value,
        final String token
    ) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "' in " + token);
        }

        return state.setValue(property, parsed.get());
    }
}
