package dev.turtywurty.turtymultiloader.testmod.fabric;

import dev.turtywurty.turtymultiloader.datagen.DataGeneration;
import dev.turtywurty.turtymultiloader.fabric.datagen.FabricDataGeneration;
import dev.turtywurty.turtymultiloader.testmod.TestDataGeneration;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;

public final class TestModFabricDataGeneration implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGeneration.run(generator, TestDataGeneration.SPEC);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder builder) {
        DataGeneration.addRegistryBootstraps(TestDataGeneration.SPEC, builder);
    }
}
