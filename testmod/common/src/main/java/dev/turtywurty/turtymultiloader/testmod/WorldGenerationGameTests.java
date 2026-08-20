package dev.turtywurty.turtymultiloader.testmod;

import com.mojang.serialization.Codec;
import dev.turtywurty.turtymultiloader.worldgen.BiomeSelectors;
import dev.turtywurty.turtymultiloader.worldgen.BuiltInDatapackActivation;
import dev.turtywurty.turtymultiloader.worldgen.WorldGeneration;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.concurrent.atomic.AtomicBoolean;

public final class WorldGenerationGameTests {
    public static final Identifier TEST_ID = id("world_generation_service");
    public static final ResourceKey<Registry<TestWorldgenValue>> TEST_REGISTRY = ResourceKey.createRegistryKey(
        id("test_worldgen_value")
    );
    public static final ResourceKey<TestWorldgenValue> BOOTSTRAPPED_VALUE = ResourceKey.create(
        TEST_REGISTRY,
        id("bootstrapped")
    );
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private WorldGenerationGameTests() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true))
            return;

        WorldGeneration.registerDatapackRegistry(TEST_REGISTRY, TestWorldgenValue.CODEC);
        WorldGeneration.registerBootstrap(
            TEST_REGISTRY,
            context -> context.register(BOOTSTRAPPED_VALUE, new TestWorldgenValue("data-generator bootstrap"))
        );
        WorldGeneration.registerBuiltInDatapack(
            id("worldgen_test_pack"),
            Component.literal("TurtyMultiloader world-generation test pack"),
            BuiltInDatapackActivation.NORMAL
        );

        // False selectors exercise both code-modifier backends without altering test-world generation or spawns.
        var disabledPlains = BiomeSelectors.includeByKey(Biomes.PLAINS).and(context -> false);
        WorldGeneration.addFeature(
            id("disabled_feature_addition"),
            disabledPlains,
            GenerationStep.Decoration.UNDERGROUND_ORES,
            OrePlacements.ORE_COAL_UPPER
        );
        WorldGeneration.addSpawn(
            id("disabled_spawn_addition"),
            disabledPlains,
            MobCategory.CREATURE,
            () -> EntityType.COW,
            10,
            2,
            4
        );
        WorldGeneration.removeSpawn(
            id("disabled_spawn_removal"),
            disabledPlains,
            () -> EntityType.PIG
        );
    }

    public static void verifyWorldGenerationService(GameTestHelper helper) {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        WorldGeneration.addBootstraps(builder);
        var generatedRegistries = builder.build(RegistryAccess.EMPTY);
        helper.assertValueEqual(
            generatedRegistries.lookupOrThrow(TEST_REGISTRY).getOrThrow(BOOTSTRAPPED_VALUE).value().description(),
            "data-generator bootstrap",
            "Collected registry bootstrap value"
        );
        helper.assertTrue(
            helper.getLevel().registryAccess().lookup(TEST_REGISTRY).isPresent(),
            "Custom datapack registry was not loaded"
        );
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, path);
    }

    public record TestWorldgenValue(String description) {
        public static final Codec<TestWorldgenValue> CODEC = Codec.STRING.xmap(
            TestWorldgenValue::new,
            TestWorldgenValue::description
        );
    }
}
