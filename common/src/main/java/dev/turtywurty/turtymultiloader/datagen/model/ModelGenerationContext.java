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

    public void blockModel(Identifier modelId, JsonElement json) {
        add(ModelResourceType.BLOCK_MODEL, modelId, json);
    }

    public void itemModel(Identifier modelId, JsonElement json) {
        add(ModelResourceType.ITEM_MODEL, modelId, json);
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
