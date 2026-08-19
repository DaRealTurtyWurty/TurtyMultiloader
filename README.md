# TurtyMultiloader

TurtyMultiloader is a Minecraft mod targeting Fabric and NeoForge from a shared codebase.

## Requirements

- Java 25
- Minecraft 26.1.1

## Project structure

- `common` contains code and resources shared by every loader.
- `fabric` contains the Fabric entry point and loader metadata.
- `neoforge` contains the NeoForge entry point and loader metadata.

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

### Initialization

Loader entrypoints call `CommonMod.init()`. Their client-only entrypoints call `CommonClient.init()`, so client initialization is never linked from a dedicated-server entrypoint. Both methods are idempotent.

Optional integrations are registered as lazy factories through `OptionalModIntegrations.register(...)` or `registerClient(...)`. A factory is instantiated only when its required mod is loaded. Register client integrations from client initialization code so their optional client API references are never linked on a dedicated server.

To run data generation for NeoForge:

```shell
./gradlew :neoforge:runData
```
