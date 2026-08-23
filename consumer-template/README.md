# TurtyMultiloader consumer template

Copy this directory into a new repository, change the identity values in `gradle.properties`, and replace the
`com.example.examplemod` package. The three projects deliberately use only public Maven coordinates and public loader
plugins; they do not depend on TurtyMultiloader's private `buildSrc` conventions.

Published-artifact mode uses `turtymultiloaderRepository` and `turtymultiloader_version`:

```shell
./gradlew check -PturtymultiloaderRepository=https://maven.example.invalid/releases
```

For a local source checkout, keep the same dependencies and enable explicit composite substitution:

```shell
./gradlew check -PturtymultiloaderSource=../TurtyMultiloader
```

Plain `includeBuild("../TurtyMultiloader")` is insufficient because Gradle's default coordinates use source project
names such as `fabric`, whereas releases use artifact IDs such as `turtymultiloader-fabric`.
`settings.gradle` contains substitution rules for all core, gas, slurry, and multiblock modules.

The Fabric artifact is both an `implementation` dependency for development and an `include` dependency for the
consumer JAR. NeoForge uses `implementation` plus `jarJar` with an exact Maven version range. Shared code compiles
against the common artifacts. Both loader projects merge common Java/resources, generated resources, and the common
access widener/access transformer. The configured client datagen run is also the headless client-entrypoint smoke test.
