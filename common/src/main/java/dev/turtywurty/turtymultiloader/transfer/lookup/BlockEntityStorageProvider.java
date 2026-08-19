package dev.turtywurty.turtymultiloader.transfer.lookup;

import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface BlockEntityStorageProvider<BE extends BlockEntity, V extends ResourceVariant<?>> {
    ResourceStorage<V> find(BE blockEntity, Direction side);
}
