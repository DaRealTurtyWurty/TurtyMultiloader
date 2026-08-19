package dev.turtywurty.gasapi.api;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;

@FunctionalInterface
public interface GasVariantAttributeHandler {
    Component getName(ResourceVariant<Gas> gasVariant);
}
