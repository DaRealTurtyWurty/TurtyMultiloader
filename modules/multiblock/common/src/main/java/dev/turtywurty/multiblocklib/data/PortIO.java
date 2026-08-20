package dev.turtywurty.multiblocklib.data;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public enum PortIO implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output"),
    BOTH("both");

    public static final StringRepresentable.EnumCodec<PortIO> CODEC = StringRepresentable.fromEnum(PortIO::values);

    private final String serializedName;

    PortIO(final String serializedName) {
        this.serializedName = serializedName;
    }

    public static PortIO fromString(final String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (PortIO io : values()) {
            if (io.serializedName.equals(normalized)) {
                return io;
            }
        }

        throw new IllegalArgumentException("Unknown port IO: " + value);
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }
}
