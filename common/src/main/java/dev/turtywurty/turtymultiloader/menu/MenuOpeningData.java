package dev.turtywurty.turtymultiloader.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Common opening-data helpers used by client menu factories.
 */
public final class MenuOpeningData {
    private MenuOpeningData() {
    }

    public static <T extends BlockEntity> T requireBlockEntity(
        Inventory inventory,
        BlockPos pos,
        Class<T> blockEntityType
    ) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(blockEntityType, "blockEntityType");

        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (!blockEntityType.isInstance(blockEntity)) {
            String actual = blockEntity == null ? "none" : blockEntity.getClass().getName();
            throw new IllegalStateException(
                "Expected " + blockEntityType.getName() + " at " + pos + ", found " + actual
            );
        }

        return blockEntityType.cast(blockEntity);
    }
}
