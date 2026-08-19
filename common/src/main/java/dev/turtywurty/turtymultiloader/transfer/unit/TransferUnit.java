package dev.turtywurty.turtymultiloader.transfer.unit;

import net.minecraft.resources.Identifier;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Exact rational scale relative to a dimension's neutral base unit.
 */
public final class TransferUnit {
    private final Identifier id;
    private final UnitDimension dimension;
    private final String symbol;
    private final long numerator;
    private final long denominator;

    TransferUnit(Identifier id, UnitDimension dimension, String symbol, long numerator, long denominator) {
        this.id = Objects.requireNonNull(id, "id");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Identifier id() {
        return this.id;
    }

    public UnitDimension dimension() {
        return this.dimension;
    }

    public String symbol() {
        return this.symbol;
    }

    public long numerator() {
        return this.numerator;
    }

    public long denominator() {
        return this.denominator;
    }

    public long convert(long amount, TransferUnit target, RoundingMode rounding) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rounding, "rounding");
        if (!this.dimension.equals(target.dimension))
            throw new IllegalArgumentException("Cannot convert " + this.dimension + " to " + target.dimension);

        BigInteger dividend = BigInteger.valueOf(amount)
            .multiply(BigInteger.valueOf(this.numerator))
            .multiply(BigInteger.valueOf(target.denominator));
        BigInteger divisor = BigInteger.valueOf(this.denominator).multiply(BigInteger.valueOf(target.numerator));
        BigInteger[] result = dividend.divideAndRemainder(divisor);
        if (result[1].signum() == 0)
            return result[0].longValueExact();

        boolean increment = switch (rounding) {
            case UP -> true;
            case DOWN -> false;
            case CEILING -> amount > 0;
            case FLOOR -> amount < 0;
            case HALF_UP, HALF_DOWN, HALF_EVEN -> {
                int comparison = result[1].abs().shiftLeft(1).compareTo(divisor.abs());
                yield comparison > 0 || comparison == 0 && (rounding == RoundingMode.HALF_UP
                    || rounding == RoundingMode.HALF_EVEN && result[0].testBit(0));
            }
            case UNNECESSARY -> throw new ArithmeticException("Rounding is necessary");
        };
        if (increment)
            result[0] = result[0].add(BigInteger.valueOf(amount >= 0 ? 1 : -1));
        return result[0].longValueExact();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof TransferUnit other && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "TransferUnit[" + this.id + "]";
    }
}
