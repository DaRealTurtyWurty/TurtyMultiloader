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
- `modules/multiblock` is the optional, multiloader port of `MultiblockLib`.
- `testmod/common`, `testmod/fabric`, and `testmod/neoforge` contain an isolated consumer mod and its GameTests.

Keep loader-specific APIs inside their corresponding module. Code in `common` must only depend on Minecraft and
libraries available to every supported loader.

## Building

Run the Gradle wrapper from the repository root:

```shell
./gradlew build
```

The built JARs are written to each loader module's `build/libs` directory.

`build` and `check` are release checks: they publish to the verification Maven repository, build the standalone
consumer, run both GameTest servers, and run Fabric plus NeoForge client/server datagen. The client datagen launches
also exercise physical-client entrypoints in a headless environment; graphical rendering still belongs in manual
client runs.

## Consumer setup

Releases use group `dev.turtywurty.turtymultiloader`, semantic versions such as `1.0.0`, and stable artifact IDs:

| Use        | Common                                      | Fabric                                      | NeoForge                                      |
|------------|---------------------------------------------|---------------------------------------------|-----------------------------------------------|
| Core       | `turtymultiloader-common`            | `turtymultiloader-fabric`            | `turtymultiloader-neoforge`            |
| Gas        | `turtymultiloader-gas-common`        | `turtymultiloader-gas-fabric`        | `turtymultiloader-gas-neoforge`        |
| Slurry     | `turtymultiloader-slurry-common`     | `turtymultiloader-slurry-fabric`     | `turtymultiloader-slurry-neoforge`     |
| Multiblock | `turtymultiloader-multiblock-common` | `turtymultiloader-multiblock-fabric` | `turtymultiloader-multiblock-neoforge` |

Add the Maven repository that contains the release to every consumer project. A local checkout publishes to
`build/local-maven` by default, so a sibling build can use
`maven { url = uri("../TurtyMultiloader/build/local-maven") }`.
Common code uses compile-only dependencies; each distributable loader JAR embeds the corresponding loader artifact:

```groovy
def tmlGroup = 'dev.turtywurty.turtymultiloader'
def tmlVersion = '1.0.0'
def tml = { name -> "${tmlGroup}:${name}:${tmlVersion}" }

// common/build.gradle
dependencies {
    compileOnly tml('turtymultiloader-common')
    compileOnly tml('turtymultiloader-gas-common')     // optional
    compileOnly tml('turtymultiloader-slurry-common') // optional
    compileOnly tml('turtymultiloader-multiblock-common') // optional
}

// fabric/build.gradle
dependencies {
    implementation tml('turtymultiloader-fabric')
    include tml('turtymultiloader-fabric')
    implementation tml('turtymultiloader-gas-fabric')
    include tml('turtymultiloader-gas-fabric')
    implementation tml('turtymultiloader-slurry-fabric')
    include tml('turtymultiloader-slurry-fabric')
    implementation tml('turtymultiloader-multiblock-fabric')
    include tml('turtymultiloader-multiblock-fabric')
}

// neoforge/build.gradle
dependencies {
    implementation tml('turtymultiloader-neoforge')
    jarJar "${tmlGroup}:turtymultiloader-neoforge:[${tmlVersion}]"
    implementation tml('turtymultiloader-gas-neoforge')
    jarJar "${tmlGroup}:turtymultiloader-gas-neoforge:[${tmlVersion}]"
    implementation tml('turtymultiloader-slurry-neoforge')
    jarJar "${tmlGroup}:turtymultiloader-slurry-neoforge:[${tmlVersion}]"
    implementation tml('turtymultiloader-multiblock-neoforge')
    jarJar "${tmlGroup}:turtymultiloader-multiblock-neoforge:[${tmlVersion}]"
}
```

The copy-ready [`consumer-template`](consumer-template) covers common/Fabric/NeoForge source and resource merging,
access wideners, access transformers, metadata expansion, client/server/GameTest runs, split datagen, `include`,
`jarJar`, and packaging assertions. It uses no convention from this repository's private `buildSrc`.

For local development in another checkout, publish every module to Maven Local:

```shell
./gradlew publishConsumerArtifactsToMavenLocal
```

Add `mavenLocal()` to the consumer's repositories. This uses the same coordinates as Repsy without composite-build
substitution.

To publish this checkout, run `./gradlew publishConsumerArtifacts`. The destination defaults to
`build/local-maven`; override it with `-PlocalMavenUrl=/path/or/url` or the `LOCAL_MAVEN_URL` environment variable.
Tagged releases are verified and published to `https://repo.repsy.io/mvn/turtywurty/public` by the release workflow.
Configure the `REPSY_USERNAME` and `REPSY_PASSWORD` GitHub Actions secrets before pushing a `v<version>` tag.

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
player join/disconnect/respawn/dimension change, successful player block breaks, post-damage/death, command
registration, and successful
datapack reloads. Callback arguments are vanilla types, and observational callbacks do not expose either loader's event
objects:

```java
Events.onLevelLoad(level ->WorldPipeNetworks.

getOrCreate(level));
        Events.

onStartLevelTick(level ->

pipeNetworks(level).

forEach(network ->network.

tick(level)));
        Events.

onPlayerDimensionChange((player, origin, destination) ->

resendLevelState(player, destination));
        Events.

onBlockBroken((level, player, pos, state, blockEntity) ->

removeFluidPocket(level, pos));
        Events.

onLivingDamaged((entity, source, damageTaken) ->

afterDamage(entity, source));
        Events.

onCommandRegistration(dispatcher ->dispatcher.

register(createIndustriaCommand()));
```

