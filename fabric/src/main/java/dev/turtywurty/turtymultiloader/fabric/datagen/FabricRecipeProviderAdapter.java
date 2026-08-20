package dev.turtywurty.turtymultiloader.fabric.datagen;

import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adapts common recipe callbacks to Fabric's recipe output and condition support.
 */
final class FabricRecipeProviderAdapter extends FabricRecipeProvider {
    private final List<FunctionalRecipeProvider.RecipeGenerator> generators;

    FabricRecipeProviderAdapter(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registries,
        List<FunctionalRecipeProvider.RecipeGenerator> generators
    ) {
        super(output, registries);
        this.generators = List.copyOf(generators);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new FunctionalRecipeProvider(registries, output, (lookup, recipeOutput) ->
            generators.forEach(generator -> generator.generate(lookup, recipeOutput))
        );
    }

    @Override
    public String getName() {
        return "Recipes";
    }
}
