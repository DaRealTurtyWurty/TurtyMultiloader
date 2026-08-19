package dev.turtywurty.gasapi.api;

import com.mojang.serialization.Codec;
import dev.turtywurty.gasapi.GasApi;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Factory and codecs for the neutral gas variant type. */
public final class GasVariant {
    public static final Codec<ResourceVariant<Gas>> CODEC = GasApi.RESOURCE_FAMILY.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<Gas>> STREAM_CODEC =
        GasApi.RESOURCE_FAMILY.streamCodec();

    private GasVariant() {
    }

    public static ResourceVariant<Gas> blank() {
        return GasApi.RESOURCE_FAMILY.type().empty();
    }

    public static ResourceVariant<Gas> of(Holder<Gas> gas) {
        return GasApi.RESOURCE_FAMILY.type().of(gas);
    }

    public static ResourceVariant<Gas> of(Holder<Gas> gas, DataComponentPatch components) {
        return ResourceVariant.of(GasApi.RESOURCE_FAMILY.type(), gas, components);
    }
}
