package dev.turtywurty.turtymultiloader.client.rendering;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * State-aware registry for domain renderers such as Industria conveyor renderers.
 *
 * <p>This class owns lookup and resource-reload behavior only. The caller decides which shared world-render stage
 * invokes a renderer and what its render context contains.</p>
 */
public final class SpecialBlockRendererRegistry<R extends ResourceManagerReloadListener>
    implements ResourceManagerReloadListener {
    private final Map<BlockState, Function<BlockState, ? extends R>> factories = new LinkedHashMap<>();
    private volatile Map<BlockState, R> renderers = Map.of();

    public synchronized void register(BlockState state, Function<BlockState, ? extends R> factory) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(factory, "factory");
        if (factories.putIfAbsent(state, factory) != null)
            throw new IllegalStateException("A special renderer is already registered for " + state);
    }

    public synchronized void register(Block block, Function<BlockState, ? extends R> factory) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(factory, "factory");
        block.getStateDefinition().getPossibleStates().forEach(state -> register(state, factory));
    }

    public void register(Block block, Supplier<? extends R> factory) {
        Objects.requireNonNull(factory, "factory");
        register(block, ignored -> factory.get());
    }

    public @Nullable R find(BlockState state) {
        return renderers.get(Objects.requireNonNull(state, "state"));
    }

    @Override
    public synchronized void onResourceManagerReload(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        Map<BlockState, R> rebuilt = new LinkedHashMap<>();
        Map<R, Boolean> reloaded = new IdentityHashMap<>();
        factories.forEach((state, factory) -> {
            R renderer = Objects.requireNonNull(factory.apply(state), "renderer factory result");
            if (reloaded.put(renderer, Boolean.TRUE) == null)
                renderer.onResourceManagerReload(resourceManager);
            rebuilt.put(state, renderer);
        });
        renderers = Map.copyOf(rebuilt);
    }
}
