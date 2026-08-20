# TurtyMultiloader

TurtyMultiloader is a Minecraft mod targeting Fabric and NeoForge from a shared codebase.

## Requirements

- Java 25
- Minecraft 26.1.1

## Project structure

- `common` contains code and resources shared by every loader.
- `fabric` contains the Fabric entry point and loader metadata.
- `neoforge` contains the NeoForge entry point and loader metadata.
- `modules/gas` is the optional, multiloader successor to `FabricGasApi`.
- `modules/slurry` is the optional, multiloader successor to `FabricSlurryApi`.
- `testmod/common`, `testmod/fabric`, and `testmod/neoforge` contain an isolated consumer mod and its GameTests.

Keep loader-specific APIs inside their corresponding module. Code in `common` must only depend on Minecraft and
libraries available to every supported loader.

## Building

Run the Gradle wrapper from the repository root:

```shell
./gradlew build
```

The built JARs are written to each loader module's `build/libs` directory.

## Development

Import the repository as a Gradle project in IntelliJ IDEA and use Java 25 for both the project SDK and Gradle JVM.
Gradle generates client and server run configurations for each loader.

### Platform service

Use `Platform` from common code for loader-specific environment information:

```java
Loader loader = Platform.loader();
PhysicalSide physicalSide = Platform.physicalSide();
LogicalSide logicalSide = Platform.logicalSide(level);
boolean development = Platform.isDevelopmentEnvironment();
boolean loaded = Platform.isModLoaded(modId);
Optional<String> version = Platform.modVersion(modId);

Path gameDirectory = Platform.gameDirectory();
Path configDirectory = Platform.configDirectory();
Path savesDirectory = Platform.savesDirectory();
Path exportDirectory = Platform.exportDirectory();
```

Logical side requires a `Level` because a physical client can also host a logical server. The directory accessors return
paths without creating them; `savesDirectory()` resolves to `saves` and `exportDirectory()` resolves to `exports`
beneath the game directory.

### Events and lifecycle

`Events` is the common callback facade. It covers server start/stop, level load/unload, server and level tick
boundaries,
player join/disconnect/respawn, successful player block breaks, post-damage/death, command registration, and successful
datapack reloads. Callback arguments are vanilla types, and observational callbacks do not expose either loader's event
objects:

```java
Events.onLevelLoad(level -> WorldPipeNetworks.getOrCreate(level));
Events.onStartLevelTick(level -> pipeNetworks(level).forEach(network -> network.tick(level)));
Events.onBlockBroken((level, player, pos, state, blockEntity) -> removeFluidPocket(level, pos));
Events.onLivingDamaged((entity, source, damageTaken) -> afterDamage(entity, source));
Events.onCommandRegistration(dispatcher -> dispatcher.register(createIndustriaCommand()));
```

Client callbacks live under `event.client` so dedicated-server initialization never resolves client classes. They cover
client and client-level ticks, client-level enter/leave, block-entity unload, play connection/disconnection, tooltip
construction, key mappings, resource reload listeners, and the two render stages used by Industria:

```java
ClientEvents.onTooltip((stack, context, flag, lines) -> addMobJarTooltip(stack, lines));
ClientEvents.onBlockEntityUnload((blockEntity, level) -> rendererCache.remove(blockEntity.getBlockPos()));

KeyMapping debugKey = ClientEvents.registerKeyMapping(new KeyMapping(
    "key.industria.toggle_debug_rendering",
    GLFW.GLFW_KEY_F6,
    INDUSTRIA_KEY_CATEGORY
));

ClientEvents.registerResourceReloadListener(id("conveyor_renderers"), reloadListener);
ClientEvents.onRenderStage(RenderStage.COLLECT_SUBMITS, conveyorRenderer::render);
ClientEvents.onRenderStage(RenderStage.AFTER_SOLID_FEATURES, debugRenderer::render);
```

