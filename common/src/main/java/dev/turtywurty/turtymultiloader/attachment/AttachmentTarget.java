package dev.turtywurty.turtymultiloader.attachment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A typed common-code view of an object that can hold attachments.
 */
public record AttachmentTarget(Kind kind, Object value) {
    public AttachmentTarget {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
        if (!kind.type.isInstance(value))
            throw new IllegalArgumentException(kind + " attachment target must be a " + kind.type.getName());
    }

    public static AttachmentTarget entity(Entity entity) {
        return new AttachmentTarget(Kind.ENTITY, entity);
    }

    public static AttachmentTarget blockEntity(BlockEntity blockEntity) {
        return new AttachmentTarget(Kind.BLOCK_ENTITY, blockEntity);
    }

    public static AttachmentTarget chunk(ChunkAccess chunk) {
        return new AttachmentTarget(Kind.CHUNK, chunk);
    }

    public static AttachmentTarget level(Level level) {
        return new AttachmentTarget(Kind.LEVEL, level);
    }

    public static AttachmentTarget server(MinecraftServer server) {
        return new AttachmentTarget(Kind.SERVER, server);
    }

    public <T> Optional<T> get(AttachmentType<T> type) {
        return AttachmentService.get().get(this, type);
    }

    public <T> T getOrCreate(AttachmentType<T> type) {
        return AttachmentService.get().getOrCreate(this, type);
    }

    public <T> Optional<T> set(AttachmentType<T> type, T newValue) {
        return AttachmentService.get().set(this, type, newValue);
    }

    public <T> Optional<T> remove(AttachmentType<T> type) {
        return AttachmentService.get().remove(this, type);
    }

    public void markDirty(AttachmentType<?> type) {
        AttachmentService.get().markDirty(this, type);
    }

    public <T> T update(AttachmentType<T> type, UnaryOperator<T> update) {
        return AttachmentService.get().update(this, type, update);
    }

    public <T> T mutate(AttachmentType<T> type, Consumer<? super T> mutation) {
        return AttachmentService.get().mutate(this, type, mutation);
    }

    public enum Kind {
        ENTITY(Entity.class),
        BLOCK_ENTITY(BlockEntity.class),
        CHUNK(ChunkAccess.class),
        LEVEL(Level.class),
        SERVER(MinecraftServer.class);

        private final Class<?> type;

        Kind(Class<?> type) {
            this.type = type;
        }
    }
}
