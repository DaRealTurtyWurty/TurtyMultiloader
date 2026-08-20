package com.example.examplemod.neoforge;

import com.example.examplemod.ExampleDataGeneration;
import com.example.examplemod.ExampleMod;
import dev.turtywurty.turtymultiloader.neoforge.datagen.NeoForgeDataGeneration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(ExampleMod.MOD_ID)
public final class ExampleNeoForge {
    public ExampleNeoForge(IEventBus modBus) {
        ExampleMod.init();
        modBus.addListener(GatherDataEvent.Client.class,
            event -> NeoForgeDataGeneration.run(event, ExampleDataGeneration.SPEC));
        modBus.addListener(GatherDataEvent.Server.class,
            event -> NeoForgeDataGeneration.run(event, ExampleDataGeneration.SPEC));
    }
}

