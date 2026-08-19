package dev.turtywurty.slurryapi.neoforge;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.client.SlurryRenderHandlerRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

final class SlurryApiNeoForgeClient {
    private SlurryApiNeoForgeClient() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(AddClientReloadListenersEvent.class, SlurryApiNeoForgeClient::addReloadListeners);
    }

    private static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(SlurryApi.id("slurry_textures"), new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void prepared, ResourceManager manager, ProfilerFiller profiler) {
                SlurryRenderHandlerRegistry.onResourcesReload();
            }
        });
    }
}
