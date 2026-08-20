package dev.turtywurty.multiblocklib.pattern;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public enum MultiblockRotation implements StringRepresentable {
    NONE("none"),
    CW_90("cw_90"),
    CW_180("cw_180"),
    CW_270("cw_270");

    public static final StringRepresentable.EnumCodec<MultiblockRotation> CODEC = StringRepresentable.fromEnum(MultiblockRotation::values);

    private final String serializedName;

    MultiblockRotation(final String serializedName) {
        this.serializedName = serializedName;
    }

    public static MultiblockRotation fromString(final String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (MultiblockRotation rotation : values()) {
            if (rotation.serializedName.equals(normalized)) {
                return rotation;
            }
        }

        throw new IllegalArgumentException("Unknown rotation: " + value);
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }
}
