package dev.turtywurty.multiblocklib.port.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.turtywurty.multiblocklib.data.PortIO;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.SimpleSingleSlotStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MultiblockFluidPortStorage extends SimpleSingleSlotStorage<ResourceVariant<Fluid>> {
    private static final Codec<ResourceVariant<Fluid>> LEGACY_VARIANT_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid").forGetter(ResourceVariant::holder),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                .forGetter(ResourceVariant::components)
        ).apply(instance, (holder, components) -> ResourceVariant.of(ResourceTypes.FLUID, holder, components))
    );

    public MultiblockFluidPortStorage(final long capacity, final PortIO io, final Runnable onChange) {
        super(ResourceTypes.FLUID, capacity, TransferSupport.BOTH, variant -> true, storage -> onChange.run());
    }

    public void readValue(final ValueInput value) {
        ResourceVariant<Fluid> resource = value.read("variant", LEGACY_VARIANT_CODEC)
            .orElse(ResourceTypes.FLUID.empty());
        long amount = Math.min(value.getLongOr("amount", 0L), capacity(0, resource));
        try (TransferTransaction transaction = TransferTransaction.openRoot()) {
            set(resource, amount, transaction);
            transaction.commit();
        }
    }

    public void writeValue(final ValueOutput value) {
        value.store("variant", LEGACY_VARIANT_CODEC, getResource());
        value.putLong("amount", getAmount());
    }
}
