package dev.turtywurty.turtymultiloader.transfer.resource;

import com.mojang.serialization.Codec;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKey;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.UnitDimension;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Complete declaration for a registry-backed resource family owned by a consuming mod.
 *
 * <p>The caller supplies its registry and corresponding vanilla holder codecs. The resulting type,
 * lookup key, and codecs cannot accidentally disagree.</p>
 */
public record ResourceFamily<T>(
    ResourceType<T> type,
    StorageKey<ResourceVariant<T>> storageKey,
    Codec<ResourceVariant<T>> codec,
    StreamCodec<RegistryFriendlyByteBuf, ResourceVariant<T>> streamCodec
) {
    public ResourceFamily {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(streamCodec, "streamCodec");
        if (storageKey.resourceType() != type)
            throw new IllegalArgumentException("Storage key and resource type do not match");
    }

    public static <T> ResourceFamily<T> registered(
        Identifier id,
        ResourceKey<? extends Registry<T>> registryKey,
        UnitDimension unitDimension,
        Predicate<? super T> emptyPredicate,
        Supplier<? extends Holder<T>> emptyHolder,
        TransferUnit storageUnit,
        Codec<Holder<T>> holderCodec,
        StreamCodec<? super RegistryFriendlyByteBuf, Holder<T>> holderStreamCodec
    ) {
        Objects.requireNonNull(holderCodec, "holderCodec");
        Objects.requireNonNull(holderStreamCodec, "holderStreamCodec");
        ResourceType<T> type = ResourceType.registered(id, registryKey, unitDimension,
            emptyPredicate, emptyHolder);
        return new ResourceFamily<>(
            type,
            StorageKeys.key(id, type, storageUnit),
            ResourceVariantCodecs.codec(type, holderCodec),
            ResourceVariantCodecs.streamCodec(type, holderStreamCodec)
        );
    }
}
