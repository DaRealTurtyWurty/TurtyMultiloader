package dev.turtywurty.turtymultiloader.transfer.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * Codecs keep Holder and DataComponentPatch visible instead of flattening registry identity.
 */
public final class ResourceVariantCodecs {
    public static final Codec<ResourceVariant<Item>> ITEM = codec(ResourceTypes.ITEM, Item.CODEC);
    public static final Codec<ResourceVariant<Fluid>> FLUID = codec(
        ResourceTypes.FLUID,
        BuiltInRegistries.FLUID.holderByNameCodec()
    );
    public static final Codec<ResourceVariant<UnitResource>> ENERGY = codec(
        ResourceTypes.ENERGY,
        Codec.BOOL.xmap(
            value -> Holder.direct(value ? UnitResource.VALUE : UnitResource.EMPTY),
            holder -> holder.value() == UnitResource.VALUE
        )
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<Item>> ITEM_STREAM = streamCodec(
        ResourceTypes.ITEM,
        ByteBufCodecs.holderRegistry(Registries.ITEM)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<Fluid>> FLUID_STREAM = streamCodec(
        ResourceTypes.FLUID,
        ByteBufCodecs.holderRegistry(Registries.FLUID)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<UnitResource>> ENERGY_STREAM = streamCodec(
        ResourceTypes.ENERGY,
        ByteBufCodecs.BOOL.map(
            value -> Holder.direct(value ? UnitResource.VALUE : UnitResource.EMPTY),
            holder -> holder.value() == UnitResource.VALUE
        )
    );

    private ResourceVariantCodecs() {
    }

    public static <T> Codec<ResourceVariant<T>> codec(ResourceType<T> type, Codec<Holder<T>> holderCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
            holderCodec.fieldOf("id").forGetter(ResourceVariant::holder),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                .forGetter(ResourceVariant::components)
        ).apply(instance, (holder, components) -> ResourceVariant.of(type, holder, components)));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<T>> streamCodec(
        ResourceType<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, Holder<T>> holderCodec
    ) {
        return StreamCodec.composite(
            holderCodec, ResourceVariant::holder,
            DataComponentPatch.STREAM_CODEC, ResourceVariant::components,
            (holder, components) -> ResourceVariant.of(type, holder, components)
        );
    }
}
