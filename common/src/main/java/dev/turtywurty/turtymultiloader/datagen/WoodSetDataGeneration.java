package dev.turtywurty.turtymultiloader.datagen;

import dev.turtywurty.turtymultiloader.registration.BlockWithItem;
import dev.turtywurty.turtymultiloader.registration.WoodSet;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Reusable data-generator contribution for {@link WoodSet}.
 */
public final class WoodSetDataGeneration {
    private WoodSetDataGeneration() {
    }

    public static void contribute(DataGenerationSpec.Builder builder, WoodSet set) {
        builder
            .recipes((registries, output) -> new Recipes(registries, output, set).generate())
            .lootTables(Set.of(), List.of(new LootTableProvider.SubProviderEntry(
                registries -> new DirectLoot(registries, set), LootContextParamSets.BLOCK
            )))
            .blockTags((registries, tags) -> {
                tags.tag(set.logsBlockTag()).add(
                    set.log().block().key(), set.strippedLog().block().key(), set.wood().block().key(),
                    set.strippedWood().block().key()
                );
                tags.tag(BlockTags.LOGS_THAT_BURN).addTag(set.logsBlockTag());
                tags.tag(BlockTags.PLANKS).add(set.planks().block().key());
                tags.tag(BlockTags.LEAVES).add(set.leaves().block().key());
                tags.tag(BlockTags.SAPLINGS).add(set.sapling().block().key());
                tags.tag(BlockTags.WOODEN_BUTTONS).add(set.button().block().key());
                tags.tag(BlockTags.WOODEN_DOORS).add(set.door().block().key());
                tags.tag(BlockTags.WOODEN_FENCES).add(set.fence().block().key());
                tags.tag(BlockTags.FENCE_GATES).add(set.fenceGate().block().key());
                tags.tag(BlockTags.WOODEN_PRESSURE_PLATES).add(set.pressurePlate().block().key());
                tags.tag(BlockTags.WOODEN_TRAPDOORS).add(set.trapdoor().block().key());
                tags.tag(BlockTags.WOODEN_STAIRS).add(set.stairs().block().key());
                tags.tag(BlockTags.WOODEN_SLABS).add(set.slab().block().key());
                tags.tag(BlockTags.STANDING_SIGNS).add(set.sign().key());
                tags.tag(BlockTags.WALL_SIGNS).add(set.wallSign().key());
                tags.tag(BlockTags.CEILING_HANGING_SIGNS).add(set.hangingSign().key());
                tags.tag(BlockTags.WALL_HANGING_SIGNS).add(set.wallHangingSign().key());
            })
            .itemTags((registries, tags) -> {
                tags.tag(set.logsItemTag()).add(
                    set.log().item().key(), set.strippedLog().item().key(), set.wood().item().key(),
                    set.strippedWood().item().key()
                );
                tags.tag(ItemTags.LOGS_THAT_BURN).addTag(set.logsItemTag());
                tags.tag(ItemTags.PLANKS).add(set.planks().item().key());
                tags.tag(ItemTags.LEAVES).add(set.leaves().item().key());
                tags.tag(ItemTags.SAPLINGS).add(set.sapling().item().key());
                tags.tag(ItemTags.WOODEN_BUTTONS).add(set.button().item().key());
                tags.tag(ItemTags.WOODEN_DOORS).add(set.door().item().key());
                tags.tag(ItemTags.WOODEN_FENCES).add(set.fence().item().key());
                tags.tag(ItemTags.FENCE_GATES).add(set.fenceGate().item().key());
                tags.tag(ItemTags.WOODEN_PRESSURE_PLATES).add(set.pressurePlate().item().key());
                tags.tag(ItemTags.WOODEN_TRAPDOORS).add(set.trapdoor().item().key());
                tags.tag(ItemTags.WOODEN_STAIRS).add(set.stairs().item().key());
                tags.tag(ItemTags.WOODEN_SLABS).add(set.slab().item().key());
                tags.tag(ItemTags.SIGNS).add(set.signItem().key());
                tags.tag(ItemTags.HANGING_SIGNS).add(set.hangingSignItem().key());
                tags.tag(ItemTags.BOATS).add(set.boatItem().key());
                tags.tag(ItemTags.CHEST_BOATS).add(set.chestBoatItem().key());
            })
            .entityTypeTags((registries, tags) -> tags.tag(EntityTypeTags.BOAT)
                .add(set.boatEntityType().key(), set.chestBoatEntityType().key()))
            .language("en_us", language -> addLanguage(language, set))
            .vanillaModels((blocks, items) -> addModels(blocks, items, set));
    }

