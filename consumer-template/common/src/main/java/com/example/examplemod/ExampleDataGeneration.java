package com.example.examplemod;

import dev.turtywurty.turtymultiloader.datagen.DataGeneration;
import dev.turtywurty.turtymultiloader.datagen.DataGenerationSpec;

public final class ExampleDataGeneration {
    public static final DataGenerationSpec SPEC = DataGeneration.spec(ExampleMod.MOD_ID).build();

    private ExampleDataGeneration() {
    }
}

