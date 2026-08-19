package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.function.Supplier;

/** A queued custom registry and its vanilla registry key. */
public final class CustomRegistry<T> implements Supplier<Registry<T>> {
    private final RegistryService owner;
    private final ResourceKey<Registry<T>> key;
    private volatile Supplier<? extends Registry<T>> registry;

    public CustomRegistry(RegistryService owner, ResourceKey<Registry<T>> key) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.key = Objects.requireNonNull(key, "key");
    }

    public ResourceKey<Registry<T>> key() {
        return key;
    }

    public <V extends T> RegistrationHandle<T, V> register(Identifier id, Supplier<? extends V> factory) {
        return owner.register(key, id, factory);
    }

    @Override
    public Registry<T> get() {
        Supplier<? extends Registry<T>> current = registry;
        if (current == null)
            throw new IllegalStateException("Custom registry " + key.identifier() + " has not been applied yet");

        return current.get();
    }

    /** Binds this custom registry. Intended for registration backends. */
    public synchronized void bind(Supplier<? extends Registry<T>> registry) {
        if (this.registry != null)
            throw new IllegalStateException("Custom registry " + key.identifier() + " is already bound");

        this.registry = Objects.requireNonNull(registry, "registry");
    }
}
