package dev.turtywurty.turtymultiloader.client.registration;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Adds context-dependent baked models to a block's normal state model.
 *
 * <p>The loader wrapper renders the original model first, then invokes {@link #collectAdditionalModels}. Each emitted
 * model receives its own deterministic random seed. Implementations must be thread-safe because chunk meshes may be
 * built concurrently.</p>
 */
@FunctionalInterface
public interface BlockStateModelAugmenter {
    void collectAdditionalModels(Context context, ModelCollector collector);

    /**
     * Creates the cache key for the complete augmented geometry.
     *
     * <p>The default preserves the wrapped model's key. Override this whenever the selected additional models depend
     * on level state. Return {@code null} to disable geometry caching for the context.</p>
     */
    default @Nullable Object createGeometryKey(Context context, @Nullable Object wrappedKey) {
        return wrappedKey;
    }

    record Context(
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        long baseSeed
    ) {
        public Context {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(random, "random");
        }
    }

    @FunctionalInterface
    interface ModelCollector {
        void accept(BlockStateModel model, long randomSeed);
    }

    record SeededModel(BlockStateModel model, long randomSeed) {
        public SeededModel {
            Objects.requireNonNull(model, "model");
        }
    }
}
