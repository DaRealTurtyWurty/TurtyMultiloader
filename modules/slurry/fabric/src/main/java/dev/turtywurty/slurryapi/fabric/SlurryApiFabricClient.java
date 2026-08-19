package dev.turtywurty.slurryapi.fabric;

import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.slurryapi.client.SlurryRenderHandlerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SlurryApiFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return SlurryApi.id("slurry_textures");
                }

                @Override
                public void onResourceManagerReload(ResourceManager manager) {
                    SlurryRenderHandlerRegistry.onResourcesReload();
                }
            }
        );
    }
}
