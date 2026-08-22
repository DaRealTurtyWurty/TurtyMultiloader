package dev.turtywurty.turtymultiloader.attachment;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Loader-neutral registration and storage backend for data attachments.
 */
public interface AttachmentService {
    static AttachmentService get() {
        return ServiceHolder.INSTANCE;
    }

    <T> AttachmentType<T> register(Identifier id, Consumer<AttachmentType.Builder<T>> configuration);

    <T> Optional<T> get(AttachmentTarget target, AttachmentType<T> type);

    <T> T getOrCreate(AttachmentTarget target, AttachmentType<T> type);

    default <T> T getOrCreate(AttachmentTarget target, AttachmentType<T> type, T defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        Optional<T> value = get(target, type);
        if (value.isPresent())
            return value.get();

        set(target, type, defaultValue);
        return defaultValue;
    }

    default <T> T getOrSupply(AttachmentTarget target, AttachmentType<T> type, Supplier<? extends T> defaultFactory) {
        Objects.requireNonNull(defaultFactory, "defaultFactory");
        Optional<T> value = get(target, type);
        if (value.isPresent())
            return value.get();

        T defaultValue = defaultFactory.get();
        Objects.requireNonNull(defaultValue, "defaultFactory result");
        set(target, type, defaultValue);
        return defaultValue;
    }

    <T> Optional<T> set(AttachmentTarget target, AttachmentType<T> type, T value);

    <T> Optional<T> remove(AttachmentTarget target, AttachmentType<T> type);

    /**
     * Announces an in-place mutation, marking persistent holders dirty and synchronizing the current value.
     */
    void markDirty(AttachmentTarget target, AttachmentType<?> type);

    default <T> T update(AttachmentTarget target, AttachmentType<T> type, UnaryOperator<T> update) {
        Objects.requireNonNull(update, "update");
        T value = update.apply(getOrCreate(target, type));
        set(target, type, Objects.requireNonNull(value, "update result"));
        return value;
    }

    default <T> T mutate(AttachmentTarget target, AttachmentType<T> type, Consumer<? super T> mutation) {
        Objects.requireNonNull(mutation, "mutation");
        T value = getOrCreate(target, type);
        mutation.accept(value);
        markDirty(target, type);
        return value;
    }

    final class ServiceHolder {
        private static final AttachmentService INSTANCE = ServiceLoader.load(
                AttachmentService.class,
                AttachmentService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No attachment service is available"));

        private ServiceHolder() {
        }
    }
}
