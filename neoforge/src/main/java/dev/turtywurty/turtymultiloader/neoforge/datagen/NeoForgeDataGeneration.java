package dev.turtywurty.turtymultiloader.neoforge.datagen;

import dev.turtywurty.turtymultiloader.datagen.DataGenerationSide;
import dev.turtywurty.turtymultiloader.datagen.DataGenerationSpec;
import dev.turtywurty.turtymultiloader.datagen.DataProviderContext;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

import java.util.Objects;

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

        run(event, spec, DataGenerationSide.SERVER);
    }

    /** Collect registry entries before either client or server providers are constructed. */
    public static void run(GatherDataRegistryEntriesEvent event, DataGenerationSpec spec) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(spec, "spec");
        if (!event.getModContainer().getModId().equals(spec.modId()))
            throw new IllegalArgumentException("Data-generation spec belongs to another mod");
        spec.addRegistryBootstraps(new RegistrySetBuilder() {
            @Override
            public <T> RegistrySetBuilder add(
                ResourceKey<? extends Registry<T>> key,
                SingleRegistryBootstrap<T> bootstrap
            ) {
                event.add(key, bootstrap);
                return this;
            }
            @Override
            public RegistrySetBuilder add(MultiRegistryBootstrap bootstrap) {
                event.add(bootstrap);
                return this;
            }
        });
    }

    private static void run(GatherDataEvent event, DataGenerationSpec spec, DataGenerationSide side) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(spec, "spec");
        validateOwner(event, spec);

        var context = new DataProviderContext(
            spec.modId(),
            event.getGenerator().getPackOutput(),
            event.getDefaultPackGenerator().getReloadableLookupProvider());
        spec.providers().stream()
            .filter(declaration -> declaration.side() == side)
            .map(declaration -> declaration.factory().create(context))
            .forEach(event::addProvider);
    }

    private static void validateOwner(GatherDataEvent event, DataGenerationSpec spec) {
        String eventModId = event.getModContainer().getModId();
        if (!eventModId.equals(spec.modId()))
            throw new IllegalArgumentException(
                "Data-generation spec for " + spec.modId() + " cannot run for NeoForge mod " + eventModId);
    }
}
