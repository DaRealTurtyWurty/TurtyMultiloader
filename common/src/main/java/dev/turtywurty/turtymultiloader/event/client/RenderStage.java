package dev.turtywurty.turtymultiloader.event.client;

/**
 * Render stages shared by Fabric and NeoForge that Industria currently consumes.
 */
public enum RenderStage {
    /**
     * Submit geometry to {@link LevelRenderContext#submitNodeCollector()}.
     */
    COLLECT_SUBMITS,
    /**
     * Render after opaque entities, block entities, and particles.
     */
    AFTER_SOLID_FEATURES
}