`LevelRenderContext` contains only vanilla renderer objects: the client and level, game/level renderers, render state,
pose stack, buffer source, and the submit-node collector when that stage supplies one. Register common callbacks during
common initialization and client callbacks during client initialization. Registrations are process-lifetime callbacks;
the native Fabric and NeoForge event systems do not provide a shared unregister operation.

### Client registration and rendering

`ClientRegistrations` covers static client declarations whose implementations are vanilla types but whose registration
timing differs between Fabric and NeoForge. NeoForge declarations are queued for the appropriate mod-bus event; Fabric
applies them through its initialization registries. Register them from the consuming mod's client initializer:

```java
ClientRegistrations.registerEntityRenderer(RUBBER_BOAT, context ->
    new BoatRenderer(context, RUBBER_BOAT_LAYER));
ClientRegistrations.registerBlockEntityRenderer(CRUSHER_BLOCK_ENTITY, CrusherRenderer::new);
ClientRegistrations.registerModelLayer(CRUSHER_LAYER, CrusherModel::createLayer);

ClientRegistrations.registerBlockTintSources(List.of(RUBBER_LEAVES_TINT), RUBBER_LEAVES);
ClientRegistrations.registerFluidModel(
    new FluidModel.Unbaked(stillMaterial, flowingMaterial, overlayMaterial, tintSource),
    CRUDE_OIL_STILL,
    CRUDE_OIL_FLOWING
);
```

The same facade registers data-driven client extension codecs:

```java
ClientRegistrations.registerItemTintSource(id("heated"), HeatedTintSource.CODEC);
ClientRegistrations.registerItemModel(id("drill_head"), DrillHeadItemModel.Unbaked.CODEC);
ClientRegistrations.registerSpecialModelRenderer(id("block_entity_item"),
    IndustriaBlockEntityItemRenderer.Unbaked.CODEC);
```

Additional block-state models use a loader-neutral handle. Fabric backs it with an `ExtraModelKey`; NeoForge backs it
with a `StandaloneModelKey`:

```java
AdditionalModel<BlockStateModel> scannerModel =
    ClientRegistrations.registerAdditionalBlockStateModel(id("item/seismic_scanner_model"));

BlockStateModel baked = scannerModel.getOrThrow(); // only after model reload has completed
```

Screens remain under `ClientMenus`. Key mappings, tooltip callbacks, resource reload listeners, and world-render stages
remain under `ClientEvents`, since those are callback lifecycles rather than static renderer declarations.

For state-dependent domain renderers such as conveyors, `SpecialBlockRendererRegistry` provides block/state factory
registration, cached lookup, and reload handling. Register the registry itself with
`ClientEvents.registerResourceReloadListener(...)`, then query it from a `ClientEvents.onRenderStage(...)` callback.
The renderer interface and its domain render context stay in the consuming mod.

Custom model implementations are intentionally not bridged. Shared pipe connection/state calculation and geometry
data can live in common code, but Fabric's model-loading/renderer API and NeoForge's loader/geometry/baking pipeline
should have separate adapters. The service only bridges vanilla model layers, fluid models, data-driven item codecs,
and standalone model registration.

### Registry service

`RegistryService` is the loader-neutral registration API. Its methods queue declarations rather than registering
immediately, allowing NeoForge to attach `DeferredRegister` instances at the correct time. Fabric applies the same
declarations through its normal registries. The consuming mod's loader entrypoint calls `apply()` after declaring its
common content; its NeoForge entrypoint first binds the service to that mod's event bus.

Identifiers and vanilla registry concepts remain explicit:

```java
private static final RegistryService REGISTRIES = RegistryService.get();
private static final Identifier EXAMPLE_ID =
    Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "example");

public static final RegistrationHandle<Block, ExampleBlock> EXAMPLE_BLOCK =
    REGISTRIES.registerBlock(EXAMPLE_ID, ExampleBlock::new);
```

