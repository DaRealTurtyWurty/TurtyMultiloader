package dev.turtywurty.turtymultiloader.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TestMenu extends AbstractContainerMenu {
    private final BlockPos openingPos;

    public TestMenu(int containerId, Inventory inventory, BlockPos openingPos) {
        super(TestModContent.TEST_MENU.get(), containerId);
        this.openingPos = openingPos.immutable();
    }

    public BlockPos openingPos() {
        return openingPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
