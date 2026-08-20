package com.example.examplemod.fabric;

import com.example.examplemod.ExampleMod;
import net.fabricmc.api.ModInitializer;

public final class ExampleFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.init();
    }
}