Client callbacks live under `event.client` so dedicated-server initialization never resolves client classes. They cover
client and client-level ticks, client-level enter/leave, block-entity unload, play connection/disconnection, tooltip
construction, key mappings, resource reload listeners, and the two render stages used by Industria:

```java
ClientEvents.onTooltip((stack, context, flag, lines) ->

addMobJarTooltip(stack, lines));
        ClientEvents.

onBlockEntityUnload((blockEntity, level) ->rendererCache.

remove(blockEntity.getBlockPos()));

KeyMapping debugKey = ClientEvents.registerKeyMapping(new KeyMapping(
        "key.industria.toggle_debug_rendering",
        GLFW.GLFW_KEY_F6,
        INDUSTRIA_KEY_CATEGORY
));

ClientEvents.

registerResourceReloadListener(id("conveyor_renderers"),reloadListener);
        ClientEvents.

onRenderStage(RenderStage.COLLECT_SUBMITS, conveyorRenderer::render);
ClientEvents.

onRenderStage(RenderStage.AFTER_SOLID_FEATURES, debugRenderer::render);
```

`LevelRenderContext` contains only vanilla renderer objects: the client and level, game/level renderers, render state,
pose stack, buffer source, and the submit-node collector when that stage supplies one. Register common callbacks during
common initialization and client callbacks during client initialization. Registrations are process-lifetime callbacks;
the native Fabric and NeoForge event systems do not provide a shared unregister operation.

### Configuration

`Configurations` registers loader-neutral, typed JSON configs using Mojang `Codec<T>`. The common API never exposes
NeoForge `ModConfigSpec`. Every spec declares one of four explicit scopes:

- `STARTUP`: global, loaded immediately on both physical sides, never synchronized.
- `CLIENT`: global, loaded only during physical-client initialization, never synchronized.
- `COMMON`: global, loaded during common initialization on both physical sides, never synchronized.
- `SERVER`: loaded for the active world and synchronized from the server to clients on login.

The default filenames are `<namespace>-<path>-<scope>.json`. Server files default to `<world>/serverconfig`; the other
scopes use the loader's global config directory. Custom paths can retain an existing format and location. For example,
Industria can keep `<world>/config/industria.json`:

```java
public record IndustriaConfig(boolean rubberTrees, int pipeCapacity) {
    public static final Codec<IndustriaConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("rubber_trees").forGetter(IndustriaConfig::rubberTrees),
        Codec.INT.fieldOf("pipe_capacity").forGetter(IndustriaConfig::pipeCapacity)
    ).apply(instance, IndustriaConfig::new));
}

public static final ConfigHandle<IndustriaConfig> SERVER_CONFIG = Configurations.register(
    ConfigurationSpec.builder(
            Identifier.fromNamespaceAndPath("industria", "server"),
            ConfigScope.SERVER,
            IndustriaConfig.CODEC,
            () -> new IndustriaConfig(true, 81_000)
        )
        .path(ConfigPaths.world("config/industria.json"))
        .validator(ConfigValidator.predicate(
            config -> config.pipeCapacity() > 0,
            "pipe_capacity must be positive"
        ))
        .onChange((config, lifecycle) -> rebuildCaches(config))
        .build()
);

// Optional convenience accessor used by the rest of Industria.
public static IndustriaConfig server() {
    return SERVER_CONFIG.value();
}
```

The handle is the live access point. Register it once during common mod initialisation, then read the typed value from
ordinary common code—there is no loader-specific lookup:

```java
int capacity = IndustriaConfigs.SERVER_CONFIG.value().pipeCapacity();
if(IndustriaConfigs.

server().

rubberTrees()){

generateRubberTree(level, pos);
}
```

`value()` always returns the current in-memory object. For a `SERVER` config, it becomes the world file's value during
`Events.onServerStarting`; on a remote client, the same handle becomes the server-synchronised value when the login
packet arrives. Code that must run specifically when a value becomes active should use the spec's `onChange` callback
and inspect `ConfigLifecycle`. `isLoaded()` distinguishes an installed file/network value from the preload default.
When the server stops or a remote client disconnects, every `SERVER` handle returns to its default, becomes unloaded,
and notifies its listener with `ConfigLifecycle.UNLOAD`, so values cannot leak into the next connection.

For immutable record configs, replace the record to change a setting. `setAndSave` validates, writes, and synchronises
in one operation:

```java
IndustriaConfig old = IndustriaConfigs.SERVER_CONFIG.value();
IndustriaConfigs.SERVER_CONFIG.

setAndSave(
    new IndustriaConfig(old.rubberTrees(), 162_000),
server
);
```

If several values are being changed together, call `set(...)`, then `save(server)` and `synchronize(server)` once.
`reload(server)` re-reads the world file, while `path(server)` exposes its resolved location. Treat synchronized server
handles as read-only in client code; server changes are authoritative and replace the client's value.

Missing files are created from validated defaults. Invalid JSON, codec failures, and validation failures produce a
`.bak` copy before defaults are written. Writes use a temporary file and an atomic replace where supported. Handles
support explicit load, reload, save, set, and `setAndSave`; the latter also synchronizes a server config. Use
`synchronize(server)` after an in-memory server-side change when saving is intentionally deferred.

Admins can inspect and edit registered configs with `/turtymultiloader config list`, `get <id> [path]`,
`set <id> <path> <json>`, `reload <id>`, and `save <id>`. `set` edits the encoded JSON and decodes and validates the
entire object before it is installed, saved, and synchronized. Client-scoped files cannot be changed from server
commands.

