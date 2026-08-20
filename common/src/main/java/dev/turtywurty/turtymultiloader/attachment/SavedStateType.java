package dev.turtywurty.turtymultiloader.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A small typed wrapper around vanilla saved data. World state is stored per dimension; server state is stored in the
 * overworld data storage and is therefore shared by the whole server.
 */
public final class SavedStateType<T> {
    private final Scope scope;
    private final Supplier<? extends T> defaultFactory;
    private final SavedDataType<ValueData<T>> vanillaType;

    private SavedStateType(Scope scope, Identifier id, Codec<T> codec, Supplier<? extends T> defaultFactory) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");

        Codec<ValueData<T>> valueCodec = codec.fieldOf("value").codec().xmap(ValueData::new, ValueData::value);
        this.vanillaType = new SavedDataType<>(id, this::newDefaultData, valueCodec, null);
    }

    public static <T> SavedStateType<T> world(
        Identifier id,
        Codec<T> codec,
        Supplier<? extends T> defaultFactory
    ) {
        return new SavedStateType<>(Scope.WORLD, id, codec, defaultFactory);
    }

    public static <T> SavedStateType<T> server(
        Identifier id,
        Codec<T> codec,
        Supplier<? extends T> defaultFactory
    ) {
        return new SavedStateType<>(Scope.SERVER, id, codec, defaultFactory);
    }

    public Scope scope() {
        return scope;
    }

    public SavedState<T> access(ServerLevel level) {
        if (scope != Scope.WORLD)
            throw new IllegalStateException("Server-global saved state must be accessed through MinecraftServer");
        return new SavedState<>(Objects.requireNonNull(level, "level").getDataStorage().computeIfAbsent(vanillaType));
    }

    public SavedState<T> access(MinecraftServer server) {
        if (scope != Scope.SERVER)
            throw new IllegalStateException("World saved state must be accessed through ServerLevel");
        return new SavedState<>(Objects.requireNonNull(server, "server").overworld()
            .getDataStorage().computeIfAbsent(vanillaType));
    }

    private ValueData<T> newDefaultData() {
        return new ValueData<>(Objects.requireNonNull(defaultFactory.get(), "defaultFactory result"));
    }

    public enum Scope {
        WORLD,
        SERVER
    }

    public static final class SavedState<T> {
        private final ValueData<T> data;

        private SavedState(ValueData<T> data) {
            this.data = data;
        }

        public T get() {
            return data.value();
        }

        public void set(T value) {
            data.value = Objects.requireNonNull(value, "value");
            data.setDirty();
        }

        public T update(UnaryOperator<T> update) {
            T value = Objects.requireNonNull(update, "update").apply(get());
            set(Objects.requireNonNull(value, "update result"));
            return value;
        }

        public T mutate(Consumer<? super T> mutation) {
            Objects.requireNonNull(mutation, "mutation").accept(get());
            data.setDirty();
            return get();
        }

        public void markDirty() {
            data.setDirty();
        }
    }

    private static final class ValueData<T> extends SavedData {
        private T value;

        private ValueData(T value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        private T value() {
            return value;
        }
    }
}
