package dev.turtywurty.turtymultiloader.neoforge;

import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import dev.turtywurty.turtymultiloader.attachment.AttachmentService;
import dev.turtywurty.turtymultiloader.attachment.AttachmentTarget;
import dev.turtywurty.turtymultiloader.attachment.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Function;
import java.util.function.Supplier;

public final class NeoForgeAttachmentService implements AttachmentService {
    private static volatile IEventBus modBus;

    private final Map<AttachmentType<?>, NativeTypes<?>> types =
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
        var holder = registrar.register(id.getPath(), () -> createNative(type, NeoForgeAttachmentService::wrap));
        registrar.register(bus);

        // NeoForge has no MinecraftServer attachment holder. Use a dedicated native type on the canonical global
        // persistence level so server-global and level values cannot alias even when they share a common type.
        Identifier serverId = Identifier.fromNamespaceAndPath(
            TurtyMultiloader.MOD_ID,
            "server_attachment/" + id.getNamespace() + "/" + id.getPath()
        );
        DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> serverRegistrar =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, serverId.getNamespace());
        var serverHolder = serverRegistrar.register(
            serverId.getPath(),
            () -> createNative(type, NeoForgeAttachmentService::wrapServer)
        );
        serverRegistrar.register(bus);
        types.put(type, new NativeTypes<>(holder, serverHolder));
        return type;
    }

    @Override
    public <T> Optional<T> get(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).getExistingDataOrNull(nativeType(target, type)));
    }

    @Override
    public <T> T getOrCreate(AttachmentTarget target, AttachmentType<T> type) {
        type.defaultFactory().orElseThrow(() ->
            new IllegalStateException("Attachment " + type.id() + " has no default factory"));
        return nativeTarget(target).getData(nativeType(target, type));
    }

    @Override
    public <T> Optional<T> set(AttachmentTarget target, AttachmentType<T> type, T value) {
        return Optional.ofNullable(nativeTarget(target).setData(
            nativeType(target, type),
            Objects.requireNonNull(value, "value")
        ));
    }

    @Override
    public <T> Optional<T> remove(AttachmentTarget target, AttachmentType<T> type) {
        return Optional.ofNullable(nativeTarget(target).removeData(nativeType(target, type)));
    }

    @Override
    public void markDirty(AttachmentTarget target, AttachmentType<?> type) {
        IAttachmentHolder holder = nativeTarget(target);
        holder.syncData(nativeType(target, type));
        switch (target.kind()) {
            case BLOCK_ENTITY -> ((BlockEntity) target.value()).setChanged();
            case CHUNK -> ((ChunkAccess) target.value()).markUnsaved();
            default -> {
            }
        }
    }

    private <T> net.neoforged.neoforge.attachment.AttachmentType<T> createNative(
        AttachmentType<T> type,
        Function<IAttachmentHolder, AttachmentTarget> targetWrapper
    ) {
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
            (holder, player) -> type.syncPredicate().orElseThrow().test(targetWrapper.apply(holder), player),
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

    private static AttachmentTarget wrapServer(IAttachmentHolder holder) {
        if (holder instanceof ServerLevel level)
            return AttachmentTarget.server(level.getServer());
        throw new IllegalArgumentException("Server attachment must be stored by a server level, got " + holder.getClass());
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> net.neoforged.neoforge.attachment.AttachmentType<T> nativeType(
        AttachmentTarget target,
        AttachmentType<T> type
    ) {
        NativeTypes<?> nativeTypes = types.get(Objects.requireNonNull(type, "type"));
        if (nativeTypes == null)
            throw new IllegalArgumentException("Attachment type was not registered by this service: " + type.id());
        Supplier<? extends net.neoforged.neoforge.attachment.AttachmentType<?>> supplier =
            target.kind() == AttachmentTarget.Kind.SERVER ? nativeTypes.server() : nativeTypes.regular();
        return (net.neoforged.neoforge.attachment.AttachmentType<T>) supplier.get();
    }

    private record NativeTypes<T>(
        Supplier<? extends net.neoforged.neoforge.attachment.AttachmentType<T>> regular,
        Supplier<? extends net.neoforged.neoforge.attachment.AttachmentType<T>> server
    ) {
    }
}
