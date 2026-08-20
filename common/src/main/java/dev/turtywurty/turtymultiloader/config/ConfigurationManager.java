package dev.turtywurty.turtymultiloader.config;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import dev.turtywurty.turtymultiloader.TurtyMultiloader;
import dev.turtywurty.turtymultiloader.config.client.ConfigScreenFactory;
import dev.turtywurty.turtymultiloader.event.Events;
import dev.turtywurty.turtymultiloader.network.NetworkService;
import dev.turtywurty.turtymultiloader.network.PayloadPhase;
import dev.turtywurty.turtymultiloader.network.PayloadRegistrationOptions;
import dev.turtywurty.turtymultiloader.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Shared implementation used by both loader services.
 */
public final class ConfigurationManager {
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier SYNC_ID = Identifier.fromNamespaceAndPath(
        TurtyMultiloader.MOD_ID,
        "configuration_sync"
    );
    private static final CustomPacketPayload.Type<SyncPayload> SYNC_TYPE = new CustomPacketPayload.Type<>(SYNC_ID);
    private static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> SYNC_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        SyncPayload::configId,
        ByteBufCodecs.stringUtf8(1_048_576),
        SyncPayload::json,
        SyncPayload::new
    ).cast();

    private final Map<Identifier, Handle<?>> handles = new LinkedHashMap<>();
    private final Map<String, ConfigScreenFactory> screens = new LinkedHashMap<>();
    private boolean commonInitialized;
    private boolean clientInitialized;
    private MinecraftServer activeServer;

    private ConfigurationManager() {
    }

    public static ConfigurationManager instance() {
        return INSTANCE;
    }

    public synchronized <T> ConfigHandle<T> register(ConfigurationSpec<T> spec) {
        Objects.requireNonNull(spec, "spec");
        Handle<T> handle = new Handle<>(spec);
        if (handles.putIfAbsent(spec.id(), handle) != null)
            throw new IllegalStateException("Configuration " + spec.id() + " is already registered");

        if (shouldLoadNow(spec.scope())) {
            if (spec.scope() == ConfigScope.SERVER)
                handle.load(activeServer, ConfigLifecycle.LOAD);
            else
                handle.load(null, ConfigLifecycle.LOAD);
        }
        return handle;
    }

    public synchronized Optional<ConfigHandle<?>> find(Identifier id) {
        return Optional.ofNullable(handles.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized Collection<ConfigHandle<?>> all() {
        return List.copyOf(handles.values());
    }

    public synchronized void initializeCommon() {
        if (commonInitialized)
            return;
        commonInitialized = true;

        NetworkService network = NetworkService.get();
        network.registerPlayClientbound(SYNC_TYPE, SYNC_CODEC, PayloadRegistrationOptions.required("1"));
        network.addLoginSync(player -> synchronizedPayloads());
        Events.onServerStarting(this::serverStarting);
        Events.onServerStopping(this::serverStopping);
        Events.onCommandRegistration(dispatcher -> dispatcher.register(command()));

        handles.values().stream()
            .filter(handle -> handle.spec().scope() == ConfigScope.COMMON)
            .forEach(handle -> handle.load(null, ConfigLifecycle.LOAD));
    }

    public synchronized void initializeClient() {
        if (clientInitialized)
            return;
        clientInitialized = true;

        NetworkService network = NetworkService.get();
        network.registerClientHandler(PayloadPhase.PLAY, SYNC_TYPE, (payload, context) ->
            applySynchronization(payload)
        );
        network.onClientDisconnect(this::clientDisconnected);
        handles.values().stream()
            .filter(handle -> handle.spec().scope() == ConfigScope.CLIENT)
            .forEach(handle -> handle.load(null, ConfigLifecycle.LOAD));
    }

    public synchronized void registerScreen(String modId, ConfigScreenFactory factory) {
        if (screens.putIfAbsent(Objects.requireNonNull(modId, "modId"), Objects.requireNonNull(factory, "factory"))
            != null)
            throw new IllegalStateException("A config screen is already registered for " + modId);
    }

    public synchronized Optional<ConfigScreenFactory> screenFactory(String modId) {
        return Optional.ofNullable(screens.get(Objects.requireNonNull(modId, "modId")));
    }

    private boolean shouldLoadNow(ConfigScope scope) {
        return switch (scope) {
            case STARTUP -> true;
            case COMMON -> commonInitialized;
            case CLIENT -> clientInitialized && Platform.physicalSide().isClient();
            case SERVER -> activeServer != null;
        };
    }

    private synchronized void serverStarting(MinecraftServer server) {
        activeServer = Objects.requireNonNull(server, "server");
        handles.values().stream()
            .filter(handle -> handle.spec().scope() == ConfigScope.SERVER)
            .forEach(handle -> handle.load(server, ConfigLifecycle.LOAD));
    }

    private synchronized void serverStopping(MinecraftServer server) {
        handles.values().stream()
            .filter(handle -> handle.spec().scope() == ConfigScope.SERVER && handle.isLoaded())
            .forEach(handle -> handle.save(server));
        activeServer = null;
        clearServerConfigurations();
    }

    private synchronized void clientDisconnected() {
        // An integrated server owns this manager too. If its disconnect event runs before the server-stop event,
        // leave the value installed so serverStopping can persist it; serverStopping clears it afterwards.
        if (activeServer == null)
            clearServerConfigurations();
    }

    private void clearServerConfigurations() {
        handles.values().stream()
            .filter(handle -> handle.spec().scope() == ConfigScope.SERVER)
            .forEach(Handle::unload);
    }

    private synchronized List<SyncPayload> synchronizedPayloads() {
        List<SyncPayload> payloads = new ArrayList<>();
        for (Handle<?> handle : handles.values()) {
            if (handle.spec().scope().synchronizedToClient() && handle.isLoaded())
                payloads.add(handle.payload());
        }
        return payloads;
    }

    private synchronized void applySynchronization(SyncPayload payload) {
        Handle<?> handle = handles.get(payload.configId());
        if (handle == null) {
            TurtyMultiloader.LOGGER.warn("Received unknown synchronized config {}", payload.configId());
            return;
        }
        if (!handle.spec().scope().synchronizedToClient()) {
            TurtyMultiloader.LOGGER.warn("Ignoring synchronization for non-server config {}", payload.configId());
            return;
        }
        handle.install(JsonParser.parseString(payload.json()), ConfigLifecycle.SYNCHRONIZE);
    }

    private void synchronize(MinecraftServer server, Handle<?> handle) {
        if (handle.spec().scope().synchronizedToClient())
            NetworkService.get().sendToAll(server, handle.payload());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal(TurtyMultiloader.MOD_ID)
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
            .then(Commands.literal("config")
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("get")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> get(context.getSource(), id(context, "id"), null))
                        .then(Commands.argument("path", StringArgumentType.word())
                            .executes(context -> get(
                                context.getSource(),
                                id(context, "id"),
                                StringArgumentType.getString(context, "path")
                            )))))
                .then(Commands.literal("set")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("path", StringArgumentType.word())
                            .then(Commands.argument("json", StringArgumentType.greedyString())
                                .executes(context -> set(
                                    context.getSource(),
                                    id(context, "id"),
                                    StringArgumentType.getString(context, "path"),
                                    StringArgumentType.getString(context, "json")
                                ))))))
                .then(Commands.literal("reload")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> reload(context.getSource(), id(context, "id")))))
                .then(Commands.literal("save")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> save(context.getSource(), id(context, "id"))))));
    }

    private synchronized int list(CommandSourceStack source) {
        if (handles.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No configurations are registered"), false);
            return 0;
        }
        handles.values().forEach(handle -> source.sendSuccess(() -> Component.literal(
            handle.spec().id() + " [" + handle.spec().scope().name().toLowerCase() + "]"
        ), false));
        return handles.size();
    }

    private synchronized int get(CommandSourceStack source, Identifier id, String path) {
        Handle<?> handle = requireHandle(source, id);
        if (handle == null)
            return 0;
        try {
            JsonElement value = path == null ? handle.encode() : jsonAt(handle.encode(), path);
            source.sendSuccess(() -> Component.literal(GSON.toJson(value)), false);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private synchronized int set(CommandSourceStack source, Identifier id, String path, String json) {
        Handle<?> handle = requireHandle(source, id);
        if (handle == null)
            return 0;
        if (handle.spec().scope() == ConfigScope.CLIENT) {
            source.sendFailure(Component.literal("Client configs cannot be changed from a server command"));
            return 0;
        }
        try {
            JsonElement root = handle.encode().deepCopy();
            setJsonAt(root, path, JsonParser.parseString(json));
            handle.install(root, ConfigLifecycle.COMMAND);
            handle.save(source.getServer());
            synchronize(source.getServer(), handle);
            source.sendSuccess(() -> Component.literal("Updated and saved " + id + " at " + path), true);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private synchronized int reload(CommandSourceStack source, Identifier id) {
        Handle<?> handle = requireHandle(source, id);
        if (handle == null)
            return 0;
        if (handle.spec().scope() == ConfigScope.CLIENT) {
            source.sendFailure(Component.literal("Client configs cannot be reloaded from a server command"));
            return 0;
        }
        try {
            handle.reload(source.getServer());
            synchronize(source.getServer(), handle);
            source.sendSuccess(() -> Component.literal("Reloaded " + id), true);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private synchronized int save(CommandSourceStack source, Identifier id) {
        Handle<?> handle = requireHandle(source, id);
        if (handle == null)
            return 0;
        if (handle.spec().scope() == ConfigScope.CLIENT) {
            source.sendFailure(Component.literal("Client configs cannot be saved from a server command"));
            return 0;
        }
        try {
            handle.save(source.getServer());
            synchronize(source.getServer(), handle);
            source.sendSuccess(() -> Component.literal("Saved " + id), true);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private Handle<?> requireHandle(CommandSourceStack source, Identifier id) {
        Handle<?> handle = handles.get(id);
        if (handle == null)
            source.sendFailure(Component.literal("Unknown configuration: " + id));
        return handle;
    }

    private static Identifier id(CommandContext<CommandSourceStack> context, String name) {
        Identifier id = Identifier.tryParse(StringArgumentType.getString(context, name));
        if (id == null)
            throw new IllegalArgumentException("Invalid configuration identifier");
        return id;
    }

    private static JsonElement jsonAt(JsonElement root, String path) {
        JsonElement current = root;
        for (String part : parts(path)) {
            if (!current.isJsonObject() || !current.getAsJsonObject().has(part))
                throw new ConfigException("Unknown config path: " + path);
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    private static void setJsonAt(JsonElement root, String path, JsonElement value) {
        String[] parts = parts(path);
        if (parts.length == 0)
            throw new ConfigException("The root value cannot be replaced; specify a field path");
        JsonElement current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            if (!current.isJsonObject() || !current.getAsJsonObject().has(parts[index]))
                throw new ConfigException("Unknown config path: " + path);
            current = current.getAsJsonObject().get(parts[index]);
        }
        if (!current.isJsonObject())
            throw new ConfigException("Config path does not select an object: " + path);
        JsonObject object = current.getAsJsonObject();
        if (!object.has(parts[parts.length - 1]))
            throw new ConfigException("Unknown config path: " + path);
        object.add(parts[parts.length - 1], value);
    }

    private static String[] parts(String path) {
        if (path == null || path.isBlank())
            return new String[0];
        return path.split("\\.");
    }

    private final class Handle<T> implements ConfigHandle<T> {
        private final ConfigurationSpec<T> spec;
        private T value;
        private boolean loaded;
        private Path resolvedPath;

        private Handle(ConfigurationSpec<T> spec) {
            this.spec = spec;
            this.value = defaultValue();
        }

        @Override
        public ConfigurationSpec<T> spec() {
            return spec;
        }

        @Override
        public synchronized T value() {
            return value;
        }

        @Override
        public synchronized boolean isLoaded() {
            return loaded;
        }

        @Override
        public synchronized Optional<Path> path() {
            return Optional.ofNullable(resolvedPath);
        }

        @Override
        public Path path(MinecraftServer server) {
            return resolve(server);
        }

        @Override
        public synchronized JsonElement encode() {
            return spec.codec().encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        }

        @Override
        public void load() {
            load(null, ConfigLifecycle.LOAD);
        }

        @Override
        public void load(MinecraftServer server) {
            load(server, ConfigLifecycle.LOAD);
        }

        @Override
        public void reload() {
            load(null, ConfigLifecycle.RELOAD);
        }

        @Override
        public void reload(MinecraftServer server) {
            load(server, ConfigLifecycle.RELOAD);
        }

        private synchronized void load(MinecraftServer server, ConfigLifecycle lifecycle) {
            Path path = resolve(server);
            resolvedPath = path;
            if (Files.notExists(path)) {
                value = defaultValue();
                loaded = true;
                write(path);
                spec.listener().accept(value, lifecycle);
                return;
            }
            try {
                T decoded = decode(JsonParser.parseString(Files.readString(path)));
                value = decoded;
                loaded = true;
            } catch (Exception exception) {
                backup(path);
                value = defaultValue();
                loaded = true;
                write(path);
                TurtyMultiloader.LOGGER.error(
                    "Invalid config {} was backed up and replaced with defaults",
                    spec.id(),
                    exception
                );
            }
            spec.listener().accept(value, lifecycle);
        }

        @Override
        public void save() {
            save(null);
        }

        @Override
        public synchronized void save(MinecraftServer server) {
            Path path = resolve(server);
            resolvedPath = path;
            validate(value);
            write(path);
            loaded = true;
            spec.listener().accept(value, ConfigLifecycle.SAVE);
        }

        @Override
        public void synchronize(MinecraftServer server) {
            ConfigurationManager.this.synchronize(Objects.requireNonNull(server, "server"), this);
        }

        @Override
        public synchronized void set(T value) {
            validate(Objects.requireNonNull(value, "value"));
            this.value = value;
            loaded = true;
            spec.listener().accept(value, ConfigLifecycle.SET);
        }

        private synchronized void install(JsonElement json, ConfigLifecycle lifecycle) {
            T decoded = decode(json);
            value = decoded;
            loaded = true;
            spec.listener().accept(value, lifecycle);
        }

        private synchronized void unload() {
            if (!loaded && resolvedPath == null)
                return;
            value = defaultValue();
            loaded = false;
            resolvedPath = null;
            spec.listener().accept(value, ConfigLifecycle.UNLOAD);
        }

        private T decode(JsonElement json) {
            T decoded = spec.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
            validate(decoded);
            return decoded;
        }

        private T defaultValue() {
            T defaultValue = Objects.requireNonNull(spec.defaultFactory().get(), "defaultFactory result");
            validate(defaultValue);
            return defaultValue;
        }

        private void validate(T candidate) {
            List<String> errors = spec.validator().validate(candidate);
            if (errors == null)
                throw new ConfigException("Validator returned null for " + spec.id());
            if (!errors.isEmpty())
                throw new ConfigException("Invalid configuration " + spec.id() + ": " + String.join("; ", errors));
        }

        private Path resolve(MinecraftServer server) {
            Optional<MinecraftServer> context = Optional.ofNullable(server);
            if (spec.scope() == ConfigScope.SERVER && context.isEmpty())
                context = Optional.ofNullable(activeServer);
            return spec.path().resolve(spec.id(), spec.scope(), context).toAbsolutePath().normalize();
        }

        private void write(Path path) {
            try {
                Path parent = path.getParent();
                if (parent != null)
                    Files.createDirectories(parent);
                Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
                Files.writeString(temporary, GSON.toJson(encode()) + System.lineSeparator());
                try {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                throw new ConfigException("Failed to save configuration " + spec.id() + " to " + path, exception);
            }
        }

        private void backup(Path path) {
            if (Files.notExists(path))
                return;
            try {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                TurtyMultiloader.LOGGER.error("Failed to back up invalid config {}", spec.id(), exception);
            }
        }

        private SyncPayload payload() {
            return new SyncPayload(spec.id(), GSON.toJson(encode()));
        }
    }

    private record SyncPayload(Identifier configId, String json) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SYNC_TYPE;
        }
    }
}
