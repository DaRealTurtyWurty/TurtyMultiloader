package dev.turtywurty.turtymultiloader.datagen.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.Objects;

/**
 * A vanilla recipe provider backed by a common callback. Loader adapters own the native runner/output contract.
 */
public final class FunctionalRecipeProvider extends RecipeProvider {
    private final RecipeGenerator generator;

    public FunctionalRecipeProvider(
        HolderLookup.Provider registries,
        RecipeOutput output,
        RecipeGenerator generator
    ) {
        super(registries, output);
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @Override
    public void buildRecipes() {
        generator.generate(registries, output);
    }

    @FunctionalInterface
    public interface RecipeGenerator {
        void generate(HolderLookup.Provider registries, RecipeOutput output);
    }
}
