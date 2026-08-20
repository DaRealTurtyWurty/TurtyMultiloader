package dev.turtywurty.turtymultiloader.neoforge.datagen;

import dev.turtywurty.turtymultiloader.datagen.DataGenerationSide;
import dev.turtywurty.turtymultiloader.datagen.DataGenerationSpec;
import dev.turtywurty.turtymultiloader.datagen.DataProviderContext;
import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Objects;
import java.util.Set;

/**
 * NeoForge adapters for a common {@link DataGenerationSpec}.
 */
public final class NeoForgeDataGeneration {
    private NeoForgeDataGeneration() {
    }

    public static void run(GatherDataEvent.Client event, DataGenerationSpec spec) {
        run(event, spec, DataGenerationSide.CLIENT);
    }

    public static void run(GatherDataEvent.Server event, DataGenerationSpec spec) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(spec, "spec");
        validateOwner(event, spec);

        if (spec.generatesDynamicRegistries()) {
            RegistrySetBuilder builder = new RegistrySetBuilder();
            spec.addRegistryBootstraps(builder);
            event.createDatapackRegistryObjects(builder, Set.of(spec.modId()));
        }
        run(event, spec, DataGenerationSide.SERVER);
    }

    private static void run(GatherDataEvent event, DataGenerationSpec spec, DataGenerationSide side) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(spec, "spec");
        validateOwner(event, spec);

        DataProviderContext context = new DataProviderContext(
            spec.modId(),
            event.getGenerator().getPackOutput(),
            event.getLookupProvider()
        );
        spec.providers().stream()
            .filter(declaration -> declaration.side() == side)
            .map(declaration -> declaration.factory().create(context))
            .forEach(event::addProvider);
        if (side == DataGenerationSide.SERVER && !spec.recipeGenerators().isEmpty()) {
            event.addProvider(new NeoForgeRecipeProviderAdapter(
                context.output(),
                context.registries(),
                spec.recipeGenerators()
            ));
        }
    }

    private static void validateOwner(GatherDataEvent event, DataGenerationSpec spec) {
        String eventModId = event.getModContainer().getModId();
        if (!eventModId.equals(spec.modId())) {
            throw new IllegalArgumentException(
                "Data-generation spec for " + spec.modId() + " cannot run for NeoForge mod " + eventModId
            );
        }
    }
}
