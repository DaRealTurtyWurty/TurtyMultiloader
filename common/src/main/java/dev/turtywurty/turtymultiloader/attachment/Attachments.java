package dev.turtywurty.turtymultiloader.attachment;

import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * Entry point for declaring loader-neutral attachment types.
 */
public final class Attachments {
    private Attachments() {
    }

    public static <T> AttachmentType<T> register(
        Identifier id,
        Consumer<AttachmentType.Builder<T>> configuration
    ) {
        return AttachmentService.get().register(id, configuration);
    }
}
