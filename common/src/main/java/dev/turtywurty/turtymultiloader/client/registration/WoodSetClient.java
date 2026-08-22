package dev.turtywurty.turtymultiloader.client.registration;

import dev.turtywurty.turtymultiloader.registration.WoodSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Client-side registrations required by a {@link WoodSet}.
 */
public final class WoodSetClient {
    private WoodSetClient() {
    }

    public static void register(WoodSet woodSet) {
        Objects.requireNonNull(woodSet, "woodSet");
        ModelLayerLocation boatLayer = layer(woodSet, "boat");
        ModelLayerLocation chestBoatLayer = layer(woodSet, "chest_boat");
        ClientRegistrations.registerModelLayer(boatLayer, BoatModel::createBoatModel);
        ClientRegistrations.registerModelLayer(chestBoatLayer, BoatModel::createChestBoatModel);
        ClientRegistrations.registerEntityRenderer(
            woodSet.boatEntityType(), context -> new WoodSetBoatRenderer(context, boatLayer)
        );
        ClientRegistrations.registerEntityRenderer(
            woodSet.chestBoatEntityType(), context -> new WoodSetBoatRenderer(context, chestBoatLayer)
        );
    }

    private static ModelLayerLocation layer(WoodSet woodSet, String directory) {
        Identifier model = Identifier.fromNamespaceAndPath(
            woodSet.id().getNamespace(), directory + "/" + woodSet.id().getPath()
        );
        return new ModelLayerLocation(model, "main");
    }
}
