package dev.turtywurty.turtymultiloader.transfer.resource;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Immutable resource identity: a typed vanilla holder and its component patch, without an amount.
 */
public record ResourceVariant<T>(ResourceType<T> type, Holder<T> holder, DataComponentPatch components) {
    public ResourceVariant {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(holder, "holder");
        Objects.requireNonNull(components, "components");
        if (holder.isBound() && type.isEmpty(holder.value()))
            components = DataComponentPatch.EMPTY;
    }

    public static <T> ResourceVariant<T> of(ResourceType<T> type, Holder<T> holder) {
        return new ResourceVariant<>(type, holder, DataComponentPatch.EMPTY);
    }

    public static <T> ResourceVariant<T> of(ResourceType<T> type, Holder<T> holder, DataComponentPatch components) {
        return new ResourceVariant<>(type, holder, components);
    }

    public static ResourceVariant<Item> ofItem(ItemStack stack) {
        if (stack.isEmpty())
            return ResourceTypes.ITEM.empty();
        return of(ResourceTypes.ITEM, stack.typeHolder(), stack.getComponentsPatch());
    }

    public T value() {
        return this.holder.value();
    }

    public boolean isBlank() {
        return this.type.isEmpty(value());
    }

    public boolean hasComponents() {
        return !this.components.isEmpty();
    }

    public ResourceVariant<T> withComponents(DataComponentPatch components) {
        return new ResourceVariant<>(this.type, this.holder, components);
    }
}
