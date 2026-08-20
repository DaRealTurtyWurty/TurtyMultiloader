package dev.turtywurty.turtymultiloader.datagen.model;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loader-neutral JSON model and blockstate declarations.
 */
public final class ModelGenerationContext {
    private final Map<ModelResource, JsonElement> resources = new LinkedHashMap<>();

    public void blockState(Identifier blockId, JsonElement json) {
        add(ModelResourceType.BLOCK_STATE, blockId, json);
    }

    /**
     * Adds a block model. The ID may be relative to the block model folder ({@code example}) or already use the
     * conventional models-relative path ({@code block/example}).
     */
    public void blockModel(Identifier modelId, JsonElement json) {
        add(ModelResourceType.BLOCK_MODEL, prefixed(modelId, "block/"), json);
    }

    /**
     * Adds an item model. The ID may be relative to the item model folder ({@code example}) or already use the
     * conventional models-relative path ({@code item/example}).
     */
    public void itemModel(Identifier modelId, JsonElement json) {
        add(ModelResourceType.ITEM_MODEL, prefixed(modelId, "item/"), json);
    }

    /**
     * Adds the modern item definition stored below {@code assets/<namespace>/items}.
     */
    public void itemDefinition(Identifier itemId, JsonElement json) {
        add(ModelResourceType.ITEM_DEFINITION, itemId, json);
    }

    public Map<ModelResource, JsonElement> resources() {
        return Map.copyOf(resources);
    }

    private void add(ModelResourceType type, Identifier id, JsonElement json) {
        ModelResource resource = new ModelResource(type, Objects.requireNonNull(id, "id"));
        if (resources.putIfAbsent(resource, Objects.requireNonNull(json, "json")) != null)
            throw new IllegalArgumentException("Duplicate generated model resource: " + resource);
    }

    private static Identifier prefixed(Identifier id, String prefix) {
        Objects.requireNonNull(id, "id");
        return id.getPath().startsWith(prefix)
            ? id
            : Identifier.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath());
    }

    public record ModelResource(ModelResourceType type, Identifier id) {
        public ModelResource {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }
    }

    public enum ModelResourceType {
        BLOCK_STATE,
        BLOCK_MODEL,
        ITEM_MODEL,
        ITEM_DEFINITION
    }
}
