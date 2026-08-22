package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader-neutral configuration for a custom creative tab.
 *
 * <p>The registry backends apply this configuration to the loader's custom-tab builder. In particular, Fabric
 * custom tabs cannot be created with vanilla's positioned-tab builder.</p>
 */
public final class CreativeTabBuilder {
    private Component title;
    private Supplier<ItemStack> icon = () -> ItemStack.EMPTY;
    private Consumer<CreativeTabOutput> displayItems = ignored -> {
    };
    private boolean alignedRight;
    private boolean hideTitle;
    private boolean noScrollBar;
    private Identifier backgroundTexture;

    public CreativeTabBuilder(Identifier id) {
        Objects.requireNonNull(id, "id");
        this.title = Component.translatable("itemGroup." + id.getNamespace() + "." + id.getPath());
    }

    public CreativeTabBuilder title(Component title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    public CreativeTabBuilder icon(Supplier<ItemStack> icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
        return this;
    }

    public CreativeTabBuilder displayItems(Consumer<CreativeTabOutput> displayItems) {
        this.displayItems = Objects.requireNonNull(displayItems, "displayItems");
        return this;
    }

    public CreativeTabBuilder alignedRight() {
        this.alignedRight = true;
        return this;
    }

    public CreativeTabBuilder hideTitle() {
        this.hideTitle = true;
        return this;
    }

    public CreativeTabBuilder noScrollBar() {
        this.noScrollBar = true;
        return this;
    }

    public CreativeTabBuilder backgroundTexture(Identifier backgroundTexture) {
        this.backgroundTexture = Objects.requireNonNull(backgroundTexture, "backgroundTexture");
        return this;
    }

    /**
     * Returns the configured item population callback. Intended for registry backends.
     */
    public Consumer<CreativeTabOutput> displayItems() {
        return displayItems;
    }

    /**
     * Applies the loader-independent builder properties. Intended for registry backends.
     */
    public void configure(CreativeModeTab.Builder builder) {
        Objects.requireNonNull(builder, "builder")
            .title(title)
            .icon(icon);
        if (alignedRight)
            builder.alignedRight();
        if (hideTitle)
            builder.hideTitle();
        if (noScrollBar)
            builder.noScrollBar();
        if (backgroundTexture != null)
            builder.backgroundTexture(backgroundTexture);
    }
}
