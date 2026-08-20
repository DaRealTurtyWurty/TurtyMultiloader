package dev.turtywurty.multiblocklib.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.turtywurty.multiblocklib.MultiblockLib;
import dev.turtywurty.multiblocklib.pattern.MultiblockPattern;
import dev.turtywurty.multiblocklib.pattern.MultiblockPatternFactory;
import dev.turtywurty.multiblocklib.pattern.MultiblockPatternRegistry;
import dev.turtywurty.multiblocklib.pattern.MultiblockRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;

public class MultiblockDefinitionManager extends SimplePreparableReloadListener<Map<Identifier, MultiblockDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("multiblocks");

    private Map<Identifier, MultiblockDefinition> definitions = Map.of();

    private static MultiblockDefinition parseDefinition(final Identifier id, final JsonObject json) {
        Identifier triggerItemId = Identifier.parse(GsonHelper.getAsString(json, "trigger_item"));
        if (!BuiltInRegistries.ITEM.containsKey(triggerItemId)) {
            throw new IllegalArgumentException("Unknown trigger item: " + triggerItemId);
        }

        Set<MultiblockRotation> rotations = parseRotations(json);
        boolean allowMirroring = GsonHelper.getAsBoolean(json, "allow_mirror", false);
        Identifier controllerBlockId = Identifier.parse(GsonHelper.getAsString(
            json,
            "controller_block",
            MultiblockLib.MULTIBLOCK_CONTROLLER_KEY.identifier().toString()
        ));
        if (!BuiltInRegistries.BLOCK.containsKey(controllerBlockId)) {
            throw new IllegalArgumentException("Unknown controller block: " + controllerBlockId);
        }
        Block controllerBlock = BuiltInRegistries.BLOCK.getValue(controllerBlockId);
        if (!MultiblockLib.isControllerBlock(controllerBlock)) {
            throw new IllegalArgumentException("Block is not a registered multiblock controller: " + controllerBlockId);
        }
        Item triggerItem = BuiltInRegistries.ITEM.getValue(triggerItemId);
        BlockPos controller = parseBlockPos(GsonHelper.getAsJsonArray(json, "controller"));
        MultiblockPattern pattern = parsePattern(GsonHelper.getAsJsonObject(json, "pattern"));
        if (pattern.matcherAt(controller) == null) {
            throw new IllegalArgumentException("Controller position " + controller + " is not part of the pattern");
        }
        List<PortDefinition> ports = parsePorts(json);
        validatePorts(pattern, ports);

        return new MultiblockDefinition(id, triggerItem, rotations, allowMirroring, controllerBlockId, controller, pattern, ports);
    }

    private static Set<MultiblockRotation> parseRotations(final JsonObject json) {
        if (!json.has("allowed_rotations")) {
            return EnumSet.allOf(MultiblockRotation.class);
        }

        EnumSet<MultiblockRotation> rotations = EnumSet.noneOf(MultiblockRotation.class);
        JsonArray array = GsonHelper.getAsJsonArray(json, "allowed_rotations");
        for (JsonElement element : array) {
            rotations.add(MultiblockRotation.fromString(GsonHelper.convertToString(element, "rotation")));
        }
        return rotations.isEmpty() ? EnumSet.allOf(MultiblockRotation.class) : rotations;
    }

    private static MultiblockPattern parsePattern(final JsonObject json) {
        Identifier typeId = Identifier.parse(GsonHelper.getAsString(json, "type"));
        MultiblockPatternFactory factory = MultiblockPatternRegistry.get(typeId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown multiblock pattern type: " + typeId);
        }

        return factory.create(json);
    }

    private static List<PortDefinition> parsePorts(final JsonObject json) {
        if (!json.has("ports")) {
            return List.of();
        }

        List<PortDefinition> ports = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(json, "ports");
        for (JsonElement element : array) {
            JsonObject obj = GsonHelper.convertToJsonObject(element, "port");
            BlockPos pos = parseBlockPos(GsonHelper.getAsJsonArray(obj, "pos"));
            Set<String> types = new HashSet<>();
            if (obj.has("types") && obj.get("types").isJsonArray()) {
                JsonArray typesArray = GsonHelper.getAsJsonArray(obj, "types");
                for (JsonElement typeElement : typesArray) {
                    types.add(GsonHelper.convertToString(typeElement, "type").toLowerCase(Locale.ROOT));
                }
            } else if (obj.has("type")) {
                types.add(GsonHelper.getAsString(obj, "type").toLowerCase(Locale.ROOT));
            } else if (obj.has("types")) {
                types.add(GsonHelper.getAsString(obj, "types").toLowerCase(Locale.ROOT));
            }
            PortIO io = PortIO.fromString(GsonHelper.getAsString(obj, "io", "both"));
            ports.add(new PortDefinition(pos, Set.copyOf(types), io));
        }
        return ports;
    }

    private static BlockPos parseBlockPos(final JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException("Expected 3-element position array, got " + array.size());
        }

        return new BlockPos(
            GsonHelper.convertToInt(array.get(0), "x"),
            GsonHelper.convertToInt(array.get(1), "y"),
            GsonHelper.convertToInt(array.get(2), "z")
        );
    }

    private static void validatePorts(final MultiblockPattern pattern, final List<PortDefinition> ports) {
        Set<BlockPos> seen = new HashSet<>();
        for (PortDefinition port : ports) {
            if (pattern.matcherAt(port.position()) == null) {
                throw new IllegalArgumentException("Port position " + port.position() + " is not part of the pattern");
            }
            if (port.types().isEmpty()) {
                throw new IllegalArgumentException("Port at " + port.position() + " must declare at least one type");
            }
            if (!seen.add(port.position())) {
                throw new IllegalArgumentException("Duplicate port position: " + port.position());
            }
        }
    }

    @Override
    protected Map<Identifier, MultiblockDefinition> prepare(final @NonNull ResourceManager manager, final @NonNull ProfilerFiller profiler) {
        Map<Identifier, MultiblockDefinition> loaded = new HashMap<>();
        Map<Identifier, Resource> resources = LISTER.listMatchingResources(manager);
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier file = entry.getKey();
            Identifier id = LISTER.fileToId(file);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = StrictJsonParser.parse(reader);
                MultiblockDefinition definition = parseDefinition(id, GsonHelper.convertToJsonObject(json, "multiblock"));
                loaded.put(id, definition);
            } catch (Exception ex) {
                LOGGER.error("Failed to load multiblock definition {} from {}", id, file, ex);
            }
        }

        return loaded;
    }

    @Override
    protected void apply(final Map<Identifier, MultiblockDefinition> prepared, final @NonNull ResourceManager manager, final @NonNull ProfilerFiller profiler) {
        this.definitions = Map.copyOf(prepared);
        LOGGER.info("Loaded {} multiblock definitions: {}", this.definitions.size(), this.definitions.keySet());
    }

    public Map<Identifier, MultiblockDefinition> definitions() {
        return this.definitions;
    }

    public MultiblockDefinition get(final Identifier id) {
        return this.definitions.get(id);
    }
}
