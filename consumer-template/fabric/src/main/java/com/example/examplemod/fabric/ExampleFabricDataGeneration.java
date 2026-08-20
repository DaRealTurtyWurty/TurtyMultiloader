package com.example.examplemod.fabric;

import com.example.examplemod.ExampleDataGeneration;
import dev.turtywurty.turtymultiloader.datagen.DataGeneration;
import dev.turtywurty.turtymultiloader.fabric.datagen.FabricDataGeneration;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;

public final class ExampleFabricDataGeneration implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGeneration.run(generator, ExampleDataGeneration.SPEC);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder builder) {
        DataGeneration.addRegistryBootstraps(ExampleDataGeneration.SPEC, builder);
    }
}

