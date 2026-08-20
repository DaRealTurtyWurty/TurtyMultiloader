package dev.turtywurty.turtymultiloader.datagen.language;

import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Objects;

/**
 * Collects entries for one language file.
 */
public final class LanguageGenerationContext {
    private final Map<String, String> translations;

    public LanguageGenerationContext(Map<String, String> translations) {
        this.translations = Objects.requireNonNull(translations, "translations");
    }

    public void add(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        String previous = translations.putIfAbsent(key, value);
        if (previous != null)
            throw new IllegalArgumentException("Duplicate translation key: " + key);
    }

    public void add(Block block, String value) {
        add(Objects.requireNonNull(block, "block").getDescriptionId(), value);
    }

    public void add(Item item, String value) {
        add(Objects.requireNonNull(item, "item").getDescriptionId(), value);
    }

    public void add(EntityType<?> entityType, String value) {
        add(Objects.requireNonNull(entityType, "entityType").getDescriptionId(), value);
    }

    public void add(TagKey<?> tag, String value) {
        Objects.requireNonNull(tag, "tag");
        String registry = tag.registry().identifier().toShortLanguageKey().replace('/', '.');
        String path = tag.location().getPath().replace('/', '.');
        add("tag." + registry + "." + tag.location().getNamespace() + "." + path, value);
    }

    public void add(ConventionTag<?> tag, String value) {
        add(Objects.requireNonNull(tag, "tag").key(), value);
    }
}
