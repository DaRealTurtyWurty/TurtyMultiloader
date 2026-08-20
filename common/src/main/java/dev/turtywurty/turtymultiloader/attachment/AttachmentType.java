package dev.turtywurty.turtymultiloader.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Loader-neutral declaration of an attachment and its lifecycle policies.
 */
public final class AttachmentType<T> {
    private final Identifier id;
    private final Supplier<? extends T> defaultFactory;
    private final Codec<T> persistenceCodec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
    private final AttachmentSyncPredicate syncPredicate;
    private final boolean copyOnDeath;

    public AttachmentType(Identifier id, Builder<T> builder) {
        this.id = Objects.requireNonNull(id, "id");
        this.defaultFactory = builder.defaultFactory;
        this.persistenceCodec = builder.persistenceCodec;
        this.streamCodec = builder.streamCodec;
        this.syncPredicate = builder.syncPredicate;
        this.copyOnDeath = builder.copyOnDeath;

        if (copyOnDeath && persistenceCodec == null)
            throw new IllegalStateException("copyOnDeath requires a persistence codec for " + id);
        if ((streamCodec == null) != (syncPredicate == null))
            throw new IllegalStateException("A stream codec and sync predicate must be configured together for " + id);
    }

    public Identifier id() {
        return id;
    }

    public Optional<Supplier<? extends T>> defaultFactory() {
        return Optional.ofNullable(defaultFactory);
    }

    public Optional<Codec<T>> persistenceCodec() {
        return Optional.ofNullable(persistenceCodec);
    }

    public Optional<StreamCodec<? super RegistryFriendlyByteBuf, T>> streamCodec() {
        return Optional.ofNullable(streamCodec);
    }

    public Optional<AttachmentSyncPredicate> syncPredicate() {
        return Optional.ofNullable(syncPredicate);
    }

    public boolean isPersistent() {
        return persistenceCodec != null;
    }

    public boolean isSynchronized() {
        return streamCodec != null;
    }

    public boolean copyOnDeath() {
        return copyOnDeath;
    }

    public T createDefault() {
        if (defaultFactory == null)
            throw new IllegalStateException("Attachment " + id + " has no default factory");
        return Objects.requireNonNull(defaultFactory.get(), "Default factory returned null for " + id);
    }

    public static final class Builder<T> {
        private Supplier<? extends T> defaultFactory;
        private Codec<T> persistenceCodec;
        private StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
        private AttachmentSyncPredicate syncPredicate;
        private boolean copyOnDeath;

        public Builder() {
        }

        public Builder<T> defaultFactory(Supplier<? extends T> defaultFactory) {
            this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory");
            return this;
        }

        public Builder<T> persistent(Codec<T> codec) {
            this.persistenceCodec = Objects.requireNonNull(codec, "codec");
            return this;
        }

        public Builder<T> transientValue() {
            this.persistenceCodec = null;
            this.copyOnDeath = false;
            return this;
        }

        public Builder<T> syncWith(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            AttachmentSyncPredicate predicate
        ) {
            this.streamCodec = Objects.requireNonNull(codec, "codec");
            this.syncPredicate = Objects.requireNonNull(predicate, "predicate");
            return this;
        }

        public Builder<T> syncToOwner(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            return syncWith(codec, AttachmentSyncPredicate.owner());
        }

        public Builder<T> syncToTrackers(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            return syncWith(codec, AttachmentSyncPredicate.trackers());
        }

        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }
    }
}
