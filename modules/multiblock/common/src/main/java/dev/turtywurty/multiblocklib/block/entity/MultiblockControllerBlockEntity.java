package dev.turtywurty.multiblocklib.block.entity;

import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.data.MultiblockPartEntry;
import dev.turtywurty.multiblocklib.data.PortIO;
import dev.turtywurty.multiblocklib.data.PortInstance;
import dev.turtywurty.multiblocklib.port.*;
import dev.turtywurty.multiblocklib.port.storage.MultiblockEnergyPortStorage;
import dev.turtywurty.multiblocklib.port.storage.MultiblockFluidPortStorage;
import dev.turtywurty.multiblocklib.port.storage.MultiblockItemPortStorage;
import dev.turtywurty.multiblocklib.world.MultiblockWorldData;
import dev.turtywurty.turtymultiloader.transfer.TransferService;
import dev.turtywurty.turtymultiloader.transfer.lookup.StorageKeys;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceVariant;
import dev.turtywurty.turtymultiloader.transfer.resource.UnitResource;
import dev.turtywurty.turtymultiloader.transfer.storage.ResourceStorage;
import dev.turtywurty.turtymultiloader.transfer.storage.TransferSupport;
import dev.turtywurty.turtymultiloader.transfer.transaction.TransferTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

public class MultiblockControllerBlockEntity extends BlockEntity {
    public static final int DEFAULT_ITEM_CAPACITY = 64;
    public static final long DEFAULT_FLUID_CAPACITY = 1000;
    public static final long DEFAULT_ENERGY_CAPACITY = 10000;
    private static final long OUTPUT_ITEM_PUSH = 64;
    private static final long OUTPUT_FLUID_PUSH = 1000;
    private static final long OUTPUT_ENERGY_PUSH = 1000;
    private final List<MultiblockPartEntry> parts = new ArrayList<>();
    private final List<PortInstance> ports = new ArrayList<>();
    private final List<PortRuntime> portRuntimes = new ArrayList<>();
    private final Map<BlockPos, PortRuntime> portByOffset = new HashMap<>();
    private PortRegistrar definedPorts;
    private Identifier definitionId;
    private boolean breaking;

    public MultiblockControllerBlockEntity(final BlockPos pos, final BlockState state) {
        this(MultiblockLib.MULTIBLOCK_CONTROLLER_ENTITY_HANDLE.get(), pos, state);
    }

