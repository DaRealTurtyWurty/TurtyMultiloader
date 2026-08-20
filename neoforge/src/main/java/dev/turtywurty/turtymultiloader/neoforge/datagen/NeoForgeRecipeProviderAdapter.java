package dev.turtywurty.turtymultiloader.neoforge.datagen;

import dev.turtywurty.turtymultiloader.datagen.provider.FunctionalRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adapts common recipe callbacks to NeoForge's condition-aware recipe output.
 */
final class NeoForgeRecipeProviderAdapter extends RecipeProvider.Runner {
    private final List<FunctionalRecipeProvider.RecipeGenerator> generators;

    NeoForgeRecipeProviderAdapter(
        PackOutput output,
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