Client mods may register a custom screen through `ConfigurationScreens.register(modId, factory)`. NeoForge exposes it
from the Mods screen through `IConfigScreenFactory`; Fabric integrations can call `ConfigurationScreens.create(...)`
from Mod Menu or another optional UI without forcing that dependency into this library.

### Client registration and rendering

`ClientRegistrations` covers static client declarations whose implementations are vanilla types but whose registration
timing differs between Fabric and NeoForge. NeoForge declarations are queued for the appropriate mod-bus event; Fabric
applies them through its initialization registries. Register them from the consuming mod's client initializer:

```java
ClientRegistrations.registerEntityRenderer(RUBBER_BOAT, context ->
        new

BoatRenderer(context, RUBBER_BOAT_LAYER));
        ClientRegistrations.

registerBlockEntityRenderer(CRUSHER_BLOCK_ENTITY, CrusherRenderer::new);
ClientRegistrations.

registerModelLayer(CRUSHER_LAYER, CrusherModel::createLayer);

ClientRegistrations.

registerBlockTintSources(List.of(RUBBER_LEAVES_TINT),RUBBER_LEAVES);
        ClientRegistrations.

registerFluidModel(
    new FluidModel.Unbaked(stillMaterial, flowingMaterial, overlayMaterial, tintSource),

CRUDE_OIL_STILL,
CRUDE_OIL_FLOWING
);
```

The same facade registers data-driven client extension codecs:

```java
ClientRegistrations.registerItemTintSource(id("heated"),HeatedTintSource.CODEC);
        ClientRegistrations.

registerItemModel(id("drill_head"),DrillHeadItemModel.Unbaked.CODEC);
        ClientRegistrations.

registerSpecialModelRenderer(id("block_entity_item"),

IndustriaBlockEntityItemRenderer.Unbaked.CODEC);
```

Additional block-state models use a loader-neutral handle. Fabric backs it with an `ExtraModelKey`; NeoForge backs it
with a `StandaloneModelKey`:

```java
AdditionalModel<BlockStateModel> scannerModel =
        ClientRegistrations.registerAdditionalBlockStateModel(id("item/seismic_scanner_model"));

BlockStateModel baked = scannerModel.getOrThrow(); // only after model reload has completed
```

When the source model needs a non-identity bake transform, use the loader-neutral unbaked definition. It is the common
equivalent of Fabric's `SimpleUnbakedExtraModel.blockStateModel(modelId, modelState)`:

```java
public AdditionalBlockStateModelDefinition createUnbakedModel() {
    return AdditionalBlockStateModelDefinition.blockStateModel(modelId, modelState);
}

AdditionalModel<BlockStateModel> transformedModel =
    ClientRegistrations.registerAdditionalBlockStateModel(modelKeyId, createUnbakedModel());
```

Context-dependent wrappers use `BlockStateModelAugmenter`, avoiding Fabric's `WrapperBlockStateModel`, `QuadEmitter`,
and `FabricModelManager` in common sources. Register an augmenter for a block or a `BlockState` predicate during client
initialization. Fabric installs it in the final baked-model wrapping phase; NeoForge wraps the corresponding entries
in its baking result:

```java
ClientRegistrations.registerBlockStateModelAugmenter(
    state -> state.getBlock() instanceof PipeBlock<?, ?>,
    new BlockStateModelAugmenter() {
        @Override
        public void collectAdditionalModels(Context context, ModelCollector models) {
            for (Direction direction : Direction.values()) {
                if (context.state().getValue(PipeBlock.propertyFor(direction)) != PipeBlock.ConnectorType.BLOCK)
                    continue;

                BlockPos targetPos = context.pos().relative(direction);
                BlockState targetState = context.level().getBlockState(targetPos);
                ConnectionModelSet modelSet = PipeConnectionModelRegistry.findModel(
                    context.level(), targetPos, context.state(), targetState, direction.getOpposite());
                if (modelSet == null)
                    continue;

                ConnectionModelReference reference = modelSet.get(direction);
                BlockStateModel connectionModel = reference == null ? null : reference.model().get();
                if (connectionModel != null) {
                    long seed = context.baseSeed() ^ ((long) direction.ordinal() * 0x9E3779B97F4A7C15L);
                    models.accept(connectionModel, seed);
                }
            }
        }

        @Override
        public Object createGeometryKey(Context context, Object wrappedKey) {
            if (wrappedKey == null)
                return null;
            return new PipeGeometryKey(
                wrappedKey,
                findModelId(context, Direction.NORTH),
                findModelId(context, Direction.SOUTH),
                findModelId(context, Direction.WEST),
                findModelId(context, Direction.EAST),
                findModelId(context, Direction.UP),
                findModelId(context, Direction.DOWN)
            );
        }
    }
);
```

In this example, `ConnectionModelReference.model()` is an `AdditionalModel<BlockStateModel>` returned by
`registerAdditionalBlockStateModel(...)`; its `get()` method replaces the Fabric-only
`FabricModelManager.getModel(ExtraModelKey)` lookup. The wrapper renders the original model first and automatically
reseeds it with `state.getSeed(pos)`. Each added model uses the seed supplied to `ModelCollector`. Geometry-key code
must include every piece of level state that changes the selected geometry; returning `null` disables caching.

#### Low-level render types and GUI extraction

TurtyMultiloader exposes the private vanilla `RenderPipelines.register(...)` and `RenderType.create(...)` operations
to common client code through `ClientRegistrations.registerRenderPipeline(...)` and
`ClientRegistrations.createRenderType(...)`. Register the pipeline before using it to build and retain a render type:

