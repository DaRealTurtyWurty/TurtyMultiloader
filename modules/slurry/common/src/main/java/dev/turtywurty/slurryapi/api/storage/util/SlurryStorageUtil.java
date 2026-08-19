package dev.turtywurty.slurryapi.api.storage.util;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.slurryapi.api.SlurryVariantAttributes;
import dev.turtywurty.turtymultiloader.transfer.StorageTransfer;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Neutral slurry-container interaction utilities with the original fill/empty sound hooks.
 */
public final class SlurryStorageUtil {
    private SlurryStorageUtil() {
    }

    public static boolean interactWithSlurryStorage(
        ResourceStorage<ResourceVariant<Slurry>> storage,
        ResourceStorage<ResourceVariant<Slurry>> heldItemStorage,
        Player player,
        Item handItem
    ) {
        return moveFirst(storage, heldItemStorage, player, handItem, true)
            || moveFirst(heldItemStorage, storage, player, handItem, false);
    }

    private static boolean moveFirst(
        ResourceStorage<ResourceVariant<Slurry>> source,
        ResourceStorage<ResourceVariant<Slurry>> target,
        Player player,
        Item handItem,
        boolean fill
    ) {
        for (int index = 0; index < source.size(); index++) {
            if (source.amount(index) == 0)
                continue;
            ResourceVariant<Slurry> resource = source.resource(index);
            if (StorageTransfer.move(source, target, resource, source.amount(index)) == 0)
                continue;
            SoundEvent sound = fill
                ? SlurryVariantAttributes.getFillSound(resource, handItem)
                : SlurryVariantAttributes.getEmptySound(resource, handItem);
            float pitch = 0.6F + player.getRandom().nextFloat() * 0.4F;
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound,
                SoundSource.PLAYERS, 1.0F, pitch);
            return true;
        }
        return false;
    }
}
