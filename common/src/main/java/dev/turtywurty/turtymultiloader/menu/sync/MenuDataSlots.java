package dev.turtywurty.turtymultiloader.menu.sync;

import net.minecraft.world.inventory.ContainerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

/**
 * Builds vanilla {@link ContainerData} views, including lossless long and double values split across two slots.
 */
public final class MenuDataSlots implements ContainerData {
    private final List<Entry> entries;

    private MenuDataSlots(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int get(int dataId) {
        return entry(dataId).getter().getAsInt();
    }

    @Override
    public void set(int dataId, int value) {
        entry(dataId).setter().accept(value);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    private Entry entry(int dataId) {
        if (dataId < 0 || dataId >= entries.size())
            throw new IndexOutOfBoundsException("Menu data slot " + dataId + " is outside 0.." + (entries.size() - 1));

        return entries.get(dataId);
    }

    public static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        private Builder() {
        }

        public Builder add(IntSupplier getter, IntConsumer setter) {
            entries.add(new Entry(
                Objects.requireNonNull(getter, "getter"),
                Objects.requireNonNull(setter, "setter")
            ));
            return this;
        }

        public Builder addBoolean(java.util.function.BooleanSupplier getter, Consumer<Boolean> setter) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            return add(() -> getter.getAsBoolean() ? 1 : 0, value -> setter.accept(value != 0));
        }

        public Builder addFloat(DoubleSupplier getter, DoubleConsumer setter) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            return add(
                () -> Float.floatToRawIntBits((float) getter.getAsDouble()),
                value -> setter.accept(Float.intBitsToFloat(value))
            );
        }

        public Builder addLong(LongSupplier getter, LongConsumer setter) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            add(
                () -> (int) getter.getAsLong(),
                low -> setter.accept((getter.getAsLong() & 0xFFFFFFFF00000000L) | Integer.toUnsignedLong(low))
            );
            add(
                () -> (int) (getter.getAsLong() >>> 32),
                high -> setter.accept(((long) high << 32) | (getter.getAsLong() & 0xFFFFFFFFL))
            );
            return this;
        }

        public Builder addDouble(DoubleSupplier getter, DoubleConsumer setter) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            return addLong(
                () -> Double.doubleToRawLongBits(getter.getAsDouble()),
                value -> setter.accept(Double.longBitsToDouble(value))
            );
        }

        public <E extends Enum<E>> Builder addEnum(
            Class<E> enumType,
            java.util.function.Supplier<E> getter,
            Consumer<E> setter
        ) {
            Objects.requireNonNull(enumType, "enumType");
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            E[] constants = enumType.getEnumConstants();
            return add(
                () -> getter.get().ordinal(),
                ordinal -> {
                    if (ordinal < 0 || ordinal >= constants.length)
                        throw new IllegalArgumentException("Invalid " + enumType.getSimpleName() + " ordinal " + ordinal);

                    setter.accept(constants[ordinal]);
                }
            );
        }

        public MenuDataSlots build() {
            return new MenuDataSlots(entries);
        }
    }

    private record Entry(IntSupplier getter, IntConsumer setter) {
    }
}
