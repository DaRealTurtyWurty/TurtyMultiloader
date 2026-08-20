package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.attachment.AttachmentService;
import dev.turtywurty.turtymultiloader.attachment.AttachmentTarget;
import dev.turtywurty.turtymultiloader.attachment.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NeoForgeAttachmentService implements AttachmentService {
    private static volatile IEventBus modBus;

    private final Map<AttachmentType<?>, Supplier<? extends net.neoforged.neoforge.attachment.AttachmentType<?>>> types =
        new IdentityHashMap<>();

    public static synchronized void bind(IEventBus bus) {
        if (modBus != null && modBus != bus)
            throw new IllegalStateException("NeoForge attachment service is already bound to a mod event bus");
        modBus = Objects.requireNonNull(bus, "bus");
    }

    @Override
    public synchronized <T> AttachmentType<T> register(
        Identifier id,
        Consumer<AttachmentType.Builder<T>> configuration
    ) {
        IEventBus bus = modBus;
        if (bus == null)
            throw new IllegalStateException("NeoForge attachment service has not been bound to a mod event bus");

        AttachmentType.Builder<T> builder = new AttachmentType.Builder<>();
        Objects.requireNonNull(configuration, "configuration").accept(builder);
        AttachmentType<T> type = new AttachmentType<>(id, builder);

        DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> registrar =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, id.getNamespace());
        var holder = registrar.register(id.getPath(), () -> createNative(type));
        registrar.register(bus);
        types.put(type, holder);
        return type;
    }

    @Override
    public <T> Optional<T> get(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).getExistingDataOrNull(nativeType(type)));
    }

    @Override
    public <T> T getOrCreate(AttachmentTarget target, AttachmentType<T> type) {
        type.defaultFactory().orElseThrow(() ->
            new IllegalStateException("Attachment " + type.id() + " has no default factory"));
        return nativeTarget(target).getData(nativeType(type));
    }

    @Override
    public <T> Optional<T> set(AttachmentTarget target, AttachmentType<T> type, T value) {
        return Optional.ofNullable(nativeTarget(target).setData(nativeType(type), Objects.requireNonNull(value, "value")));
    }

    @Override
    public <T> Optional<T> remove(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).removeData(nativeType(type)));
    }

    @Override
    public void markDirty(AttachmentTarget target, AttachmentType<?> type) {
        IAttachmentHolder holder = nativeTarget(target);
        holder.syncData(nativeType(type));
        switch (target.kind()) {
            case BLOCK_ENTITY -> ((BlockEntity) target.value()).setChanged();
            case CHUNK -> ((ChunkAccess) target.value()).markUnsaved();
            default -> {
            }
        }
    }

    private <T> net.neoforged.neoforge.attachment.AttachmentType<T> createNative(AttachmentType<T> type) {
        Supplier<T> defaultFactory = type.defaultFactory()
            .<Supplier<T>>map(factory -> type::createDefault)
            .orElse(() -> {
                throw new IllegalStateException("Attachment " + type.id() + " has no default factory");
            });
        var builder = net.neoforged.neoforge.attachment.AttachmentType.builder(defaultFactory);
        type.persistenceCodec().ifPresent(codec -> builder.serialize(codec.fieldOf("value")));
        if (type.copyOnDeath())
            builder.copyOnDeath();
        type.streamCodec().ifPresent(codec -> builder.sync(
            (holder, player) -> type.syncPredicate().orElseThrow().test(wrap(holder), player),
            codec
        ));
        return builder.build();
    }

    private static IAttachmentHolder nativeTarget(AttachmentTarget target) {
        Object value = target.kind() == AttachmentTarget.Kind.SERVER
            ? ((MinecraftServer) target.value()).overworld()
            : target.value();
        return (IAttachmentHolder) value;
    }

    private static AttachmentTarget wrap(IAttachmentHolder holder) {
        return switch (holder) {
            case Entity entity -> AttachmentTarget.entity(entity);
            case BlockEntity blockEntity -> AttachmentTarget.blockEntity(blockEntity);
            case ChunkAccess chunk -> AttachmentTarget.chunk(chunk);
            case Level level -> AttachmentTarget.level(level);
            default ->
                throw new IllegalArgumentException("Unsupported NeoForge attachment target " + holder.getClass());
        };
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> net.neoforged.neoforge.attachment.AttachmentType<T> nativeType(AttachmentType<T> type) {
        Supplier<? extends net.neoforged.neoforge.attachment.AttachmentType<?>> supplier =
            types.get(Objects.requireNonNull(type, "type"));
        if (supplier == null)
            throw new IllegalArgumentException("Attachment type was not registered by this service: " + type.id());
        return (net.neoforged.neoforge.attachment.AttachmentType<T>) supplier.get();
    }
}
