package dev.turtywurty.turtymultiloader.transfer.resource;

import com.mojang.serialization.Codec;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.UnitDimension;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Core resource families and the generic factory used by optional chemical modules.
 */
public final class ResourceTypes {
    public static final ResourceType<Item> ITEM = ResourceType.registered(
        id("item"), Registries.ITEM, UnitDimension.ITEM,
        item -> item == Items.AIR, () -> Items.AIR.builtInRegistryHolder()
    );
    public static final ResourceType<Fluid> FLUID = ResourceType.registered(
        id("fluid"), Registries.FLUID, UnitDimension.FLUID,
        fluid -> fluid == Fluids.EMPTY, () -> Fluids.EMPTY.builtInRegistryHolder()
    );
    public static final ResourceType<UnitResource> ENERGY = scalar("energy");

    private ResourceTypes() {
    }

    public static <T> ResourceFamily<T> chemical(
        Identifier id,
        ResourceKey<? extends Registry<T>> registryKey,
        UnitDimension unitDimension,
        Predicate<? super T> emptyPredicate,
        Supplier<? extends Holder<T>> emptyHolder,
        TransferUnit unit,
        Codec<Holder<T>> holderCodec,
        StreamCodec<? super RegistryFriendlyByteBuf, Holder<T>> holderStreamCodec
    ) {
        return ResourceFamily.registered(id, registryKey, unitDimension, emptyPredicate, emptyHolder,
            unit, holderCodec, holderStreamCodec);
    }

    private static ResourceType<UnitResource> scalar(String path) {
        return ResourceType.direct(id(path), UnitDimension.ENERGY,
            value -> value == UnitResource.EMPTY, () -> UnitResource.EMPTY);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("turtymultiloader", path);
    }
}