`RegistrationHandle` exposes `id()`, the vanilla entry `key()`, and the bound vanilla `holder()`. It also implements
`Supplier<T>` for constructors that depend on earlier declarations. Calling `get()` or `holder()` before registration
has been applied fails explicitly.

The service has typed helpers for blocks, items, fluids, block entities, entities and attributes, menus, recipes,
data components, consume effects, position sources, world-generation features, creative tabs, wood types, stripping,
and flammability. `register(...)` accepts any vanilla `ResourceKey<? extends Registry<R>>`, and
`customRegistry(...)` creates custom registries whose entries use the same handles.

### Menus and screens

`Menus` registers extended menu types without exposing Fabric's `ExtendedMenuType` or NeoForge's container factory.
The opening value can be any type with a vanilla `StreamCodec`; it does not need to be a packet payload. Menu and
screen implementations that otherwise use only vanilla classes can stay in common:

```java
public static final ExtendedMenuRegistration<CrusherMenu, BlockPos> CRUSHER_MENU = Menus.registerExtended(
    id("crusher"),
    CrusherMenu::new,
    BlockPos.STREAM_CODEC
);

// Client constructor used by the registration above.
public CrusherMenu(int containerId, Inventory inventory, BlockPos pos) {
    this(containerId, inventory, MenuOpeningData.requireBlockEntity(inventory, pos, CrusherBlockEntity.class));
}
```

An extended provider is also vanilla apart from the small common interface. Open it only on the logical server:

```java
public final class CrusherBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    @Override
    public BlockPos getMenuOpeningData(ServerPlayer player) {
        return getBlockPos();
    }

    // getDisplayName() and createMenu(...) are the normal vanilla MenuProvider methods.
}

Menus.open(serverPlayer, crusherBlockEntity, CRUSHER_MENU);
```

`Menus.open(player, provider)` covers ordinary vanilla menus. The overload accepting an explicit opening value is
useful when that value is not naturally owned by the provider. The library encodes the same declared codec through
Fabric's extended provider or NeoForge's additional-data writer.

Register screens from the consuming mod's client initializer. `MenuScreenFactory` has the same three arguments as
the vanilla constructor, while hiding loader registration timing:

```java
ClientMenus.register(CRUSHER_MENU, CrusherScreen::new);
```

For vanilla integer properties, `MenuDataSlots.builder()` creates a `ContainerData` view and includes boolean, enum,
float, lossless `long`, and lossless `double` mappings. Longs and doubles occupy two vanilla data slots:

```java
ContainerData data = MenuDataSlots.builder()
    .add(() -> progress, value -> progress = value)
    .addLong(() -> storedEnergy, value -> storedEnergy = value)
    .addEnum(Mode.class, () -> mode, value -> mode = value)
    .build();

// In the menu constructor, as usual:
addDataSlots(data);
```

Vanilla slot synchronization remains the preferred path for `ItemStack` slots. For state that cannot be represented
faithfully by vanilla slots or integer data slots, declare a typed menu channel during common initialization:

```java
MenuSyncChannel<CrusherMenu, MachineSnapshot> SNAPSHOT = MenuSyncChannel.register(
    id("crusher_snapshot"),
    CrusherMenu.class,
    MachineSnapshot.STREAM_CODEC
);
```

Attach its receiver during client initialization with `SNAPSHOT.registerClientReceiver(CrusherMenu::applySnapshot)`.
On the server, a `MenuSyncTracker` sends an initial value and subsequent changes, scoped by container ID so stale
packets cannot update a replacement menu. Call the tracker after `super.broadcastChanges()` in the menu. Use
`MenuSyncTracker.immutable(...)` for records and other immutable values, or `copying(...)` with a snapshot function
for mutable values. This path is intended for large counters, fluids/gases, recipe lists, or compound machine state;
normal item slots and small integer properties should continue to use vanilla synchronization.

### Networking service

