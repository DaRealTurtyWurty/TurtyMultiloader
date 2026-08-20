package dev.turtywurty.multiblocklib.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public class MultiblockWorldData extends SavedData {
    public static final SavedDataType<MultiblockWorldData> TYPE = new SavedDataType<>(
        Identifier.parse("multiblocklib:multiblock_world"),
        MultiblockWorldData::new,
        Entry.CODEC.listOf()
            .xmap(MultiblockWorldData::new, MultiblockWorldData::toEntries),
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Long2ObjectMap<PartData> partToControllerMap;

    public MultiblockWorldData() {
        this.partToControllerMap = new Long2ObjectOpenHashMap<>();
    }

    private MultiblockWorldData(final List<Entry> entries) {
        this();
        for (Entry entry : entries) {
            this.partToControllerMap.put(entry.part(), new PartData(entry.controller(), entry.stateId()));
        }
    }

    public static MultiblockWorldData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>();
        for (Long2ObjectMap.Entry<PartData> entry : this.partToControllerMap.long2ObjectEntrySet()) {
            PartData data = entry.getValue();
            entries.add(new Entry(entry.getLongKey(), data.controller(), data.stateId()));
        }
        return entries;
    }

    public void mapPart(final BlockPos partPos, final BlockPos controllerPos, final BlockState originalState) {
        this.partToControllerMap.put(partPos.asLong(), new PartData(controllerPos.asLong(), Block.getId(originalState)));
        this.setDirty();
    }

    public BlockPos getControllerFor(final BlockPos partPos) {
        PartData value = this.partToControllerMap.get(partPos.asLong());
        return value == null ? null : BlockPos.of(value.controller());
    }

    public void removePart(final BlockPos partPos) {
        PartData removed = this.partToControllerMap.remove(partPos.asLong());
        if (removed != null) {
            this.setDirty();
        }
    }

    public void removePartsForController(final BlockPos controllerPos) {
        long controller = controllerPos.asLong();
        List<Long> toRemove = new ArrayList<>();
        for (Long2ObjectMap.Entry<PartData> entry : this.partToControllerMap.long2ObjectEntrySet()) {
            if (entry.getValue().controller() == controller) {
                toRemove.add(entry.getLongKey());
            }
        }
        if (toRemove.isEmpty()) {
            return;
        }
        for (Long key : toRemove) {
            this.partToControllerMap.remove(key.longValue());
        }
        this.setDirty();
    }

    public void restorePartsForController(final ServerLevel level, final BlockPos controllerPos) {
        long controller = controllerPos.asLong();
        List<RestoreEntry> toRestore = new ArrayList<>();
        for (Long2ObjectMap.Entry<PartData> entry : this.partToControllerMap.long2ObjectEntrySet()) {
            PartData data = entry.getValue();
            if (data.controller() == controller) {
                toRestore.add(new RestoreEntry(entry.getLongKey(), data.stateId()));
            }
        }
        if (toRestore.isEmpty()) {
            return;
        }
        for (RestoreEntry entry : toRestore) {
            level.setBlock(BlockPos.of(entry.part()), Block.stateById(entry.stateId()), 3);
            this.partToControllerMap.remove(entry.part());
        }
        this.setDirty();
    }

    private record PartData(long controller, int stateId) {
    }

    private record RestoreEntry(long part, int stateId) {
    }

    private record Entry(long part, long controller, int stateId) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("part").forGetter(Entry::part),
            Codec.LONG.fieldOf("controller").forGetter(Entry::controller),
            Codec.INT.fieldOf("state_id").forGetter(Entry::stateId)
        ).apply(instance, Entry::new));
    }
}
