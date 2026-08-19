package dev.turtywurty.gasapi;

import dev.turtywurty.gasapi.api.Gas;
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

/** Loader-neutral entry point for the optional gas module. */
public final class GasApi {
    public static final String MOD_ID = "gasapi";

    public static final CustomRegistry<Gas> GASES = RegistryService.get().customRegistry(
        id("gases"),
        CustomRegistryOptions.DEFAULT.withSync()
    );
    public static final RegistrationHandle<Gas, Gas> EMPTY = register("empty");
    public static final RegistrationHandle<Gas, Gas> AIR = register("air");

    public static final UnitDimension DIMENSION = UnitDimension.of(id("gas"));
    public static final TransferUnit UNIT = Units.REGISTRY.register(id("unit"), DIMENSION, "g", 1);
    public static final TransferUnit BUCKET = Units.REGISTRY.register(id("bucket"), DIMENSION, "gB", 81_000);

    public static final ResourceFamily<Gas> RESOURCE_FAMILY = ResourceTypes.chemical(
        id("gas"),
        GASES.key(),
        DIMENSION,
        Gas::isEmpty,
        EMPTY::holder,
        UNIT,
        RegistryFixedCodec.create(GASES.key()),
        ByteBufCodecs.holderRegistry(GASES.key())
    );

    private GasApi() {
    }

    /** Forces the module declarations to load before the owning mod applies registration. */
    public static void initialize() {
    }

    public static RegistrationHandle<Gas, Gas> register(String path) {
        return register(id(path));
    }

    public static RegistrationHandle<Gas, Gas> register(Identifier id) {
        return GASES.register(id, () -> new Gas(id));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