`NetworkService` uses vanilla `CustomPacketPayload` records and `StreamCodec`s directly. It does not wrap buffers or
payload data. Declare each payload once in common initialization with its phase, direction, protocol version, and
required/optional policy:

```java
public record SetModePayload(BlockPos pos, int mode) implements CustomPacketPayload {
    public static final Type<SetModePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "set_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetModePayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetModePayload::pos,
        ByteBufCodecs.VAR_INT, SetModePayload::mode,
        SetModePayload::new
    );

    @Override
    public Type<SetModePayload> type() {
        return TYPE;
    }
}

private static final NetworkService NETWORK = NetworkService.get();

static {
    NETWORK.registerPlayServerbound(
        SetModePayload.TYPE,
        SetModePayload.CODEC,
        PayloadRegistrationOptions.required("2"),
        (payload, context) -> {
            ServerPlayer sender = context.sender().orElseThrow();
            if (!context.hasPermission(Commands.LEVEL_GAMEMASTERS))
                return;
            // Validate payload.pos()/mode() against sender before changing server state.
        }
    );
}
```

The six registration methods cover clientbound, serverbound, and bidirectional payloads in both play and
configuration phases. Attach the receiving side of a clientbound or bidirectional payload from the consuming mod's
client initializer; this keeps client implementation classes off dedicated servers:

```java
NETWORK.registerClientHandler(PayloadPhase.PLAY, SyncMachinePayload.TYPE, (payload, context) -> {
    // Runs on the render thread; context.player() contains the LocalPlayer.
});
```

Handlers run on the logical side's main game thread on both loaders. `PayloadContext` exposes the phase, receiving
side, player, authenticated serverbound sender, vanilla `PermissionSet`, reply/disconnect operations, and
`enqueueWork(...)`. Configuration handlers have no player or sender.

Play-phase send helpers cover the server, one player, every player, entity/chunk tracking players, one dimension, and
players near a `Vec3`. `canSendToServer(...)` and `canSend(...)` let optional features be gated explicitly. Required
payload or protocol mismatches disconnect during negotiation; optional mismatches remain connectable and sends are
skipped. NeoForge uses its native payload negotiation, while Fabric advertises an internal version-probe channel for
each vanilla payload type.

Lifecycle and initial synchronization are loader-neutral:

```java
NETWORK.onConnection(context -> preload(context.profile()));
NETWORK.onJoin(player -> initializeSession(player));
NETWORK.onDisconnect(player -> closeSession(player));
NETWORK.onClientJoin(ClientState::connected);
NETWORK.onClientDisconnect(ClientState::disconnected);

NETWORK.addLoginSync(player -> List.of(
    createMachineSnapshot(player),
    createNetworkSnapshot(player)
));
```

Login sync providers run in registration order after the server join callbacks. Unsupported optional payloads are
filtered per player. Networking declarations do not use `RegistryService.apply()`; Fabric registers them immediately
and NeoForge consumes them from `RegisterPayloadHandlersEvent`.

### Data attachments and saved state

`AttachmentService` hides Fabric and NeoForge attachment types and lifecycle hooks. Register attachment declarations
during common initialization, then use `AttachmentTarget` for entities, block entities, chunks, levels, or the whole
server:

```java
public static final AttachmentType<Integer> STOMACH_DESTRUCTION = Attachments.register(
    id("stomach_destruction"),
    builder -> builder.defaultFactory(() -> 0)
        .persistent(Codec.INT)
        .syncToOwner(ByteBufCodecs.INT)
        .copyOnDeath()
);

public static final AttachmentType<Map<BlockPos, MultiblockData>> MULTIBLOCKS = Attachments.register(
    id("multiblock"),
    builder -> builder.defaultFactory(HashMap::new)
        .persistent(Codec.unboundedMap(BLOCK_POS_STRING_CODEC, MultiblockData.CODEC))
        .syncToTrackers(ByteBufCodecs.map(
            HashMap::new,
            BlockPos.STREAM_CODEC,
            MultiblockData.STREAM_CODEC
        ))
);

AttachmentTarget target = AttachmentTarget.entity(entity);
int value = target.getOrCreate(STOMACH_DESTRUCTION);
target.set(STOMACH_DESTRUCTION, value + 1);
target.remove(STOMACH_DESTRUCTION);
```

