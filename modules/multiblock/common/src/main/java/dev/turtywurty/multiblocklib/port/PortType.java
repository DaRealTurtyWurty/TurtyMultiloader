package dev.turtywurty.multiblocklib.port;

import dev.turtywurty.multiblocklib.data.PortIO;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface PortType {
    Object createStorage(PortIO io, Runnable onChange);

    default void saveStorage(final ValueOutput output, final Object storage) {
    }

    default void loadStorage(final ValueInput input, final Object storage) {
    }
}
