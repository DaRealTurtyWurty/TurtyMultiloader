package dev.turtywurty.turtymultiloader.event.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

@FunctionalInterface
public interface TooltipCallback {
    void buildTooltip(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipFlag flag,
        List<Component> lines
    );
}