Omit `persistent(...)` (or call `transientValue()`) for memory-only data. `syncWith(codec, predicate)` accepts a
custom `(target, player)` predicate; `syncToOwner(...)` and `syncToTrackers(...)` cover the usual policies. `set`,
`update`, and `remove` notify the native attachment system automatically. After changing a mutable value in place,
call `markDirty`, or use `mutate`, which marks block entities/chunks for saving and sends the updated value:

```java
AttachmentTarget.chunk(chunk).mutate(MULTIBLOCKS, map -> map.put(pos, data));
```

World and server-global state can also be represented explicitly with vanilla saved-data wrappers. World state uses
the selected dimension's data storage; server state uses the overworld data storage as the canonical global store:

```java
SavedStateType<MachineIndex> WORLD_INDEX = SavedStateType.world(
    id("machine_index"), MachineIndex.CODEC, MachineIndex::new
);
SavedStateType<ResearchState> GLOBAL_RESEARCH = SavedStateType.server(
    id("research"), ResearchState.CODEC, ResearchState::new
);

WORLD_INDEX.access(serverLevel).mutate(index -> index.add(machine));
GLOBAL_RESEARCH.access(server).update(ResearchState::advance);
```

The returned saved-state view supports `get`, `set`, `update`, `mutate`, and `markDirty`. Attachment registration is
immediate from common code and does not use `RegistryService.apply()`; NeoForge queues its native attachment registry
entries on the library's already-bound mod event bus.

### Resource transfer service

`TransferService` exposes storages without leaking loader API types into common code. A `ResourceVariant<T>` retains
its `ResourceType<T>`, vanilla `Holder<T>`, and `DataComponentPatch`; item and fluid variants therefore preserve data
components, while registered gas, slurry, and future chemical types use the same representation. `StorageKey<V>`
keeps the resource family and amount unit explicit. Standard keys are available from `StorageKeys`.

```java
private static final TransferService TRANSFERS = TransferService.get();
private static final SimpleSingleSlotStorage<ResourceVariant<Item>> BUFFER =
    new SimpleSingleSlotStorage<>(ResourceTypes.ITEM, 64);

static {
    TRANSFERS.registerBlockEntityProvider(
        StorageKeys.ITEM,
        EXAMPLE_BLOCK_ENTITY_TYPE,
        (blockEntity, side) -> blockEntity.itemStorage(side)
    );
}

ResourceStorage<ResourceVariant<Item>> target = TRANSFERS.findBlock(
    StorageKeys.ITEM, level, targetPos, targetSide
);
try (TransferTransaction transaction = TransferTransaction.openRoot()) {
    long inserted = target.insert(itemVariant, 16, transaction);
    if (inserted == 16)
        transaction.commit();
}
```

`ResourceStorage` is indexed and exposes per-index resource, amount, capacity, validity, and insertion/extraction
support.
`SingleSlotStorage`, `CombinedStorage`, and `SidedStorage` provide the common views. Root and nested transactions
support commit, rollback-on-close, simulation, snapshot participants, close callbacks, and final commit callbacks.
`StorageSnapshot`, `StorageCodecs`, and `StorageSynchronizer` cover persistence and synchronization.

Neutral fluid amounts use droplets (`81,000` per bucket), allowing exact conversion to Fabric units and NeoForge's
`1,000`-unit bucket convention. Core defines item, fluid, and energy units. Optional resource modules register their
own dimensions and units. All standard and mod-defined units are canonical entries in `Units.REGISTRY`:

