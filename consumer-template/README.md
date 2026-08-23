# TurtyMultiloader consumer template

Copy this directory into a new repository, change the identity values in `gradle.properties`, and replace the
`com.example.examplemod` package. The three projects deliberately use only public Maven coordinates and public loader
plugins; they do not depend on TurtyMultiloader's private `buildSrc` conventions.

Published-artifact mode uses `turtymultiloaderRepository` and `turtymultiloader_version`:

```shell
./gradlew check -PturtymultiloaderRepository=https://maven.example.invalid/releases
```

For local development, publish TurtyMultiloader to Maven Local:

```shell
./gradlew publishConsumerArtifactsToMavenLocal
```

Then add `mavenLocal()` to the consumer repositories. Local and remote builds use the same Maven coordinates.

The Fabric artifact is both an `implementation` dependency for development and an `include` dependency for the
consumer JAR. NeoForge uses `implementation` plus `jarJar` with an exact Maven version range. Shared code compiles
against the common artifacts. Both loader projects merge common Java/resources, generated resources, and the common
access widener/access transformer. The configured client datagen run is also the headless client-entrypoint smoke test.
