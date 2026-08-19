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

To run data generation for NeoForge:

```shell
./gradlew :neoforge:runData
```
