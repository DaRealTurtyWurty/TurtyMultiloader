package dev.turtywurty.turtymultiloader.transfer.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public final class StorageCodecs {
    private StorageCodecs() {
    }

    public static <V extends ResourceVariant<?>> Codec<StorageSnapshot<V>> codec(Codec<V> resourceCodec) {
        Codec<StorageSnapshot.Entry<V>> entryCodec = RecordCodecBuilder.create(instance -> instance.group(
            resourceCodec.fieldOf("resource").forGetter(StorageSnapshot.Entry::resource),
            Codec.LONG.fieldOf("amount").forGetter(StorageSnapshot.Entry::amount)
        ).apply(instance, StorageSnapshot.Entry::new));
        return entryCodec.listOf().xmap(StorageSnapshot::new, StorageSnapshot::entries);
    }

    public static <V extends ResourceVariant<?>> StreamCodec<RegistryFriendlyByteBuf, StorageSnapshot<V>> streamCodec(
        StreamCodec<? super RegistryFriendlyByteBuf, V> resourceCodec
    ) {
        return new StreamCodec<>() {
            @Override
            public StorageSnapshot<V> decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                List<StorageSnapshot.Entry<V>> entries = new ArrayList<>(size);
                for (int index = 0; index < size; index++)
                    entries.add(new StorageSnapshot.Entry<>(resourceCodec.decode(buffer), buffer.readVarLong()));
                return new StorageSnapshot<>(entries);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, StorageSnapshot<V> snapshot) {
                buffer.writeVarInt(snapshot.entries().size());
                for (StorageSnapshot.Entry<V> entry : snapshot.entries()) {
                    resourceCodec.encode(buffer, entry.resource());
                    buffer.writeVarLong(entry.amount());
                }
            }
        };
    }
}
