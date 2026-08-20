package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.attachment.AttachmentTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class AttachmentGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(
        TestModContent.MOD_ID,
        "attachment_service"
    );

    private AttachmentGameTests() {
    }

    public static void verifyAttachmentService(GameTestHelper helper) {
        helper.assertTrue(TestModContent.TEST_COUNTER.isPersistent(), "Persistent attachment metadata");
        helper.assertTrue(TestModContent.TEST_COUNTER.isSynchronized(), "Synchronized attachment metadata");
        helper.assertTrue(TestModContent.TEST_COUNTER.copyOnDeath(), "Copy-on-death attachment metadata");
        helper.assertTrue(!TestModContent.TEST_MUTABLE.isPersistent(), "Transient attachment metadata");

        var levelTarget = AttachmentTarget.level(helper.getLevel());
        levelTarget.remove(TestModContent.TEST_COUNTER);
        helper.assertTrue(levelTarget.get(TestModContent.TEST_COUNTER).isEmpty(), "Removed level attachment remained");
        helper.assertValueEqual(levelTarget.getOrCreate(TestModContent.TEST_COUNTER), 0, "Default attachment value");
        helper.assertValueEqual(
            levelTarget.set(TestModContent.TEST_COUNTER, 4).orElseThrow(),
            0,
            "Previous attachment value"
        );
        helper.assertValueEqual(
            levelTarget.update(TestModContent.TEST_COUNTER, value -> value + 3),
            7,
            "Updated attachment value"
        );

        var entityTarget = AttachmentTarget.entity(helper.makeMockPlayer(GameType.SURVIVAL));
        entityTarget.set(TestModContent.TEST_COUNTER, 11);
        helper.assertValueEqual(entityTarget.get(TestModContent.TEST_COUNTER).orElseThrow(), 11, "Entity attachment");

        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        var chunkTarget = AttachmentTarget.chunk(helper.getLevel().getChunkAt(position));
        chunkTarget.set(TestModContent.TEST_COUNTER, 13);
        helper.assertValueEqual(chunkTarget.get(TestModContent.TEST_COUNTER).orElseThrow(), 13, "Chunk attachment");

        BlockPos blockEntityPosition = new BlockPos(2, 1, 1);
        helper.setBlock(blockEntityPosition, Blocks.CHEST);
        ChestBlockEntity blockEntity = helper.getBlockEntity(blockEntityPosition, ChestBlockEntity.class);
        var blockEntityTarget = AttachmentTarget.blockEntity(blockEntity);
        blockEntityTarget.set(TestModContent.TEST_COUNTER, 15);
        helper.assertValueEqual(blockEntityTarget.get(TestModContent.TEST_COUNTER).orElseThrow(), 15,
            "Block entity attachment");

        var serverTarget = AttachmentTarget.server(helper.getLevel().getServer());
        serverTarget.remove(TestModContent.TEST_GLOBAL_COUNTER);
        serverTarget.set(TestModContent.TEST_GLOBAL_COUNTER, 19);
        helper.assertValueEqual(serverTarget.get(TestModContent.TEST_GLOBAL_COUNTER).orElseThrow(), 19,
            "Server-global attachment");

        var listTarget = AttachmentTarget.level(helper.getLevel());
        listTarget.remove(TestModContent.TEST_MUTABLE);
        listTarget.mutate(TestModContent.TEST_MUTABLE, values -> values.add(17));
        helper.assertValueEqual(listTarget.getOrCreate(TestModContent.TEST_MUTABLE).getFirst(), 17,
            "Mutable attachment notification");
        listTarget.remove(TestModContent.TEST_MUTABLE);

        TestModContent.TEST_WORLD_STATE.access(helper.getLevel()).set(21);
        helper.assertValueEqual(TestModContent.TEST_WORLD_STATE.access(helper.getLevel()).get(), 21,
            "World saved state");
        TestModContent.TEST_SERVER_STATE.access(helper.getLevel().getServer()).set(34);
        helper.assertValueEqual(TestModContent.TEST_SERVER_STATE.access(helper.getLevel().getServer()).get(), 34,
            "Server saved state");

        entityTarget.remove(TestModContent.TEST_COUNTER);
        chunkTarget.remove(TestModContent.TEST_COUNTER);
        blockEntityTarget.remove(TestModContent.TEST_COUNTER);
        serverTarget.remove(TestModContent.TEST_GLOBAL_COUNTER);
        levelTarget.remove(TestModContent.TEST_COUNTER);
        helper.succeed();
    }
}
