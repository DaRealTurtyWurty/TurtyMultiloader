package dev.turtywurty.turtymultiloader.registration;

import java.util.Objects;
import java.util.function.Supplier;

/** A non-registry value whose construction is delayed until the registration batch is applied. */
public final class QueuedValue<T> implements Supplier<T> {
    private volatile T value;

    public QueuedValue() {
    }

    @Override
    public T get() {
        T current = value;
        if (current == null)
            throw new IllegalStateException("Queued value has not been applied yet");

        return current;
    }

    /** Binds this value. Intended for registration backends. */
    public synchronized void bind(T value) {
        if (this.value != null)
            throw new IllegalStateException("Queued value is already bound");

        this.value = Objects.requireNonNull(value, "value");
    }
}