    private static void addLanguage(
        dev.turtywurty.turtymultiloader.datagen.language.LanguageGenerationContext language,
        WoodSet set
    ) {
        String name = displayName(set.name());
        language.add(set.planks().get(), name + " Planks");
        language.add(set.log().get(), name + " Log");
        language.add(set.strippedLog().get(), "Stripped " + name + " Log");
        language.add(set.strippedWood().get(), "Stripped " + name + " Wood");
        language.add(set.wood().get(), name + " Wood");
        language.add(set.leaves().get(), name + " Leaves");
        language.add(set.sapling().get(), name + " Sapling");
        language.add(set.stairs().get(), name + " Stairs");
        language.add(set.slab().get(), name + " Slab");
        language.add(set.fence().get(), name + " Fence");
        language.add(set.fenceGate().get(), name + " Fence Gate");
        language.add(set.door().get(), name + " Door");
        language.add(set.trapdoor().get(), name + " Trapdoor");
        language.add(set.pressurePlate().get(), name + " Pressure Plate");
        language.add(set.button().get(), name + " Button");
        language.add(set.signItem().get(), name + " Sign");
        language.add(set.hangingSignItem().get(), name + " Hanging Sign");
        language.add(set.boatItem().get(), name + " Boat");
        language.add(set.chestBoatItem().get(), name + " Chest Boat");
        language.add(set.boatEntityType().get(), name + " Boat");
        language.add(set.chestBoatEntityType().get(), name + " Chest Boat");
    }

    private static void addModels(BlockModelGenerators models, ItemModelGenerators items, WoodSet set) {
        models.woodProvider(set.log().get()).logWithHorizontal(set.log().get()).wood(set.wood().get());
        models.woodProvider(set.strippedLog().get())
            .logWithHorizontal(set.strippedLog().get()).wood(set.strippedWood().get());
        models.createTintedLeaves(set.leaves().get(), TexturedModel.LEAVES, 0x00BB0A);
        models.createCrossBlockWithDefaultItem(set.sapling().get(), BlockModelGenerators.PlantType.NOT_TINTED);
        models.createHangingSign(set.strippedLog().get(), set.hangingSign().get(), set.wallHangingSign().get());
        models.family(set.planks().get()).generateFor(set.createBlockFamily());
        items.generateFlatItem(set.boatItem().get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(set.chestBoatItem().get(), ModelTemplates.FLAT_ITEM);
    }

    private static String displayName(String path) {
        StringBuilder result = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.toString();
    }

    private static final class Recipes extends RecipeProvider {
        private final WoodSet set;

        private Recipes(HolderLookup.Provider registries, RecipeOutput output, WoodSet set) {
            super(registries, output);
            this.set = set;
        }

        private void generate() {
            planksFromLogs(set.planks().get(), set.logsItemTag(), 4);
            woodFromLogs(set.wood().get(), set.log().get());
            woodFromLogs(set.strippedWood().get(), set.strippedLog().get());
            woodenBoat(set.boatItem().get(), set.planks().get());
            chestBoat(set.chestBoatItem().get(), set.boatItem().get());
            hangingSign(set.hangingSignItem().get(), set.strippedLog().get());
            generateRecipes(set.createBlockFamily(), FeatureFlags.DEFAULT_FLAGS);
        }

        @Override
        public void buildRecipes() {
        }
    }

    private static final class DirectLoot implements LootTableSubProvider {
        private final WoodSet set;
        private final LootBuilders builders;

        private DirectLoot(HolderLookup.Provider registries, WoodSet set) {
            this.set = set;
            this.builders = new LootBuilders(registries);
        }

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            for (BlockWithItem block : List.of(
                set.planks(), set.log(), set.strippedLog(), set.strippedWood(), set.wood(), set.sapling(),
                set.stairs(), set.fence(), set.fenceGate(), set.trapdoor(), set.pressurePlate(), set.button()
            ))
                accept(output, block.get(), builders.self(block.get()));
            accept(output, set.slab().get(), builders.slab(set.slab().get()));
            accept(output, set.door().get(), builders.door(set.door().get()));
            accept(output, set.sign().get(), builders.other(set.signItem().get()));
            accept(output, set.wallSign().get(), builders.other(set.signItem().get()));
            accept(output, set.hangingSign().get(), builders.other(set.hangingSignItem().get()));
            accept(output, set.wallHangingSign().get(), builders.other(set.hangingSignItem().get()));
            accept(output, set.leaves().get(), builders.leaves(set.leaves().get(), set.sapling().get()));
        }

        private static void accept(
            BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
            Block block,
            LootTable.Builder table
        ) {
            output.accept(block.getLootTable().orElseThrow(), table);
        }
    }

    /**
     * Uses vanilla's table builders without invoking its global block-validation pass.
     */
    private static final class LootBuilders extends BlockLootSubProvider {
        private LootBuilders(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }

        private LootTable.Builder self(Block block) {
            return createSingleItemTable(block);
        }

        private LootTable.Builder slab(Block block) {
            return createSlabItemTable(block);
        }

        private LootTable.Builder door(Block block) {
            return createDoorTable(block);
        }

        private LootTable.Builder other(Item item) {
            return createSingleItemTable(item);
        }

        private LootTable.Builder leaves(Block leaves, Block sapling) {
            return createLeavesDrops(leaves, sapling, NORMAL_LEAVES_SAPLING_CHANCES);
        }

        @Override
        public void generate() {
        }
    }
}
