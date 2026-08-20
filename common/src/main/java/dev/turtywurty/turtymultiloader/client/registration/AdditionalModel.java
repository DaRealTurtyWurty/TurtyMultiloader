package dev.turtywurty.turtymultiloader.client.registration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Loader-neutral handle for a standalone model that is not owned by a block state or item definition.
 *
 * <p>The model is only available after the client model manager has completed a resource reload.</p>
 */
public interface AdditionalModel<T> {
    Identifier id();

    @Nullable T get(ModelManager modelManager);

    default @Nullable T get() {
        return get(Minecraft.getInstance().getModelManager());
    }

    default T getOrThrow(ModelManager modelManager) {
        T model = get(Objects.requireNonNull(modelManager, "modelManager"));
        if (model == null)
            throw new IllegalStateException("Additional model is not available: " + id());
        return model;
    }

    default T getOrThrow() {
        return getOrThrow(Minecraft.getInstance().getModelManager());
    }
}
