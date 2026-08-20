package dev.turtywurty.turtymultiloader.testmod;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TestMenuScreen extends AbstractContainerScreen<TestMenu> {
    public TestMenuScreen(TestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