```java
private static final RenderPipeline CONVEYOR_PIPELINE =
        ClientRegistrations.registerRenderPipeline(
                RenderPipeline.builder()
                        .withLocation("industria:pipeline/conveyor")
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                        .build()
        );

private static final RenderType CONVEYOR_RENDER_TYPE = ClientRegistrations.createRenderType(
        "industria:conveyor",
        RenderSetup.builder(CONVEYOR_PIPELINE)
                .affectsCrumbling()
                .createRenderSetup()
);
```

`GuiGraphicsExtractorAdapter` wraps an extractor supplied by vanilla and exposes its otherwise-private, non-synthetic
helpers consistently on Fabric and NeoForge:

```java
GuiGraphicsExtractorAdapter graphics = new GuiGraphicsExtractorAdapter(extractor);

graphics.submitGuiElementRenderState(customElementState);
graphics.submitPictureInPictureRenderState(customPictureState);

ScreenRectangle activeScissor = graphics.peekScissorStack(); // null when scissoring is disabled
GuiRenderState renderState = graphics.renderState();
graphics.innerBlit(pipeline, texture, x0, x1, y0, y1, u0, u1, v0, v1, color);

// Vanilla's already-public extractor operations remain on the wrapped object.
graphics.extractor().fill(x0, y0, x1, y1, color);
```

The adapter also covers the private fill, sprite, nine-slice, tiled-blit, item-decoration, tooltip, hover-effect, and
text-parameter helpers, plus construction with an existing `Matrix3x2fStack`. These are low-level client APIs and must
only be referenced from client-loaded code. The compiler-generated lambda methods remain private.

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
common content. TurtyMultiloader's own NeoForge entrypoint already binds the singleton service to the library mod bus;
consumers must not call `NeoForgeRegistryService.bind(...)` or bind it to their own bus.

Identifiers and vanilla registry concepts remain explicit:

```java
private static final RegistryService REGISTRIES = RegistryService.get();
private static final Identifier EXAMPLE_ID =
        Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "example");

public static final RegistrationHandle<Block, ExampleBlock> EXAMPLE_BLOCK =
        REGISTRIES.registerBlock(EXAMPLE_ID, ExampleBlock::new);

public static final RegistrationHandle<BlockEntityType<?>, BlockEntityType<ExampleBlockEntity>>
        EXAMPLE_BLOCK_ENTITY = REGISTRIES.registerBlockEntityType(
        EXAMPLE_ID,
        ExampleBlockEntity::new,
        builder -> builder.validBlock(EXAMPLE_BLOCK)
);

public static final RegistrationHandle<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
        REGISTRIES.registerCreativeTab(
                Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "example_tab"),
                builder -> builder
                        .title(Component.translatable("itemGroup." + TurtyMultiloader.MOD_ID + ".example_tab"))
                        .icon(() -> new ItemStack(EXAMPLE_BLOCK.get()))
                        .displayItems(output -> output.accept(EXAMPLE_BLOCK.get()))
        );
```

The block entity builder accepts blocks directly or as suppliers. Passing registration handles keeps block resolution
deferred until the loader applies the registry entries, so block entity types can safely reference blocks declared in
the same registration batch.

Custom creative tabs also use deferred icon and content suppliers. The loader-specific registry backend selects
Fabric's custom-tab builder or NeoForge's custom-tab builder as required; when no title is configured, the default
translation key is `itemGroup.<namespace>.<path>`.

The living-entity builder registers the entity type, its attributes, and any code-based natural spawns as one common
declaration:

```java
public static final RegistrationHandle<EntityType<?>, EntityType<ExampleEntity>> EXAMPLE_ENTITY =
        REGISTRIES.registerEntityType(
                Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "example_entity"),
                ExampleEntity::new,
                MobCategory.CREATURE,
                builder -> builder
                        .sized(0.6F, 1.8F)
                        .clientTrackingRange(8)
                        .attributes(() -> Mob.createMobAttributes()
                                .add(Attributes.MAX_HEALTH, 20.0D)
                                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                                .build())
                        .spawn(
                                Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "spawn_example_entity"),
                                BiomeSelectors.foundInOverworld(),
                                10,
                                2,
                                4
                        )
        );
```

Each spawn declaration has its own unique biome-modification ID and can use any composable `BiomeSelector`. Options
not directly exposed by the wrapper remain available through `builder.vanilla(...)`. Non-living entities continue to
use `registerEntityType(id, factory)` with a vanilla `EntityType.Builder` because they have no attributes or natural
mob spawns.

Register the renderer from the consuming mod's client initializer, where the registration handle can be passed
directly as the deferred entity-type supplier:

```java
ClientRegistrations.registerEntityRenderer(EXAMPLE_ENTITY, ExampleEntityRenderer::new);
```

Loading the content class queues the type and its attributes; the loader entrypoint must then call
`RegistryService.get().apply()`.

#### Wood sets

The Industria-style wood-set registration is available as one common declaration. It creates planks, logs and wood,
their stripped variants, leaves, a sapling, stairs, slab, fence and gate, door, trapdoor, pressure plate, button,
ordinary and hanging signs, both sign items, boat and chest-boat entity types, and both boat items:

```java
public static final WoodSet RUBBER = REGISTRIES.registerWoodSet(
        Industria.id("rubber"),
        ModTreeGrowers.RUBBER,
        builder -> builder
                .leaves((context, properties) -> new RubberLeavesBlock(properties))
                .properties(WoodSetBuilder.BlockType.PLANKS, properties -> properties.strength(3.0F))
);
```

