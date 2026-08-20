package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.attachment.AttachmentService;
import dev.turtywurty.turtymultiloader.attachment.AttachmentTarget;
import dev.turtywurty.turtymultiloader.attachment.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.GlobalAttachmentsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class FabricAttachmentService implements AttachmentService {
    private final Map<AttachmentType<?>, net.fabricmc.fabric.api.attachment.v1.AttachmentType<?>> types =
        new IdentityHashMap<>();

    @Override
    public synchronized <T> AttachmentType<T> register(
        Identifier id,
        Consumer<AttachmentType.Builder<T>> configuration
    ) {
        AttachmentType.Builder<T> builder = new AttachmentType.Builder<>();
        Objects.requireNonNull(configuration, "configuration").accept(builder);
        AttachmentType<T> type = new AttachmentType<>(id, builder);
        var nativeType = AttachmentRegistry.<T>create(id, nativeBuilder -> {
            type.defaultFactory().ifPresent(factory -> nativeBuilder.initializer(() -> type.createDefault()));
            type.persistenceCodec().ifPresent(nativeBuilder::persistent);
            if (type.copyOnDeath())
                nativeBuilder.copyOnDeath();
            type.streamCodec().ifPresent(codec -> nativeBuilder.syncWith(
                codec,
                (holder, player) -> type.syncPredicate().orElseThrow().test(wrap(holder, player), player)
            ));
        });
        types.put(type, nativeType);
        return type;
    }

    @Override
    public <T> Optional<T> get(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).getAttached(nativeType(type)));
    }

    @Override
    public <T> T getOrCreate(AttachmentTarget target, AttachmentType<T> type) {
        type.defaultFactory().orElseThrow(() ->
            new IllegalStateException("Attachment " + type.id() + " has no default factory"));
        return nativeTarget(target).getAttachedOrCreate(nativeType(type), type::createDefault);
    }

    @Override
    public <T> Optional<T> set(AttachmentTarget target, AttachmentType<T> type, T value) {
        return Optional.ofNullable(nativeTarget(target).setAttached(nativeType(type), Objects.requireNonNull(value, "value")));
    }

    @Override
    public <T> Optional<T> remove(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).removeAttached(nativeType(type)));
    }

    @Override
    public void markDirty(AttachmentTarget target, AttachmentType<?> type) {
        markDirtyTyped(target, type);
    }

    private <T> void markDirtyTyped(AttachmentTarget target, AttachmentType<T> type) {
        var holder = nativeTarget(target);
        T value = holder.getAttached(nativeType(type));
        if (value != null)
            holder.setAttached(nativeType(type), value);
    }

    private net.fabricmc.fabric.api.attachment.v1.AttachmentTarget nativeTarget(AttachmentTarget target) {
        if (target.kind() == AttachmentTarget.Kind.SERVER)
            return ((GlobalAttachmentsProvider) target.value()).globalAttachments();
        return (net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) target.value();
    }

    private static AttachmentTarget wrap(
        net.fabricmc.fabric.api.attachment.v1.AttachmentTarget holder,
        net.minecraft.server.level.ServerPlayer recipient
    ) {
        return switch (holder) {
            case Entity entity -> AttachmentTarget.entity(entity);
            case BlockEntity blockEntity -> AttachmentTarget.blockEntity(blockEntity);
            case ChunkAccess chunk -> AttachmentTarget.chunk(chunk);
            case Level level -> AttachmentTarget.level(level);
            // Fabric's server-global attachment holder is deliberately opaque. The synchronization callback always
            // has a server-side recipient, which gives us the owning server without retaining it in a global map.
            default -> {
                var server = ((ServerLevel) recipient.level()).getServer();
                Object globalHolder = ((GlobalAttachmentsProvider) server).globalAttachments();
                if (holder != globalHolder)
                    throw new IllegalArgumentException("Unsupported Fabric attachment target " + holder.getClass());
                yield AttachmentTarget.server(server);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> net.fabricmc.fabric.api.attachment.v1.AttachmentType<T> nativeType(AttachmentType<T> type) {
        var nativeType = types.get(Objects.requireNonNull(type, "type"));
        if (nativeType == null)
            throw new IllegalArgumentException("Attachment type was not registered by this service: " + type.id());
        return (net.fabricmc.fabric.api.attachment.v1.AttachmentType<T>) nativeType;
    }
}
