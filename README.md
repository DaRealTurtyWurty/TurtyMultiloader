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
data components, consume effects, position sources, world-generation features, creative tabs, payload types, wood
types, stripping, and flammability. `register(...)` accepts any vanilla `ResourceKey<? extends Registry<R>>`, and
`customRegistry(...)` creates custom registries whose entries use the same handles.

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
API, verify registries and holders, and exercise transactions, native capability lookup, caching/invalidation,
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
