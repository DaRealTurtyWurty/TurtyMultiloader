package dev.turtywurty.slurryapi.api;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.Optional;

@FunctionalInterface
public interface SlurryVariantAttributeHandler {
    Component getName(ResourceVariant<Slurry> slurryVariant);

    default Optional<SoundEvent> getFillSound(ResourceVariant<Slurry> slurryVariant, Item handItem) {
        return Optional.empty();
    }

    default Optional<SoundEvent> getEmptySound(ResourceVariant<Slurry> slurryVariant, Item handItem) {
        return Optional.empty();
    }
}
