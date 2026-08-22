package dev.turtywurty.turtymultiloader.testmod;

import com.google.gson.JsonParser;
import dev.turtywurty.turtymultiloader.datagen.DataGeneration;
import dev.turtywurty.turtymultiloader.datagen.DataGenerationSpec;
import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTag;
import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTags;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

/**
 * Compile- and run-time smoke coverage for every common data-generation provider family.
 */
public final class TestDataGeneration {
    private static final ConventionTag<net.minecraft.world.item.Item> TEST_INGOTS =
        ConventionTags.item("ingots/turtymultiloader_test");
    private static final ResourceKey<DamageType> TEST_DAMAGE_TYPE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        id("test_damage")
    );
    private static final ResourceKey<LootTable> TEST_LOOT_TABLE = ResourceKey.create(
        Registries.LOOT_TABLE,
        id("test_empty")
    );
    private static final ResourceKey<Recipe<?>> TEST_RECIPE = ResourceKey.create(
        Registries.RECIPE,
        id("test_convention_tag")
    );

    public static final DataGenerationSpec SPEC = DataGeneration.spec(TestModContent.MOD_ID)
        .woodSet(TestModContent.TEST_WOOD_SET)
        .recipes((registries, output) -> ShapelessRecipeBuilder.shapeless(
                registries.lookupOrThrow(Registries.ITEM),
                RecipeCategory.MISC,
                Items.DIAMOND
            )
            .requires(TEST_INGOTS.key())
            .unlockedBy("has_iron_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
            .save(output, TEST_RECIPE))
        .blockTags((registries, tags) -> tags.tag(TagKey.create(
            Registries.BLOCK,
            id("test_blocks")
        )).add(Blocks.STONE.builtInRegistryHolder().key()))
        .itemTags((registries, tags) -> tags.tag(TEST_INGOTS)
            .add(Items.IRON_INGOT.builtInRegistryHolder().key()))
        .fluidTags((registries, tags) -> tags.tag(TagKey.create(
            Registries.FLUID,
            id("test_fluids")
        )).add(Fluids.WATER.builtInRegistryHolder().key()))
        .entityTypeTags((registries, tags) -> tags.tag(TagKey.create(
            Registries.ENTITY_TYPE,
            id("test_entities")
        )).add(EntityType.COW.builtInRegistryHolder().key()))
        .language("en_us", translations -> {
            translations.add("datagen.turtymultiloader_testmod.smoke", "Data generation smoke test");
            translations.add(TEST_INGOTS, "Test Ingots");
        })
        .models(models -> {
            models.blockState(id("test_block"), JsonParser.parseString(
                "{\"variants\":{\"\":{\"model\":\"turtymultiloader_testmod:block/test_block\"}}}"
            ));
            models.blockModel(id("test_block"), JsonParser.parseString(
                "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/stone\"}}"
            ));
            models.blockModel(id("block/test_prefixed"), JsonParser.parseString(
                "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/stone\"}}"
            ));
            models.itemModel(id("test_item"), JsonParser.parseString(
                "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/iron_ingot\"}}"
            ));
            models.itemModel(id("item/test_prefixed"), JsonParser.parseString(
                "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/iron_ingot\"}}"
            ));
            models.itemDefinition(id("test_item"), JsonParser.parseString(
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"turtymultiloader_testmod:item/test_item\"}}"
            ));
        })
        .vanillaModels((blocks, items) -> blocks.createParticleOnlyBlock(
            TestModContent.TEST_LOG.get(),
            Blocks.STONE
        ))
        .lootTables(
            Set.of(TEST_LOOT_TABLE),
            List.of(new LootTableProvider.SubProviderEntry(
                registries -> output -> output.accept(TEST_LOOT_TABLE, LootTable.lootTable()),
                LootContextParamSets.EMPTY
            ))
        )
        .damageTypes(context -> context.register(TEST_DAMAGE_TYPE, new DamageType("test_damage", 0.1F)))
        .build();

    private TestDataGeneration() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, path);
    }
}
