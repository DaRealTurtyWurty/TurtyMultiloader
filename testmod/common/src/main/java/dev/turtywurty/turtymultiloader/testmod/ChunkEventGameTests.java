package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.event.Events;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.concurrent.atomic.AtomicReference;

public final class ChunkEventGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID, "chunk_events");

    private ChunkEventGameTests() {
    }

    public static void verifyChunkEvents(GameTestHelper helper) {
        var level = helper.getLevel();
        var loaded = new AtomicReference<LevelChunk>();
        var unloaded = new AtomicReference<LevelChunk>();
        // Outside the GameTest arena's tickets, so vanilla can unload it naturally.
        int chunkX = 100;
        int chunkZ = 100;
        Events.onChunkLoad((eventLevel, chunk) -> {
            if (eventLevel == level && chunk.getPos().x() == chunkX && chunk.getPos().z() == chunkZ) {
                loaded.set(chunk);
            }
        });
        Events.onChunkUnload((eventLevel, chunk) -> {
            if (eventLevel == level && chunk.getPos().x() == chunkX && chunk.getPos().z() == chunkZ) {
                unloaded.set(chunk);
            }
        });

        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        helper.assertTrue(loaded.get() == chunk, "Chunk load callback did not receive the server level and chunk");
        helper.succeedWhen(() -> helper.assertTrue(
            unloaded.get() == chunk, "Chunk unload callback did not receive the server level and chunk"));
    }
}