    public MultiblockControllerBlockEntity(
        final BlockEntityType<? extends MultiblockControllerBlockEntity> type,
        final BlockPos pos,
        final BlockState state
    ) {
        super(type, pos, state);
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final MultiblockControllerBlockEntity controller) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!controller.isFormed()) {
            return;
        }
        if (!controller.breaking && !controller.isStructureIntact(serverLevel)) {
            controller.breakMultiblock();
            return;
        }
        controller.tickServer(serverLevel);
        controller.pushOutputs(serverLevel);
    }

    public void configure(final Identifier definitionId, final List<MultiblockPartEntry> parts, final List<PortInstance> ports) {
        this.definitionId = definitionId;
        this.parts.clear();
        this.parts.addAll(parts);
        this.ports.clear();
        this.ports.addAll(ports);
        rebuildPortStorages();
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public Identifier getDefinitionId() {
        return this.definitionId;
    }

    public boolean isFormed() {
        return this.definitionId != null && !this.parts.isEmpty();
    }

    public List<MultiblockPartEntry> getParts() {
        return List.copyOf(this.parts);
    }

    public List<PortInstance> getPorts() {
        return List.copyOf(this.ports);
    }

    public ResourceStorage<ResourceVariant<Item>> getItemStorage(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        return port != null ? port.itemStorage : null;
    }

    public ResourceStorage<ResourceVariant<Fluid>> getFluidStorage(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        return port != null ? port.fluidStorage : null;
    }

    public ResourceStorage<ResourceVariant<UnitResource>> getEnergyStorage(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        return port != null ? port.energyStorage : null;
    }

    public Object getCustomPortStorage(final BlockPos worldPos, final String typeId) {
        PortRuntime port = getPortRuntime(worldPos);
        return port != null ? port.customStorages.get(normalizeTypeId(typeId)) : null;
    }

    public <T> T getCustomPortStorage(final BlockPos worldPos, final String typeId, final Class<T> clazz) {
        Object storage = getCustomPortStorage(worldPos, typeId);
        if (!clazz.isInstance(storage)) {
            return null;
        }
        return clazz.cast(storage);
    }

    public ResourceStorage<ResourceVariant<Item>> getItemStorageForExternal(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        if (port == null || port.itemStorage == null) {
            return null;
        }
        return port.itemStorage.restrictedTo(externalSupport(port.io));
    }

    public ResourceStorage<ResourceVariant<Fluid>> getFluidStorageForExternal(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        if (port == null || port.fluidStorage == null) {
            return null;
        }
        return port.fluidStorage.restrictedTo(externalSupport(port.io));
    }

    public ResourceStorage<ResourceVariant<UnitResource>> getEnergyStorageForExternal(final BlockPos worldPos) {
        PortRuntime port = getPortRuntime(worldPos);
        if (port == null || port.energyStorage == null) {
            return null;
        }
        return port.energyStorage.restrictedTo(externalSupport(port.io));
    }

    public List<ResourceStorage<ResourceVariant<Item>>> getItemPorts(final PortIO io) {
        List<ResourceStorage<ResourceVariant<Item>>> result = new ArrayList<>();
        for (PortRuntime runtime : this.portRuntimes) {
            if (!runtime.matchesIO(io)) {
                continue;
            }
            if (runtime.itemStorage != null) {
                result.add(runtime.itemStorage);
            }
        }
        return result;
    }

    public List<ResourceStorage<ResourceVariant<Fluid>>> getFluidPorts(final PortIO io) {
        List<ResourceStorage<ResourceVariant<Fluid>>> result = new ArrayList<>();
        for (PortRuntime runtime : this.portRuntimes) {
            if (!runtime.matchesIO(io)) {
                continue;
            }
            if (runtime.fluidStorage != null) {
                result.add(runtime.fluidStorage);
            }
        }
        return result;
    }

    public List<ResourceStorage<ResourceVariant<UnitResource>>> getEnergyPorts(final PortIO io) {
        List<ResourceStorage<ResourceVariant<UnitResource>>> result = new ArrayList<>();
        for (PortRuntime runtime : this.portRuntimes) {
            if (!runtime.matchesIO(io)) {
                continue;
            }
            if (runtime.energyStorage != null) {
                result.add(runtime.energyStorage);
            }
        }
        return result;
    }

    protected void tickServer(final ServerLevel level) {
    }

    protected void definePorts(final PortRegistrar ports) {
    }

    protected BlockPos getPortLocalOffset(final BlockPos worldPos) {
        return worldPos.subtract(this.getBlockPos());
    }

    protected @Nullable Direction getPortLocalSide(final @Nullable Direction worldSide) {
        return worldSide;
    }

    public final <S> @Nullable S getExternalPortStorage(
        final PortTransfer<S> transfer,
        final BlockPos worldPos,
        final @Nullable Direction worldSide
    ) {
        if (!isFormed()) {
            return null;
        }

        return getDefinedPorts().find(
            transfer,
            getPortLocalOffset(worldPos),
            getPortLocalSide(worldSide)
        );
    }

    public boolean isBreaking() {
        return this.breaking;
    }

    @Override
    public void setRemoved() {
        if (!this.breaking && this.level instanceof ServerLevel serverLevel && serverLevel.isLoaded(this.getBlockPos())) {
            BlockState worldState = serverLevel.getBlockState(this.getBlockPos());
            if (!worldState.is(this.getBlockState().getBlock())) {
                this.breakMultiblock();
            }
        }

        super.setRemoved();
    }

    public void breakMultiblock() {
        if (this.breaking) {
            return;
        }
        Level level = this.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        this.breaking = true;
        List<MultiblockPartEntry> partsToRestore = new ArrayList<>(this.parts);
        try {
            MultiblockWorldData data = MultiblockWorldData.get(serverLevel);
            data.removePartsForController(this.getBlockPos());
            for (MultiblockPartEntry entry : partsToRestore) {
                if (entry.offset().equals(BlockPos.ZERO)) {
                    continue;
                }
                BlockPos worldPos = this.getBlockPos().offset(entry.offset());
                level.setBlock(worldPos, entry.state(), 3);
            }
            for (MultiblockPartEntry entry : partsToRestore) {
                if (!entry.offset().equals(BlockPos.ZERO)) {
                    continue;
                }
                BlockPos worldPos = this.getBlockPos();
                level.setBlock(worldPos, entry.state(), 3);
            }
        } finally {
            this.breaking = false;
            clearFormedState();
        }
    }

    private boolean isStructureIntact(final ServerLevel level) {
        for (MultiblockPartEntry entry : this.parts) {
            BlockPos worldPos = this.getBlockPos().offset(entry.offset());
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

    @Override
    protected void loadAdditional(final ValueInput input) {
        this.definitionId = input.getString("definition").map(Identifier::parse).orElse(null);
        this.parts.clear();
        for (MultiblockPartEntry entry : input.listOrEmpty("parts", MultiblockPartEntry.CODEC)) {
            this.parts.add(entry);
        }
        this.ports.clear();
        for (PortInstance entry : input.listOrEmpty("ports", PortInstance.CODEC)) {
            this.ports.add(entry);
        }
        rebuildPortStorages();
        loadPortStorage(input);
    }

    @Override
    protected void saveAdditional(final @NonNull ValueOutput output) {
        if (this.definitionId != null) {
            output.putString("definition", this.definitionId.toString());
        }

        ValueOutput.TypedOutputList<MultiblockPartEntry> partsList = output.list("parts", MultiblockPartEntry.CODEC);
        for (MultiblockPartEntry entry : this.parts) {
            partsList.add(entry);
        }

        ValueOutput.TypedOutputList<PortInstance> portList = output.list("ports", PortInstance.CODEC);
        for (PortInstance entry : this.ports) {
            portList.add(entry);
        }

        savePortStorage(output);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(final HolderLookup.@NonNull Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, registries) -> this.saveCustomOnly(registries));
    }

    private void rebuildPortStorages() {
        this.portRuntimes.clear();
        this.portByOffset.clear();
        for (PortInstance port : this.ports) {
            PortRuntime runtime = new PortRuntime(port.offset(), port.types(), port.io(), this::setChanged);
            this.portRuntimes.add(runtime);
            this.portByOffset.put(port.offset(), runtime);
        }
    }

    private PortRuntime getPortRuntime(final BlockPos worldPos) {
        BlockPos offset = worldPos.subtract(this.getBlockPos());
        return this.portByOffset.get(offset);
    }

    private void savePortStorage(final ValueOutput output) {
        ValueOutput.ValueOutputList list = output.childrenList("port_data");
        for (PortRuntime runtime : this.portRuntimes) {
            ValueOutput child = list.addChild();
            child.store("offset", BlockPos.CODEC, runtime.offset);
            if (runtime.itemStorage != null) {
                ItemStack stack = runtime.itemStorage.getStoredStack();
                if (!stack.isEmpty()) {
                    child.store("item", ItemStack.CODEC, stack);
                }
            }
            if (runtime.fluidStorage != null) {
                ValueOutput fluidChild = child.child("fluid");
                runtime.fluidStorage.writeValue(fluidChild);
            }
            if (runtime.energyStorage != null) {
                child.putLong("energy", runtime.energyStorage.getAmount());
            }
            if (!runtime.customStorages.isEmpty()) {
                ValueOutput.ValueOutputList customList = child.childrenList("custom");
                for (Map.Entry<String, Object> customEntry : runtime.customStorages.entrySet()) {
                    PortType type = PortTypeRegistry.get(customEntry.getKey());
                    if (type == null) {
                        MultiblockLib.LOGGER.warn("Unable to save custom port storage for unknown type '{}'", customEntry.getKey());
                        continue;
                    }

                    ValueOutput customChild = customList.addChild();
                    customChild.putString("type", customEntry.getKey());
                    type.saveStorage(customChild, customEntry.getValue());
                }
            }
        }
    }

    private void loadPortStorage(final ValueInput input) {
        for (ValueInput child : input.childrenListOrEmpty("port_data")) {
            BlockPos offset = child.read("offset", BlockPos.CODEC).orElse(null);
            if (offset == null) {
                continue;
            }
            PortRuntime runtime = this.portByOffset.get(offset);
            if (runtime == null) {
                continue;
            }
            if (runtime.itemStorage != null) {
                child.read("item", ItemStack.CODEC).ifPresent(runtime.itemStorage::loadStack);
            }
            if (runtime.fluidStorage != null) {
                child.child("fluid").ifPresent(runtime.fluidStorage::readValue);
            }
            if (runtime.energyStorage != null) {
                long energy = child.getLongOr("energy", runtime.energyStorage.getAmount());
                try (TransferTransaction transaction = TransferTransaction.openRoot()) {
                    runtime.energyStorage.setAmount(
                        Math.min(energy, runtime.energyStorage.getCapacity()),
                        transaction
                    );
                    transaction.commit();
                }
            }
            for (ValueInput customChild : child.childrenListOrEmpty("custom")) {
                String typeId = customChild.getString("type").orElse(null);
                if (typeId == null || typeId.isBlank()) {
                    continue;
                }

                String normalizedTypeId = normalizeTypeId(typeId);
                PortType type = PortTypeRegistry.get(normalizedTypeId);
                if (type == null) {
                    MultiblockLib.LOGGER.warn("Unable to load custom port storage for unknown type '{}'", normalizedTypeId);
                    continue;
                }

                Object storage = runtime.customStorages.get(normalizedTypeId);
                if (storage == null) {
                    MultiblockLib.LOGGER.warn("Unable to load custom port storage for type '{}' at {} because no runtime storage exists", normalizedTypeId, this.getBlockPos().offset(runtime.offset));
                    continue;
                }

                type.loadStorage(customChild, storage);
            }
        }
    }

    private void pushOutputs(final ServerLevel level) {
        pushDefinedOutputs(level);

        for (PortRuntime runtime : this.portRuntimes) {
            if (runtime.io != PortIO.OUTPUT) {
                continue;
            }
            BlockPos worldPos = this.getBlockPos().offset(runtime.offset);
            for (Direction direction : Direction.values()) {
                BlockPos targetPos = worldPos.relative(direction);
                Direction toTarget = direction.getOpposite();
                if (isPartOfThisMultiblock(targetPos)) {
                    continue;
                }

                if (runtime.itemStorage != null) {
                    ResourceStorage<ResourceVariant<Item>> target = TransferService.get().findBlock(
                        StorageKeys.ITEM, level, targetPos, toTarget
                    );
                    if (target != null) {
                        moveFirst(runtime.itemStorage, target, OUTPUT_ITEM_PUSH);
                    }
                }

                if (runtime.fluidStorage != null) {
                    ResourceStorage<ResourceVariant<Fluid>> target = TransferService.get().findBlock(
                        StorageKeys.FLUID, level, targetPos, toTarget
                    );
                    if (target != null) {
                        moveFirst(runtime.fluidStorage, target, OUTPUT_FLUID_PUSH);
                    }
                }

                if (runtime.energyStorage != null) {
                    ResourceStorage<ResourceVariant<UnitResource>> target = TransferService.get().findBlock(
                        StorageKeys.ENERGY, level, targetPos, toTarget
                    );
                    if (target != null) {
                        moveFirst(runtime.energyStorage, target, OUTPUT_ENERGY_PUSH);
                    }
                }
            }
        }
    }

    private void pushDefinedOutputs(final ServerLevel level) {
        for (PortBinding<?> binding : getDefinedPorts().bindings()) {
            if (binding.io() == PortIO.OUTPUT) {
                pushDefinedOutput(level, binding);
            }
        }
    }

    private <S> void pushDefinedOutput(final ServerLevel level, final PortBinding<S> binding) {
        S source = binding.storage().get();
        if (source == null) {
            return;
        }

        for (MultiblockPartEntry part : this.parts) {
            BlockPos portPos = this.getBlockPos().offset(part.offset());
            BlockPos localOffset = getPortLocalOffset(portPos);
            for (Direction worldSide : Direction.values()) {
                Direction localSide = getPortLocalSide(worldSide);
                if (!binding.exposes(localOffset, localSide)) {
                    continue;
                }

                BlockPos targetPos = portPos.relative(worldSide);
                if (isPartOfThisMultiblock(targetPos)) {
                    continue;
                }

                S target = binding.transfer().find(level, targetPos, worldSide.getOpposite());
                if (target != null) {
                    binding.transfer().move(source, target);
                }
            }
        }
    }

    private static <V extends ResourceVariant<?>> void moveFirst(
        final ResourceStorage<V> source,
        final ResourceStorage<V> target,
        final long maxAmount
    ) {
        for (int index = 0; index < source.size(); index++) {
            if (source.amount(index) <= 0) {
                continue;
            }
            source.moveTo(target, source.resource(index), maxAmount);
            return;
        }
    }

    private PortRegistrar getDefinedPorts() {
        if (this.definedPorts == null) {
            PortRegistrar registrar = new PortRegistrar();
            definePorts(registrar);
            this.definedPorts = registrar;
        }

        return this.definedPorts;
    }

    private boolean isPartOfThisMultiblock(final BlockPos worldPos) {
        BlockPos offset = worldPos.subtract(this.getBlockPos());
        return this.parts.stream().anyMatch(part -> part.offset().equals(offset));
    }

    private static final class PortRuntime {
        private final BlockPos offset;
        private final MultiblockItemPortStorage itemStorage;
        private final MultiblockFluidPortStorage fluidStorage;
        private final MultiblockEnergyPortStorage energyStorage;
        private final PortIO io;
        private final Map<String, Object> customStorages;

        private PortRuntime(final BlockPos offset, final Set<String> types, final PortIO io, final Runnable onChange) {
            this.offset = offset;
            this.io = io;
            Set<String> normalized = types.stream().map(type -> type.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            this.itemStorage = normalized.contains("item") ? new MultiblockItemPortStorage(DEFAULT_ITEM_CAPACITY, io, onChange) : null;
            this.fluidStorage = normalized.contains("fluid") ? new MultiblockFluidPortStorage(DEFAULT_FLUID_CAPACITY, io, onChange) : null;
            this.energyStorage = normalized.contains("energy") ? new MultiblockEnergyPortStorage(DEFAULT_ENERGY_CAPACITY, io, onChange) : null;
            this.customStorages = new HashMap<>();
            for (String typeId : normalized) {
                if (typeId.equals("item") || typeId.equals("fluid") || typeId.equals("energy")) {
                    continue;
                }
                PortType type = PortTypeRegistry.get(typeId);
                if (type == null) {
                    MultiblockLib.LOGGER.warn("Ignoring unknown custom port type '{}' for port offset {}", typeId, offset);
                    continue;
                }
                Object storage = type.createStorage(io, onChange);
                if (storage != null) {
                    this.customStorages.put(typeId, storage);
                } else {
                    MultiblockLib.LOGGER.warn("Custom port type '{}' returned null storage for port offset {}", typeId, offset);
                }
            }
        }

        private boolean matchesIO(final PortIO io) {
            if (io == null) {
                return true;
            }
            return this.io == PortIO.BOTH || this.io == io;
        }
    }

    private static String normalizeTypeId(final String typeId) {
        return typeId == null ? "" : typeId.toLowerCase(Locale.ROOT);
    }

    private static TransferSupport externalSupport(final PortIO io) {
        return switch (io) {
            case INPUT -> TransferSupport.INSERT_ONLY;
            case OUTPUT -> TransferSupport.EXTRACT_ONLY;
            case BOTH -> TransferSupport.BOTH;
        };
    }

    private void clearFormedState() {
        this.definitionId = null;
        this.parts.clear();
        this.ports.clear();
        rebuildPortStorages();
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
