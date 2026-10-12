package dev.turtywurty.turtymultiloader.datagen.provider;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A recipe provider backed by the recipe and advancement registry bootstraps. */
public final class FunctionalRecipeProvider extends RecipeProvider {
    private final BootstrapContext<Recipe<?>> recipes;
    private final BootstrapContext<Advancement> advancements;
    private final RecipeGenerator generator;

    public FunctionalRecipeProvider(BootstrapContext<Recipe<?>> recipes,
        BootstrapContext<Advancement> advancements, RecipeGenerator generator) {
        super(recipes, advancements);
        this.recipes = recipes;
        this.advancements = advancements;
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @Override
    public void buildRecipes() {
        generator.generate(recipes, advancements);
    }

    public static MultiRegistryBootstrap bootstrap(List<RecipeGenerator> generators) {
        List<RecipeGenerator> callbacks = List.copyOf(generators);
        return new MultiRegistryBootstrap() {
            @Override
            public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                return Set.of(Registries.RECIPE, Registries.ADVANCEMENT);
            }
            @Override
            public void run(BootstrapGetter registries) {
                callbacks.forEach(callback -> callback.generate(
                    registries.get(Registries.RECIPE), registries.get(Registries.ADVANCEMENT)
                ));
            }
        };
    }

    @FunctionalInterface
    public interface RecipeGenerator {
        void generate(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements);
    }
}
