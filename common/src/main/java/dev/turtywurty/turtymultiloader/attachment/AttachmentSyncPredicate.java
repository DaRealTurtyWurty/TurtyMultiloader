package dev.turtywurty.turtymultiloader.attachment;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Selects which eligible clients receive a synchronized attachment.
 */
@FunctionalInterface
public interface AttachmentSyncPredicate {
    boolean test(AttachmentTarget target, ServerPlayer player);

    /**
     * Synchronizes to every player eligible to receive the holder (normally its tracking players).
     */
    static AttachmentSyncPredicate trackers() {
        return (target, player) -> true;
    }

    /**
     * Synchronizes only to the player that owns the attachment. Only player entity targets have an owner.
     */
    static AttachmentSyncPredicate owner() {
        return (target, player) -> target.kind() == AttachmentTarget.Kind.ENTITY && target.value() == player;
    }

    default AttachmentSyncPredicate and(AttachmentSyncPredicate other) {
        Objects.requireNonNull(other, "other");
        return (target, player) -> test(target, player) && other.test(target, player);
    }

    default AttachmentSyncPredicate or(AttachmentSyncPredicate other) {
        Objects.requireNonNull(other, "other");
        return (target, player) -> test(target, player) || other.test(target, player);
    }
}
