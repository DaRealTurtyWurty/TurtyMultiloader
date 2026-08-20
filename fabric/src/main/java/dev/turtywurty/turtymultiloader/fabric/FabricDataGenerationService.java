package dev.turtywurty.turtymultiloader.fabric;

import dev.turtywurty.turtymultiloader.datagen.DataGenerationService;
import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTag;
import net.minecraft.tags.TagKey;

public final class FabricDataGenerationService implements DataGenerationService {
    @Override
    public <T> TagKey<T> resolveConventionTag(ConventionTag<T> tag) {
        return TagKey.create(tag.registry(), tag.fabricId());
    }
}
