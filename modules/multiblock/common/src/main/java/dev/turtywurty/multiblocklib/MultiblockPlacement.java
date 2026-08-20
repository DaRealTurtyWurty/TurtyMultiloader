package dev.turtywurty.multiblocklib;

import dev.turtywurty.multiblocklib.block.entity.MultiblockControllerBlockEntity;
import dev.turtywurty.multiblocklib.data.MultiblockDefinition;
import dev.turtywurty.multiblocklib.data.MultiblockPartEntry;
import dev.turtywurty.multiblocklib.data.PortDefinition;
import dev.turtywurty.multiblocklib.data.PortInstance;
import dev.turtywurty.multiblocklib.match.BlockMatcherList;
import dev.turtywurty.multiblocklib.pattern.MultiblockPattern;
import dev.turtywurty.multiblocklib.pattern.MultiblockRotation;
import dev.turtywurty.multiblocklib.pattern.MultiblockTransform;
import dev.turtywurty.multiblocklib.world.MultiblockWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public final class MultiblockPlacement {
    private static boolean loggedEmpty;

    private MultiblockPlacement() {
    }

    public static InteractionResult tryForm(final ServerLevel level, final BlockPos clickedPos, final ItemStack stack) {
        return tryForm(level, clickedPos, stack, null);
    }

    public static InteractionResult tryForm(
        final ServerLevel level,
        final BlockPos clickedPos,
        final ItemStack stack,
        final ServerPlayer player
    ) {
        if (MultiblockLib.DEFINITION_MANAGER.definitions().isEmpty() && !loggedEmpty) {
            MultiblockLib.LOGGER.warn("No multiblock definitions loaded. Ensure data packs are present and /reload.");
            loggedEmpty = true;
        }

        List<MultiblockDefinition> candidateDefinitions = new ArrayList<>();
        for (MultiblockDefinition definition : MultiblockLib.DEFINITION_MANAGER.definitions().values()) {
            if (definition.triggerItem() != stack.getItem()) {
                continue;
            }
            candidateDefinitions.add(definition);
        }
        if (candidateDefinitions.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player != null && player.isShiftKeyDown()) {
            InteractionResult diagnosticsResult = tryDiagnose(level, clickedPos, candidateDefinitions, player);
            if (diagnosticsResult != InteractionResult.PASS) {
                return diagnosticsResult;
            }
        }

        for (MultiblockDefinition definition : candidateDefinitions) {
            MultiblockMatch match = findMatch(level, clickedPos, definition);
            if (match != null) {
                formMultiblock(level, definition, match);
                return InteractionResult.SUCCESS;
            }
            MultiblockLib.LOGGER.debug("Trigger item matched for {}, but no pattern match at {}.", definition.id(), clickedPos);
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult tryDiagnose(
        final ServerLevel level,
        final BlockPos clickedPos,
        final List<MultiblockDefinition> definitions,
        final ServerPlayer player
    ) {
        Block block = level.getBlockState(clickedPos).getBlock();
        if (!MultiblockLib.isControllerBlock(block)) {
            return InteractionResult.PASS;
        }

        Identifier controllerBlockId = BuiltInRegistries.BLOCK.getKey(block);
        List<MultiblockDefinition> candidates = new ArrayList<>();
        for (MultiblockDefinition definition : definitions) {
            if (definition.controllerBlockId().equals(controllerBlockId)) {
                candidates.add(definition);
            }
        }

        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.literal("No multiblock definition for controller block " + controllerBlockId + " and held trigger item."));
            return InteractionResult.SUCCESS;
        }

        DiagnosticResult bestResult = null;
        for (MultiblockDefinition definition : candidates) {
            for (MultiblockRotation rotation : definition.allowedRotations()) {
                for (boolean mirror : mirrorOptions(definition.allowMirroring())) {
                    DiagnosticResult result = diagnoseAt(level, clickedPos, definition, rotation, mirror);
                    if (bestResult == null || result.mismatches().size() < bestResult.mismatches().size()) {
                        bestResult = result;
                    }
                }
            }
        }

        if (bestResult == null) {
            return InteractionResult.PASS;
        }

        if (bestResult.mismatches().isEmpty()) {
            player.sendSystemMessage(Component.literal("Multiblock '" + bestResult.definition().id() + "' matches the pattern (rotation="
                + bestResult.rotation().getSerializedName() + ", mirror=" + bestResult.mirror() + ")."));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.literal("Multiblock '" + bestResult.definition().id() + "' mismatch report: "
            + bestResult.mismatches().size() + " blocks incorrect (rotation="
            + bestResult.rotation().getSerializedName() + ", mirror=" + bestResult.mirror() + ")."));

        int maxReported = Math.min(bestResult.mismatches().size(), 12);
        for (int i = 0; i < maxReported; i++) {
            Mismatch mismatch = bestResult.mismatches().get(i);
            player.sendSystemMessage(Component.literal(" - at " + mismatch.worldPos()
                + " (offset " + mismatch.controllerOffset() + "): found " + mismatch.found()
                + ", expected " + mismatch.expected()));
        }

        if (bestResult.mismatches().size() > maxReported) {
            int hidden = bestResult.mismatches().size() - maxReported;
            player.sendSystemMessage(Component.literal(" ... and " + hidden + " more mismatches."));
        }

        return InteractionResult.SUCCESS;
    }

    private static MultiblockMatch findMatch(final ServerLevel level, final BlockPos clickedPos, final MultiblockDefinition definition) {
        MultiblockPattern pattern = definition.pattern();
        for (MultiblockRotation rotation : definition.allowedRotations()) {
            for (boolean mirror : mirrorOptions(definition.allowMirroring())) {
                BlockPos size = pattern.size();
                BlockState normalizedClickedState = normalizeStateForPatternMatch(level.getBlockState(clickedPos), rotation, mirror);
                for (BlockPos localPos : pattern.positions()) {
                    BlockPos transformed = MultiblockTransform.transform(localPos, size, rotation, mirror);
                    BlockPos anchor = clickedPos.subtract(transformed);
                    BlockMatcherList matcher = pattern.matcherAt(localPos);
                    if (matcher != null && !matcher.matches(normalizedClickedState)) {
                        continue;
                    }
                    if (matchesAt(level, definition, anchor, rotation, mirror)) {
                        return new MultiblockMatch(anchor, rotation, mirror);
                    }
                }
            }
        }

        return null;
    }

    private static boolean matchesAt(
        final ServerLevel level,
        final MultiblockDefinition definition,
        final BlockPos anchor,
        final MultiblockRotation rotation,
        final boolean mirror
    ) {
        MultiblockPattern pattern = definition.pattern();
        BlockPos size = pattern.size();
        for (BlockPos localPos : pattern.positions()) {
            BlockPos transformed = MultiblockTransform.transform(localPos, size, rotation, mirror);
            BlockPos worldPos = anchor.offset(transformed);
            if (!level.isLoaded(worldPos)) {
                return false;
            }

            BlockState state = level.getBlockState(worldPos);
            if (state.getBlock() == MultiblockLib.MULTIBLOCK_PART) {
                return false;
            }
            if (MultiblockLib.isControllerBlock(state.getBlock()) && !localPos.equals(definition.controllerPos())) {
                return false;
            }
            BlockMatcherList matcher = pattern.matcherAt(localPos);
            if (matcher == null || !matcher.matches(normalizeStateForPatternMatch(state, rotation, mirror))) {
                return false;
            }
        }

        return true;
    }

    private static void formMultiblock(
        final ServerLevel level,
        final MultiblockDefinition definition,
        final MultiblockMatch match
    ) {
        MultiblockPattern pattern = definition.pattern();
        BlockPos size = pattern.size();
        BlockPos controllerLocal = definition.controllerPos();
        BlockPos controllerWorld = match.anchor().offset(MultiblockTransform.transform(controllerLocal, size, match.rotation(), match.mirror()));
        Direction inferredControllerFacing = inferControllerFacing(definition, match);

        List<MultiblockPartEntry> parts = new ArrayList<>();
        for (BlockPos localPos : pattern.positions()) {
            BlockPos transformed = MultiblockTransform.transform(localPos, size, match.rotation(), match.mirror());
            BlockPos worldPos = match.anchor().offset(transformed);
            BlockState state = level.getBlockState(worldPos);
            if (worldPos.equals(controllerWorld)) {
                state = applyControllerFacing(state, inferredControllerFacing);
            }
            parts.add(new MultiblockPartEntry(worldPos.subtract(controllerWorld), state));
        }

        List<PortInstance> ports = new ArrayList<>();
        for (PortDefinition port : definition.ports()) {
            BlockPos transformed = MultiblockTransform.transform(port.position(), size, match.rotation(), match.mirror());
            BlockPos worldPos = match.anchor().offset(transformed);
            ports.add(new PortInstance(worldPos.subtract(controllerWorld), port.types(), port.io()));
        }

        MultiblockWorldData data = MultiblockWorldData.get(level);
        for (MultiblockPartEntry entry : parts) {
            BlockPos worldPos = controllerWorld.offset(entry.offset());
            if (worldPos.equals(controllerWorld)) {
                level.setBlock(worldPos, entry.state(), 3);
            } else {
                level.setBlock(worldPos, MultiblockLib.MULTIBLOCK_PART.defaultBlockState(), 3);
                data.mapPart(worldPos, controllerWorld, entry.state());
            }
        }

        if (level.getBlockEntity(controllerWorld) instanceof MultiblockControllerBlockEntity controllerEntity) {
            controllerEntity.configure(definition.id(), parts, ports);
        } else {
            MultiblockLib.LOGGER.warn("Controller block at {} is not a MultiblockControllerBlockEntity for definition {}", controllerWorld, definition.id());
        }
    }

    private static Direction inferControllerFacing(final MultiblockDefinition definition, final MultiblockMatch match) {
        Direction localForward = inferLocalForward(definition.pattern(), definition.controllerPos());
        if (localForward == null) {
            return null;
        }

        return transformLocalFacing(localForward, match.rotation(), match.mirror());
    }

    private static Direction inferLocalForward(final MultiblockPattern pattern, final BlockPos controllerPos) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos localPos : pattern.positions()) {
            minX = Math.min(minX, localPos.getX());
            maxX = Math.max(maxX, localPos.getX());
            minZ = Math.min(minZ, localPos.getZ());
            maxZ = Math.max(maxZ, localPos.getZ());
        }

        int east = maxX - controllerPos.getX();
        int west = controllerPos.getX() - minX;
        int south = maxZ - controllerPos.getZ();
        int north = controllerPos.getZ() - minZ;

        int maxExtent = Math.max(Math.max(east, west), Math.max(south, north));
        if (maxExtent <= 0) {
            return null;
        }

        int winners = 0;
        Direction winner = null;
        if (east == maxExtent) {
            winners++;
            winner = Direction.EAST;
        }
        if (west == maxExtent) {
            winners++;
            winner = Direction.WEST;
        }
        if (south == maxExtent) {
            winners++;
            winner = Direction.SOUTH;
        }
        if (north == maxExtent) {
            winners++;
            winner = Direction.NORTH;
        }

        return winners == 1 ? winner : null;
    }

    private static Direction transformLocalFacing(
        final Direction localFacing,
        final MultiblockRotation rotation,
        final boolean mirror
    ) {
        Direction facing = localFacing;
        if (mirror && facing.getAxis() == Direction.Axis.X) {
            facing = facing.getOpposite();
        }

        return switch (rotation) {
            case NONE -> facing;
            case CW_90 -> facing.getClockWise();
            case CW_180 -> facing.getOpposite();
            case CW_270 -> facing.getCounterClockWise();
        };
    }

    private static BlockState applyControllerFacing(final BlockState state, final Direction facing) {
        if (facing == null || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state;
        }

        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    private static List<Boolean> mirrorOptions(final boolean allowMirroring) {
        return allowMirroring ? List.of(false, true) : List.of(false);
    }

    private static DiagnosticResult diagnoseAt(
        final ServerLevel level,
        final BlockPos controllerPos,
        final MultiblockDefinition definition,
        final MultiblockRotation rotation,
        final boolean mirror
    ) {
        MultiblockPattern pattern = definition.pattern();
        BlockPos size = pattern.size();
        BlockPos transformedController = MultiblockTransform.transform(definition.controllerPos(), size, rotation, mirror);
        BlockPos anchor = controllerPos.subtract(transformedController);

        List<Mismatch> mismatches = new ArrayList<>();
        for (BlockPos localPos : pattern.positions()) {
            BlockPos transformed = MultiblockTransform.transform(localPos, size, rotation, mirror);
            BlockPos worldPos = anchor.offset(transformed);
            BlockMatcherList matcher = pattern.matcherAt(localPos);
            if (matcher == null) {
                continue;
            }

            if (!level.isLoaded(worldPos)) {
                mismatches.add(new Mismatch(worldPos, worldPos.subtract(controllerPos), matcher.describe(), "<unloaded chunk>"));
                continue;
            }

            BlockState worldState = level.getBlockState(worldPos);
            if (worldState.getBlock() == MultiblockLib.MULTIBLOCK_PART) {
                mismatches.add(new Mismatch(worldPos, worldPos.subtract(controllerPos), matcher.describe(), "multiblocklib:multiblock_part"));
                continue;
            }
            if (MultiblockLib.isControllerBlock(worldState.getBlock()) && !localPos.equals(definition.controllerPos())) {
                mismatches.add(new Mismatch(worldPos, worldPos.subtract(controllerPos), matcher.describe(),
                    BuiltInRegistries.BLOCK.getKey(worldState.getBlock()).toString()));
                continue;
            }

            BlockState normalized = normalizeStateForPatternMatch(worldState, rotation, mirror);
            if (!matcher.matches(normalized)) {
                String foundId = BuiltInRegistries.BLOCK.getKey(worldState.getBlock()).toString();
                mismatches.add(new Mismatch(worldPos, worldPos.subtract(controllerPos), matcher.describe(), foundId));
            }
        }

        return new DiagnosticResult(definition, rotation, mirror, mismatches);
    }

    private static BlockState normalizeStateForPatternMatch(
        final BlockState worldState,
        final MultiblockRotation rotation,
        final boolean mirror
    ) {
        BlockState normalized = worldState.rotate(inverseRotation(rotation));
        if (mirror) {
            normalized = normalized.mirror(Mirror.FRONT_BACK);
        }
        return normalized;
    }

    private static Rotation inverseRotation(final MultiblockRotation rotation) {
        return switch (rotation) {
            case NONE -> Rotation.NONE;
            case CW_90 -> Rotation.COUNTERCLOCKWISE_90;
            case CW_180 -> Rotation.CLOCKWISE_180;
            case CW_270 -> Rotation.CLOCKWISE_90;
        };
    }

    private record DiagnosticResult(
        MultiblockDefinition definition,
        MultiblockRotation rotation,
        boolean mirror,
        List<Mismatch> mismatches
    ) {
    }

    private record Mismatch(BlockPos worldPos, BlockPos controllerOffset, String expected, String found) {
    }

    private record MultiblockMatch(BlockPos anchor, MultiblockRotation rotation, boolean mirror) {
    }
}
