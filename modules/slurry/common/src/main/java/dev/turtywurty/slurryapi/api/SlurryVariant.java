package dev.turtywurty.slurryapi.api;

import com.mojang.serialization.Codec;
import dev.turtywurty.slurryapi.SlurryApi;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Factory and codecs for the neutral slurry variant type.
 */
public final class SlurryVariant {
    public static final Codec<ResourceVariant<Slurry>> CODEC = SlurryApi.RESOURCE_FAMILY.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<Slurry>> STREAM_CODEC =
        SlurryApi.RESOURCE_FAMILY.streamCodec();

    private SlurryVariant() {
    }

    public static ResourceVariant<Slurry> blank() {
        return SlurryApi.RESOURCE_FAMILY.type().empty();
    }

    public static ResourceVariant<Slurry> of(Holder<Slurry> slurry) {
        return SlurryApi.RESOURCE_FAMILY.type().of(slurry);
    }

    public static ResourceVariant<Slurry> of(Holder<Slurry> slurry, DataComponentPatch components) {
        return ResourceVariant.of(SlurryApi.RESOURCE_FAMILY.type(), slurry, components);
    }
}
