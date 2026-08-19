package dev.turtywurty.turtymultiloader.transfer.unit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.math.BigInteger;
import java.util.*;

/**
 * Canonical registry for standard and mod-defined transfer units.
 */
public final class UnitRegistry {
    private final Map<Identifier, TransferUnit> units = new LinkedHashMap<>();
    private final Codec<TransferUnit> codec = Identifier.CODEC.comapFlatMap(this::decode, TransferUnit::id);
    private final StreamCodec<ByteBuf, TransferUnit> streamCodec = Identifier.STREAM_CODEC.map(
        this::getOrThrow,
        TransferUnit::id
    );

    public synchronized TransferUnit register(
        Identifier id,
        UnitDimension dimension,
        String symbol,
        long numerator,
        long denominator
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(symbol, "symbol");
        if (numerator <= 0 || denominator <= 0)
            throw new IllegalArgumentException("Unit scale must be positive");
        if (this.units.containsKey(id))
            throw new IllegalArgumentException("Transfer unit is already registered: " + id);

        long divisor = BigInteger.valueOf(numerator).gcd(BigInteger.valueOf(denominator)).longValueExact();
        TransferUnit unit = new TransferUnit(id, dimension, symbol, numerator / divisor, denominator / divisor);
        this.units.put(id, unit);
        return unit;
    }

    public TransferUnit register(Identifier id, UnitDimension dimension, String symbol, long scale) {
        return register(id, dimension, symbol, scale, 1);
    }

    public synchronized Optional<TransferUnit> get(Identifier id) {
        return Optional.ofNullable(this.units.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized TransferUnit getOrThrow(Identifier id) {
        TransferUnit unit = this.units.get(Objects.requireNonNull(id, "id"));
        if (unit == null)
            throw new IllegalArgumentException("Unknown transfer unit: " + id);
        return unit;
    }

    public synchronized List<TransferUnit> values() {
        return List.copyOf(this.units.values());
    }

    public synchronized List<TransferUnit> values(UnitDimension dimension) {
        Objects.requireNonNull(dimension, "dimension");
        return this.units.values().stream().filter(unit -> unit.dimension().equals(dimension)).toList();
    }

    /**
     * Codec that persists the canonical unit identifier.
     */
    public Codec<TransferUnit> codec() {
        return this.codec;
    }

    /**
     * Stream codec that synchronizes the canonical unit identifier.
     */
    public StreamCodec<ByteBuf, TransferUnit> streamCodec() {
        return this.streamCodec;
    }

    private DataResult<TransferUnit> decode(Identifier id) {
        return get(id)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown transfer unit: " + id));
    }
}