The returned `WoodSet` exposes registration handles for every generated object and its `<name>_logs` block/item tags.
The builder also has named factory methods for every block, sign/boat item factories, boat entity-builder transforms,
custom `BlockSetType`/`WoodType` factories, and a generic `block(...)`/`properties(...)` escape hatch. Defaults copy
the corresponding oak behavior and retain Industria's names such as `rubber_stripped_log`.

Registration automatically wires vanilla `BlockSetType`/`WoodType` lookup (which also makes sign materials available),
stripping, flammability, sign block-entity valid blocks, and boat dispenser behavior on both loaders. Call this from
the consuming mod's client initializer for boat model layers and renderers:

```java
WoodSetClient.register(ModWoodSets.RUBBER);
```

Add the complete recipes, block loot, vanilla tags, English translations, blockstates, and item/block models to the
common data-generation spec with one line:

```java
DataGeneration.spec(Industria.MOD_ID)
    .

woodSet(ModWoodSets.RUBBER)
    .

build();
```

Provide the normal wood textures plus `textures/entity/signs/rubber.png`,
`textures/entity/signs/hanging/rubber.png`, `textures/entity/boat/rubber.png`, and
`textures/entity/chest_boat/rubber.png`. Creative-tab population remains explicit so each mod controls where the set
appears.

`RegistrationHandle` exposes `id()`, the vanilla entry `key()`, and the bound vanilla `holder()`. It also implements
`Supplier<T>` for constructors that depend on earlier declarations. Calling `get()` or `holder()` before registration
has been applied fails explicitly.

Custom trunk placers can be registered directly from their codec; consumers do not need to construct the
private-constructor vanilla `TrunkPlacerType` themselves:

```java
public static final RegistrationHandle<TrunkPlacerType<?>, TrunkPlacerType<ExampleTrunkPlacer>>
        EXAMPLE_TRUNK_PLACER = REGISTRIES.registerTrunkPlacerType(
        Identifier.fromNamespaceAndPath(TurtyMultiloader.MOD_ID, "example_trunk_placer"),
        ExampleTrunkPlacer.CODEC
);
```

Vanilla's private reusable block predicates are exposed to shared consumer source through
`VanillaBlockPredicates`. Use this class instead of referring to the private methods on `Blocks` directly:

```java
BlockBehaviour.Properties.of()
    .

isRedstoneConductor(VanillaBlockPredicates::never)
    .

isSuffocating(VanillaBlockPredicates::never)
    .

isValidSpawn(VanillaBlockPredicates::ocelotOrParrot);
```

The service has typed helpers for blocks, items, fluids, block entities, entities and attributes, menus, recipes,
data components, consume effects, position sources, world-generation features, creative tabs, wood sets/types,
stripping, and flammability. `register(...)` accepts any vanilla `ResourceKey<? extends Registry<R>>`, and
`customRegistry(...)` creates custom registries whose entries use the same handles.

### World generation

`WorldGeneration` covers the loader lifecycle around datapack registries, built-in datapacks, data-generator
bootstraps, and the small set of biome changes which must be expressed in code. Configured features, placed features,
biome tags, and other vanilla world-generation objects should remain ordinary resources wherever possible.

Industria's configured and placed feature bootstraps can be declared in common initialization and collected by either
loader's data generator:

```java
WorldGeneration.registerBootstrap(Registries.CONFIGURED_FEATURE, ConfiguredFeatureInit::bootstrap);
WorldGeneration.

registerBootstrap(Registries.PLACED_FEATURE, PlacedFeatureInit::bootstrap);

// In the loader data-generator callback:
WorldGeneration.

addBootstraps(registryBuilder);
```

Multiple declarations for the same registry are composed in registration order. The registration only describes data
generation; runtime configured/placed features still come from the generated datapack resources.

Biome tags are the preferred selection mechanism. When a cross-loader insertion genuinely needs code—for example,
because Fabric has no equivalent data resource or the predicate comes from configuration—the same declaration works
on both loaders:

```java
BiomeSelector overworld = BiomeSelectors.tag(BiomeTags.IS_OVERWORLD);

WorldGeneration.

addFeature(
        id("bauxite_ore"),

overworld,
GenerationStep.Decoration.UNDERGROUND_ORES,
PlacedFeatureInit.BAUXITE_ORE
);

        WorldGeneration.

addSpawn(
        id("example_spawn"),
    BiomeSelectors.

includeByKey(Biomes.PLAINS),

MobCategory.CREATURE,
        ()->EntityType.COW,
        10,
        2,
        4
        );
```

Selectors can match keys, tags, namespaces, existing placed/configured features, or compose custom predicates with
`and`, `or`, and `negate`. Feature removal and entity-spawn removal are also available. On NeoForge, code declarations
are applied by a standard datapack biome modifier and retain their declaration order. Their position relative to other
datapack modifiers follows NeoForge's biome-modifier registry ordering. For NeoForge-only static changes, prefer normal
files under `data/<namespace>/neoforge/biome_modifier/`; do not also declare the same change in code.

Custom registries whose entries are loaded from datapacks can be server-only or synchronized. Registration belongs in
common initialization, before the loaders' registry events:

```java
WorldGeneration.registerDatapackRegistry(RESEARCH_KEY, Research.CODEC);
WorldGeneration.

registerSyncedDatapackRegistry(MATERIAL_KEY, Material.CODEC, Material.NETWORK_CODEC);
```

Optional or forced built-in datapacks live at `resourcepacks/<id path>` in the owning mod JAR. The identifier namespace
must be the owning mod ID:

```java
WorldGeneration.registerBuiltInDatapack(
        id("classic_ores"),
    Component.

translatable("pack.industria.classic_ores"),

BuiltInDatapackActivation.DEFAULT_ENABLED
);
```

