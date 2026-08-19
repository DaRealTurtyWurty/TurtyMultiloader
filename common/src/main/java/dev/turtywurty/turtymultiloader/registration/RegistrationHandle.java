package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A reference to a queued registry entry.
 *
 * @param <R> the registry's element type
 * @param <T> the concrete registered type
 */
public final class RegistrationHandle<R, T extends R> implements Supplier<T> {
    private final Identifier id;
    private final ResourceKey<R> key;
    private volatile Holder<R> holder;
    private volatile Supplier<? extends T> value;

    public RegistrationHandle(Identifier id, ResourceKey<R> key) {
        this.id = Objects.requireNonNull(id, "id");
        this.key = Objects.requireNonNull(key, "key");
    }

    public Identifier id() {
        return id;
    }

    public ResourceKey<R> key() {
        return key;
    }

    public Holder<R> holder() {
        Holder<R> current = holder;
        if (current == null)
            throw new IllegalStateException("Registry entry " + id + " has not been applied yet");

        return current;
    }

    @Override
    public T get() {
        Supplier<? extends T> current = value;
        if (current == null)
            throw new IllegalStateException("Registry entry " + id + " has not been applied yet");

        return current.get();
    }

    /**
     * Binds this handle to a loader-owned holder. Intended for registration backends.
     */
    public synchronized void bind(Holder<R> holder, Supplier<? extends T> value) {
        if (this.holder != null)
            throw new IllegalStateException("Registry entry " + id + " is already bound");

        this.holder = Objects.requireNonNull(holder, "holder");
        this.value = Objects.requireNonNull(value, "value");
    }
}
