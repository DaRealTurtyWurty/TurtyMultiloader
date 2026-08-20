package dev.turtywurty.multiblocklib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.client.MultiblockControllerRenderState.RenderPart;
import dev.turtywurty.multiblocklib.data.MultiblockPartEntry;
import dev.turtywurty.multiblocklib.data.PortInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MultiblockControllerBlockEntityRenderer implements BlockEntityRenderer<MultiblockControllerBlockEntity, MultiblockControllerRenderState> {
    private static BlockState portMarkerState(final PortInstance port) {
        boolean multi = port.types().size() > 1;
        String primary = port.types().stream().findFirst().orElse("");
        if (port.types().contains("energy")) {
            primary = "energy";
        } else if (port.types().contains("fluid")) {
            primary = "fluid";
        } else if (port.types().contains("item")) {
            primary = "item";
        }

        if (multi) {
            return Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        }

        return switch (primary) {
            case "energy" -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case "fluid" -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case "item" -> Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            default -> Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
        };
    }

    @Override
    public MultiblockControllerRenderState createRenderState() {
        return new MultiblockControllerRenderState();
    }

    @Override
    public void extractRenderState(
        final MultiblockControllerBlockEntity blockEntity,
        final MultiblockControllerRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.parts.clear();
        state.portMarkers.clear();
        Level level = blockEntity.getLevel();
        List<MultiblockPartEntry> parts = blockEntity.getParts();
        if (!(level instanceof ClientLevel clientLevel) || !blockEntity.isFormed() || !isStructureRenderable(level, blockEntity.getBlockPos(), parts)) {
            return;
        }

        for (MultiblockPartEntry entry : parts) {
            BlockPos partPos = blockEntity.getBlockPos().offset(entry.offset());
            state.parts.add(new RenderPart(entry.offset(), createMovingBlock(partPos, entry.state(), clientLevel)));
        }

        for (PortInstance port : blockEntity.getPorts()) {
            BlockPos portPos = blockEntity.getBlockPos().offset(port.offset());
            state.portMarkers.add(new RenderPart(port.offset(), createMovingBlock(portPos, portMarkerState(port), clientLevel)));
        }
    }

    @Override
    public void submit(
        final MultiblockControllerRenderState state,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final CameraRenderState camera
    ) {
        for (RenderPart part : state.parts) {
            poseStack.pushPose();
            poseStack.translate(part.offset().getX(), part.offset().getY(), part.offset().getZ());
            submitNodeCollector.submitMovingBlock(poseStack, part.movingBlockRenderState());
            poseStack.popPose();
        }

        for (RenderPart part : state.portMarkers) {
            poseStack.pushPose();
            poseStack.translate(part.offset().getX(), part.offset().getY(), part.offset().getZ());
            submitNodeCollector.submitMovingBlock(poseStack, part.movingBlockRenderState());
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static MovingBlockRenderState createMovingBlock(final BlockPos pos, final BlockState blockState, final ClientLevel level) {
        MovingBlockRenderState renderState = new MovingBlockRenderState();
        renderState.randomSeedPos = pos;
        renderState.blockPos = pos;
        renderState.blockState = blockState;
        renderState.biome = level.getBiome(pos);
        renderState.cardinalLighting = level.cardinalLighting();
        renderState.lightEngine = level.getLightEngine();
        return renderState;
    }

    private static boolean isStructureRenderable(final Level level, final BlockPos controllerPos, final List<MultiblockPartEntry> parts) {
        if (parts.isEmpty()) {
            return false;
        }

        for (MultiblockPartEntry entry : parts) {
            BlockPos worldPos = controllerPos.offset(entry.offset());
            BlockState state = level.getBlockState(worldPos);
            if (entry.offset().equals(BlockPos.ZERO)) {
                if (!MultiblockLib.isControllerBlock(state.getBlock())) {
                    return false;
                }
                continue;
            }

            if (!state.is(MultiblockLib.MULTIBLOCK_PART)) {
                return false;
            }
        }

        return true;
    }
}