Use `ALWAYS_ENABLED` only when disabling the pack would make the mod invalid. `NORMAL` leaves it disabled until the
user selects it.

### Data generation

`DataGenerationSpec` keeps provider declarations in common code. It supports vanilla recipe callbacks and loot-table
subproviders, key-based block/item/fluid/entity (or arbitrary registry) tags, language files, raw blockstate/model/item
JSON, arbitrary vanilla `DataProvider` factories, and dynamic-registry bootstraps. Damage types and world generation
are typed conveniences over the dynamic-registry path.

```java
private static final ConventionTag<Item> TIN_INGOTS = ConventionTags.item("ingots/tin");

public static final DataGenerationSpec DATA = DataGeneration.spec(Industria.MOD_ID)
        .recipes(IndustriaRecipes::generate)
        .lootTables(Set.of(), IndustriaLootTables.SUB_PROVIDERS)
        .blockTags(IndustriaTags::generateBlocks)
        .itemTags((registries, tags) -> tags.tag(TIN_INGOTS).add(ModItems.TIN_INGOT.key()))
        .fluidTags(IndustriaTags::generateFluids)
        .entityTypeTags(IndustriaTags::generateEntityTypes)
        .language("en_us", IndustriaLanguage::generate)
        .models(IndustriaModels::generate)
        .vanillaModels(IndustriaModels::generateTyped)
        .damageTypes(IndustriaDamageTypes::bootstrap)
        .worldGeneration(Registries.CONFIGURED_FEATURE, ConfiguredFeatureInit::bootstrap)
        .worldGeneration(Registries.PLACED_FEATURE, PlacedFeatureInit::bootstrap)
        .build();
```

Tag callbacks receive a public vanilla `TagAppender<ResourceKey<T>, T>`. A tag for any other registry can be added with
`tags(registryKey, callback)`. Raw model callbacks can declare `blockState`, `blockModel`, `itemModel`, and the modern
`itemDefinition`; each accepts a vanilla `Identifier` and Gson `JsonElement`. `blockModel` and `itemModel` accept either
a short path such as `machine` or an already conventional `block/machine` or `item/machine` path; an existing prefix
is never added twice. `vanillaModels((blocks, items) -> ...)` supplies common code with fully wired vanilla
`BlockModelGenerators` and `ItemModelGenerators`, including blockstate, model, item-definition, and item-copy output.
Use
`provider(DataGenerationSide.CLIENT/SERVER, factory)` when a specialized vanilla provider is more appropriate.

Convention tags never require Fabric or NeoForge imports in common source. `ConventionTags.item("ingots/tin")` and
the block/fluid/entity equivalents describe the logical `c:` tag, while `ConventionTags.mapped(...)` can describe a
convention whose Fabric and common spellings differ. Calling `ConventionTag.key()` or using it in a tag callback asks
the loader service for the correct key. This also means recipe code can use `TIN_INGOTS.key()` without leaking Fabric
tag constants into common code.

Fabric uses a normal data-generator entrypoint:

```java
public final class IndustriaFabricDataGeneration implements DataGeneratorEntrypoint {
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGeneration.run(generator, IndustriaDataGeneration.DATA);
    }

    public void buildRegistry(RegistrySetBuilder builder) {
        DataGeneration.addRegistryBootstraps(IndustriaDataGeneration.DATA, builder);
    }
}
```

Register that class under the `fabric-datagen` entrypoint. On NeoForge 26.1, client and server generation are separate
events; bind both on the consuming mod's event bus:

```java
modBus.addListener(GatherDataEvent.Client .class,
                   event ->NeoForgeDataGeneration.

run(event, IndustriaDataGeneration.DATA));
        modBus.

addListener(GatherDataEvent.Server .class,
            event ->NeoForgeDataGeneration.

run(event, IndustriaDataGeneration.DATA));
```

NeoForge's client and server runs must use separate output roots so one hash cache cannot remove the other run's
files. Add both roots to the main resources source set. Fabric has one combined run and one output root. Configure
that Loom run with `client()` when the spec uses `vanillaModels(...)`: vanilla's model generators are client-environment
classes, and Fabric API's client datagen bootstrap runs the same combined providers before normal client startup.

