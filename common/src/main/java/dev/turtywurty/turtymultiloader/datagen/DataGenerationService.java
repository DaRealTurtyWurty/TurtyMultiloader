package dev.turtywurty.turtymultiloader.datagen;

import dev.turtywurty.turtymultiloader.datagen.convention.ConventionTag;
import net.minecraft.tags.TagKey;

import java.util.ServiceLoader;

/**
 * Loader backend for data-generation details which are not represented by vanilla APIs.
 */
public interface DataGenerationService {
    static DataGenerationService get() {
        return ServiceHolder.INSTANCE;
    }

    <T> TagKey<T> resolveConventionTag(ConventionTag<T> tag);

    final class ServiceHolder {
        private static final DataGenerationService INSTANCE = ServiceLoader.load(
                DataGenerationService.class,
                DataGenerationService.class.getClassLoader()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No data-generation service is available"));

        private ServiceHolder() {
        }
    }
}