```java
TransferUnit doubleEnergy = Units.REGISTRY.register(
    Identifier.fromNamespaceAndPath(MOD_ID, "double_energy"),
    UnitDimension.ENERGY,
    "2E",
    2
);
```

The registry provides identifier lookup, dimension queries, duplicate protection, and codecs that serialize units by
identifier.

### Optional gas and slurry modules

Gas and slurry do not live in the core artifacts. Each has common, Fabric, and NeoForge artifacts, so a mod only adds
the resource systems it uses:

```groovy
// Choose the matching dependency in each loader project.
implementation project(':gas-fabric')
implementation project(':gas-neoforge')
implementation project(':slurry-fabric')
implementation project(':slurry-neoforge')
```

The gas module is a loader-neutral rewrite of `FabricGasApi`; the slurry module is the corresponding rewrite of
`FabricSlurryApi`. They retain registered resource kinds, component-bearing variants, attributes, render handlers,
single/predicate/input/output storages, item-container storages, transfer helpers, and lookup registration. Their
public common APIs use TurtyMultiloader transactions and storages, never Fabric Transfer API or NeoForge capability
classes.

Each module owns its custom registry, resource family, storage key, codecs, unit dimension, and units:

```java
RegistrationHandle<Gas, Gas> STEAM = GasApi.register(id("steam"));
SingleGasStorage TANK = new SingleGasStorage(81_000);

GasStorage.registerBlockProvider(
    (level, pos, state, blockEntity, side) -> blockEntity.gasStorage(side),
    GAS_TANK_BLOCK
);

ResourceVariant<Gas> steam = GasVariant.of(STEAM.holder());
```

`GasApi.UNIT`/`GasApi.BUCKET` and `SlurryApi.UNIT`/`SlurryApi.BUCKET` are registered only when their module is on the
classpath. Both use `81,000` neutral units per bucket. Their providers interoperate through TurtyMultiloader's
generated Fabric lookups and NeoForge capabilities; integration with another mod's chemical API still requires a
dedicated adapter.

The Fabric module adapts its native `Storage`, `StorageView`, and `TransactionContext`, Team Reborn `EnergyStorage`,
block/item/entity API lookups, and mutable container-item contexts. The NeoForge module adapts `ResourceHandler`,
`EnergyHandler`, capabilities,
its native `TransactionContext`, and `ItemAccess`; custom resources use the same capability machinery. Provider
declarations are queued until `TransferService.apply()`. Fabric may call `apply()` again to flush declarations made by
a later consumer. NeoForge's library entrypoint owns its event-bus hookup and accepts declarations until the
capability-registration event fires; consuming mods do not bind the service to their own event bus.

### Initialization

Loader entrypoints call `CommonMod.init()`. Their client-only entrypoints call `CommonClient.init()`, so client
initialization is never linked from a dedicated-server entrypoint. Both methods are idempotent.

Optional integrations are registered as lazy factories through `OptionalModIntegrations.register(...)` or
`registerClient(...)`. A factory is instantiated only when its required mod is loaded. Register client integrations from
client initialization code so their optional client API references are never linked on a dedicated server.

To run data generation for NeoForge:

```shell
./gradlew :neoforge:runData
```

## Test mod

The `testmod-fabric` and `testmod-neoforge` Gradle projects are real consumer mods with the separate mod ID
`turtymultiloader_testmod`. Their shared GameTests register content and storage providers through the public library
API, compile and register every common/client event bridge, verify registries and holders, and exercise transactions,
serialization, sided/combined and indexed views, component-bearing item/fluid resources, optional gas/slurry module
resources and codecs, units,
post-apply provider declarations, and Team Reborn energy in a running dedicated test server. These projects produce
separate test-mod artifacts and are not packaged into either library artifact.

```shell
./gradlew :testmod-fabric:runGameTest
./gradlew :testmod-neoforge:runGameTest
```

The test projects depend on the loader library projects, never the reverse. They do not apply the publishing
conventions, and their source sets and metadata are not included in either library JAR.