Existing bootstraps registered through `WorldGeneration.registerBootstrap(...)` can be included with
`registeredWorldGeneration()`. The Fabric entrypoint must still pass the spec to `addRegistryBootstraps`; the NeoForge
server runner performs that step itself.

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
float, lossless `long`, and lossless `double` mappings. Vanilla transmits only 16 bits for each data slot, so raw
`add(...)` integer values are limited to the signed-short range. Floats occupy two slots; longs and doubles occupy
four:

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
NETWORK.registerClientHandler(PayloadPhase.PLAY, SyncMachinePayload.TYPE, (payload, context) ->{
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
NETWORK.onConnection(context ->

preload(context.profile()));
        NETWORK.

onJoin(player ->

initializeSession(player));
        NETWORK.

onDisconnect(player ->

closeSession(player));
        NETWORK.

onClientJoin(ClientState::connected);
NETWORK.

onClientDisconnect(ClientState::disconnected);

NETWORK.

addLoginSync(player ->List.

of(
        createMachineSnapshot(player),

createNetworkSnapshot(player)
));
```

Login sync providers run in registration order after the server join callbacks. Unsupported optional payloads are
filtered per player. Networking declarations do not use `RegistryService.apply()`; Fabric registers them immediately
and NeoForge consumes them from `RegisterPayloadHandlersEvent`.

Configuration-phase work that must finish before the player enters the world uses an ordered server task. Register
the request as a clientbound configuration payload and its acknowledgement as a serverbound configuration payload,
then complete the named task from the acknowledgement handler:

```java
private static final Identifier RULES_TASK = id("rules_task");

NETWORK.

registerConfigurationClientbound(RulesPayload.TYPE, RulesPayload.CODEC,
                                 PayloadRegistrationOptions.required("1"));
        NETWORK.

registerConfigurationServerbound(RulesAcceptedPayload.TYPE, RulesAcceptedPayload.CODEC,
                                 PayloadRegistrationOptions.required("1"),
    (payload,context)->context.

completeConfigurationTask(RULES_TASK));

        NETWORK.

registerConfigurationTask(new ServerConfigurationTask(RULES_TASK, context ->
        context.

send(createRulesPayload(context.connection().

profile()))
        ));
```

The client handler applies the request and calls `context.reply(new RulesAcceptedPayload())`. Configuration packets
are ordered, so the acknowledgement is sent after that handler runs. The task waits until `complete()` or
`completeConfigurationTask(...)` is called. `send(...)` rejects undeclared or wrongly-directed payloads, disconnects
for an unsupported required payload, and returns `false` for an unsupported optional payload; an optional task can
call `complete()` when there is nothing to send. `fail(Component)` disconnects explicitly. Both loaders install these
as native configuration tasks rather than emulating the wait after the player joins.

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
target.

set(STOMACH_DESTRUCTION, value +1);
target.

remove(STOMACH_DESTRUCTION);
```

Omit `persistent(...)` (or call `transientValue()`) for memory-only data. `syncWith(codec, predicate)` accepts a
custom `(target, player)` predicate; `syncToOwner(...)` and `syncToTrackers(...)` cover the usual policies. `set`,
`update`, and `remove` notify the native attachment system automatically. After changing a mutable value in place,
call `markDirty`, or use `mutate`, which marks block entities/chunks for saving and sends the updated value:

```java
AttachmentTarget.chunk(chunk).

mutate(MULTIBLOCKS, map ->map.

put(pos, data));
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

WORLD_INDEX.

access(serverLevel).

mutate(index ->index.

add(machine));
        GLOBAL_RESEARCH.

access(server).

update(ResearchState::advance);
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
try(
TransferTransaction transaction = TransferTransaction.openRoot()){
long inserted = target.insert(itemVariant, 16, transaction);
    if(inserted ==16)
        transaction.

commit();
}
```

A vanilla `Container` can be exposed as a live neutral item storage. The optional direction filters a
`WorldlyContainer` to the slots available from that face and enforces its sided insertion/extraction rules:

```java
ResourceStorage<ResourceVariant<Item>> inventory = ContainerStorage.of(container);
ResourceStorage<ResourceVariant<Item>> sidedInventory = ContainerStorage.of(container, direction);
```

`ResourceStorage` is indexed and exposes per-index resource, amount, capacity, validity, and insertion/extraction
support. `MutableResourceStorage` adds trusted transactional `insertInternal`, `extractInternal`, and exact `set`
operations for machine owners and persistence. These bypass external transfer direction policy while retaining type,
validity, and capacity invariants. `SimpleStorage` implements this contract and offers protected state access plus
dynamic capacity hooks; `SimpleSingleSlotStorage` also has a no-fixed-capacity constructor for subclasses.

Use `storage.restrictedTo(TransferSupport.INSERT_ONLY)` or `EXTRACT_ONLY` when exposing directional machine ports.
The returned live view narrows automation access without restricting recipes or persistence on the owned backing
storage. `SimpleEnergyStorage` provides the common capacity/max-input/max-output energy implementation.

`SingleSlotStorage`, `CombinedStorage`, `RestrictedStorage`, and `SidedStorage` provide the common views. Root and
nested transactions
support commit, rollback-on-close, simulation, snapshot participants, close callbacks, and final commit callbacks.
`StorageSnapshot`, `StorageCodecs`, and `StorageSynchronizer` cover persistence and synchronization; snapshots use the
trusted exact replacement path when the destination implements `MutableResourceStorage`.

Item-contained storage lookups take a single `MutableItemContext`; its `stack()` is the authoritative lookup stack, so
a context can no longer be paired with an unrelated `ItemStack`. Neutral factories cover detached constants,
single-slot storages, container/inventory slots, player hands, and carried cursor stacks:

```java
MutableItemContext context = MutableItemContext.withConstant(stack);
ResourceStorage<ResourceVariant<UnitResource>> energy = context.find(StorageKeys.ENERGY);
```

Neutral fluid amounts use droplets (`81,000` per bucket), allowing exact conversion to Fabric units and NeoForge's
`1,000`-unit bucket convention. Core defines item, fluid, and energy units. Optional resource modules register their
own dimensions and units. All standard and mod-defined units are canonical entries in `Units.REGISTRY`:

Fluid properties are available through the loader-neutral `FluidVariantAttributes`. A handler registered for a still
or flowing `Fluid` is a variant-aware overlay: methods it does not override continue to read the active loader's
native name, sounds, luminance, temperature, viscosity, density, and lighter-than-air value. NeoForge queries the
component-bearing `FluidStack` overloads of `FluidType` where available.

```java
FluidVariantAttributes.register(CRUDE_OIL.get(), new

FluidVariantAttributeHandler() {
    @Override
    public int getViscosity (ResourceVariant < Fluid > variant, @Nullable Level level){
        return variant.hasComponents() ? 8_000 : 7_500;
    }
});

int viscosity = FluidVariantAttributes.getViscosity(oilVariant, level);
int light = FluidVariantAttributes.getLuminance(oilVariant);
boolean rises = FluidVariantAttributes.isLighterThanAir(oilVariant);
```

Unit registration uses the same neutral registry:

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

GasStorage.

registerBlockProvider(
    (level, pos, state, blockEntity, side) ->blockEntity.

gasStorage(side),

GAS_TANK_BLOCK
);

ResourceVariant<Gas> steam = GasVariant.of(STEAM.holder());
```

`GasApi.UNIT`/`GasApi.BUCKET` and `SlurryApi.UNIT`/`SlurryApi.BUCKET` are registered only when their module is on the
classpath. Both use `81,000` neutral units per bucket. Their providers interoperate through TurtyMultiloader's
generated Fabric lookups and NeoForge capabilities; integration with another mod's chemical API still requires a
dedicated adapter.

### Optional multiblock module

The multiblock module ports `MultiblockLib` to common, Fabric, and NeoForge artifacts while retaining its
`dev.turtywurty.multiblocklib` packages and `multiblocklib` mod ID. It provides data-pack definitions from
`data/<namespace>/multiblocks/*.json`, block matchers, transforms, persistent formed-structure data, controller and
part blocks, client rendering, built-in pattern factories, and typed ports.

```groovy
// Choose the matching dependency in each loader project.
implementation project(':multiblock-fabric')
implementation project(':multiblock-neoforge')
```

Built-in item, fluid, and energy ports now use TurtyMultiloader's neutral `ResourceStorage`, `ResourceVariant`, and
transaction APIs. Custom ports can continue to use `PortType`, `PortRegistrar`, and `PortTransfer`. Register custom
controller blocks with `MultiblockLib.registerControllerBlock(...)`; use
`MULTIBLOCK_PART_HANDLE`, `MULTIBLOCK_CONTROLLER_HANDLE`, and `MULTIBLOCK_CONTROLLER_ENTITY_HANDLE` in declarations
that run before registry application. The corresponding direct fields are populated during registration and are safe
to use afterward.

As with every TurtyMultiloader transfer provider, the consuming mod must call `TransferService.get().apply()` after it
has declared its own providers. MultiblockLib deliberately does not flush that shared service from its dependency
entrypoint, because doing so would make Fabric resolve a consuming mod's not-yet-applied registry handles. Its loader
entrypoints initialize the module itself, including registry content, reload listeners, interaction hooks, and the
client block-entity renderer.

The original MultiblockLib code is LGPL-3.0; the module retains that license in `modules/multiblock/LICENSE` and in its
published JARs rather than relicensing it under the core project's CC0 license.

The Fabric module adapts its native `Storage`, `StorageView`, and `TransactionContext`, Team Reborn `EnergyStorage`,
block/item/entity API lookups, and mutable container-item contexts. The NeoForge module adapts `ResourceHandler`,
`EnergyHandler`, capabilities,
its native `TransactionContext`, and `ItemAccess`; custom resources use the same capability machinery. Provider
declarations are queued until `TransferService.apply()`. On Fabric, declarations made after the first `apply()` are
registered immediately; another `apply()` call is harmless but unnecessary. NeoForge's library entrypoint owns its
event-bus hookup and accepts declarations until the capability-registration event fires, then rejects late
declarations explicitly; consuming mods do not bind the service to their own event bus.

Only native storages with stable, addressable slots expose indexed access. Arbitrary Fabric `Storage` implementations
remain usable through aggregate insert/extract operations, but report `hasStableIndices() == false` and zero indexed
slots. Indexed snapshots, synchronization, and views reject these storages instead of capturing a misleading empty
inventory. For stable Fabric slots, resource-dependent capacity is measured with a rolled-back insertion simulation.

### Initialization

Loader entrypoints call `CommonMod.init()`. Their client-only entrypoints call `CommonClient.init()`, so client
initialization is never linked from a dedicated-server entrypoint. Both methods are idempotent.

Optional integrations are registered as lazy factories through `OptionalModIntegrations.register(...)` or
`registerClient(...)`. A factory is instantiated only when its required mod is loaded. Register client integrations from
client initialization code so their optional client API references are never linked on a dedicated server.

To run the library's split data-generation configurations for NeoForge:

```shell
./gradlew :neoforge:runData :neoforge:runServerData
```

## Test mod

The `testmod-fabric` and `testmod-neoforge` Gradle projects are real consumer mods with the separate mod ID
`turtymultiloader_testmod`. Their shared GameTests register content and storage providers through the public library
API, compile and register every common/client event bridge, verify registries and holders, and exercise transactions,
serialization, sided/combined and indexed views, component-bearing item/fluid resources, optional gas/slurry module
resources and codecs, units, the optional multiblock module's registration and neutral port storage,
post-apply provider declarations, Team Reborn energy, and loader-native data generation in a running dedicated test
server. These projects produce
separate test-mod artifacts and are not packaged into either library artifact.

```shell
./gradlew :testmod-fabric:runGameTest
./gradlew :testmod-neoforge:runGameTest
./gradlew :testmod-fabric:runDataGeneration
./gradlew :testmod-neoforge:runData :testmod-neoforge:runServerData
```

The test projects depend on the loader library projects, never the reverse. They do not apply the publishing
conventions, and their source sets and metadata are not included in either library JAR. In addition,
`verifyPublishedConsumer` first publishes all twelve artifacts and builds the standalone `consumer-template` exclusively
from their Maven coordinates. Its packaging assertions inspect Fabric `META-INF/jars` and NeoForge
`META-INF/jarjar/metadata.json`, covering published Gradle/POM metadata, transitive module dependencies, `include`, and
`jarJar` instead of silently substituting project dependencies.
