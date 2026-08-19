# TurtyMultiloader

TurtyMultiloader is a Minecraft mod targeting Fabric and NeoForge from a shared codebase.

## Requirements

- Java 25
- Minecraft 26.1.1

## Project structure

- `common` contains code and resources shared by every loader.
- `fabric` contains the Fabric entry point and loader metadata.
- `neoforge` contains the NeoForge entry point and loader metadata.
- `testmod/common`, `testmod/fabric`, and `testmod/neoforge` contain an isolated consumer mod and its GameTests.

Keep loader-specific APIs inside their corresponding module. Code in `common` must only depend on Minecraft and libraries available to every supported loader.

## Building

Run the Gradle wrapper from the repository root:

```shell
./gradlew build
```

The built JARs are written to each loader module's `build/libs` directory.

## Development

Import the repository as a Gradle project in IntelliJ IDEA and use Java 25 for both the project SDK and Gradle JVM. Gradle generates client and server run configurations for each loader.

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

Logical side requires a `Level` because a physical client can also host a logical server. The directory accessors return paths without creating them; `savesDirectory()` resolves to `saves` and `exportDirectory()` resolves to `exports` beneath the game directory.

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

### Initialization

Loader entrypoints call `CommonMod.init()`. Their client-only entrypoints call `CommonClient.init()`, so client initialization is never linked from a dedicated-server entrypoint. Both methods are idempotent.

Optional integrations are registered as lazy factories through `OptionalModIntegrations.register(...)` or `registerClient(...)`. A factory is instantiated only when its required mod is loaded. Register client integrations from client initialization code so their optional client API references are never linked on a dedicated server.

To run data generation for NeoForge:

```shell
./gradlew :neoforge:runData
```

## Test mod

The `testmod-fabric` and `testmod-neoforge` Gradle projects are real consumer mods with the separate mod ID
`turtymultiloader_testmod`. Their shared GameTest registers content through the public library API and verifies the
result against vanilla registries and holders in a running dedicated test server.

```shell
./gradlew :testmod-fabric:runGameTest
./gradlew :testmod-neoforge:runGameTest
```

The test projects depend on the loader library projects, never the reverse. They do not apply the publishing
conventions, and their source sets and metadata are not included in either library JAR.
