package dev.turtywurty.slurryapi;

import dev.turtywurty.slurryapi.api.Slurry;
import dev.turtywurty.turtymultiloader.registration.CustomRegistry;
import dev.turtywurty.turtymultiloader.registration.CustomRegistryOptions;
import dev.turtywurty.turtymultiloader.registration.RegistrationHandle;
import dev.turtywurty.turtymultiloader.registration.RegistryService;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceFamily;
import dev.turtywurty.turtymultiloader.transfer.resource.ResourceTypes;
import dev.turtywurty.turtymultiloader.transfer.unit.TransferUnit;
import dev.turtywurty.turtymultiloader.transfer.unit.UnitDimension;
import dev.turtywurty.turtymultiloader.transfer.unit.Units;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;

/** Loader-neutral entry point for the optional slurry module. */
public final class SlurryApi {
    public static final String MOD_ID = "slurryapi";

    public static final CustomRegistry<Slurry> SLURRIES = RegistryService.get().customRegistry(
        id("slurries"),
        CustomRegistryOptions.DEFAULT.withSync()
    );
    public static final RegistrationHandle<Slurry, Slurry> EMPTY = register("empty");

    public static final UnitDimension DIMENSION = UnitDimension.of(id("slurry"));
    public static final TransferUnit UNIT = Units.REGISTRY.register(id("unit"), DIMENSION, "s", 1);
    public static final TransferUnit BUCKET = Units.REGISTRY.register(id("bucket"), DIMENSION, "sB", 81_000);

    public static final ResourceFamily<Slurry> RESOURCE_FAMILY = ResourceTypes.chemical(
        id("slurry"),
        SLURRIES.key(),
        DIMENSION,
        Slurry::isEmpty,
        EMPTY::holder,
        UNIT,
        RegistryFixedCodec.create(SLURRIES.key()),
        ByteBufCodecs.holderRegistry(SLURRIES.key())
    );

    private SlurryApi() {
    }

    public static void initialize() {
    }

    public static RegistrationHandle<Slurry, Slurry> register(String path) {
        return register(id(path));
    }

    public static RegistrationHandle<Slurry, Slurry> register(Identifier id) {
        return SLURRIES.register(id, () -> new Slurry(id));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
