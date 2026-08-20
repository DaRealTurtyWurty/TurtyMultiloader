package dev.turtywurty.turtymultiloader.config;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Loader-neutral typed schema and lifecycle policy for one config file.
 */
public final class ConfigurationSpec<T> {
    private final Identifier id;
    private final ConfigScope scope;
    private final Codec<T> codec;
    private final Supplier<? extends T> defaultFactory;
    private final ConfigPath path;
    private final ConfigValidator<? super T> validator;
    private final BiConsumer<? super T, ConfigLifecycle> listener;

    private ConfigurationSpec(Builder<T> builder) {
        this.id = builder.id;
        this.scope = builder.scope;
        this.codec = builder.codec;
        this.defaultFactory = builder.defaultFactory;
        this.path = builder.path;
        this.validator = builder.validator;
        this.listener = builder.listener;
    }

    public static <T> Builder<T> builder(
        Identifier id,
        ConfigScope scope,
        Codec<T> codec,
        Supplier<? extends T> defaultFactory
    ) {
        return new Builder<>(id, scope, codec, defaultFactory);
    }

    public Identifier id() {
        return id;
    }

    public ConfigScope scope() {
        return scope;
    }

    public Codec<T> codec() {
        return codec;
    }

    public Supplier<? extends T> defaultFactory() {
        return defaultFactory;
    }

    public ConfigPath path() {
        return path;
    }

    public ConfigValidator<? super T> validator() {
        return validator;
    }

    public BiConsumer<? super T, ConfigLifecycle> listener() {
        return listener;
    }

    public static final class Builder<T> {
        private final Identifier id;
        private final ConfigScope scope;
        private final Codec<T> codec;
        private final Supplier<? extends T> defaultFactory;
        private ConfigPath path = ConfigPaths.defaults();
        private ConfigValidator<? super T> validator = ConfigValidator.none();
        private BiConsumer<? super T, ConfigLifecycle> listener = (value, lifecycle) -> {
        };

        private Builder(Identifier id, ConfigScope scope, Codec<T> codec, Supplier<? extends T> defaultFactory) {
            this.id = Objects.requireNonNull(id, "id");
            this.scope = Objects.requireNonNull(scope, "scope");
            this.codec = Objects.requireNonNull(codec, "codec");
            this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory");
        }

        public Builder<T> path(ConfigPath path) {
            this.path = Objects.requireNonNull(path, "path");
            return this;
        }

        public Builder<T> validator(ConfigValidator<? super T> validator) {
            this.validator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        public Builder<T> onChange(BiConsumer<? super T, ConfigLifecycle> listener) {
            this.listener = Objects.requireNonNull(listener, "listener");
            return this;
        }

        public ConfigurationSpec<T> build() {
            return new ConfigurationSpec<>(this);
        }
    }
}
