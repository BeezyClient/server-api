package dev.beezy.api.value;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/** An exact position (finite x, y, z), for holograms and team mates. */
@Stable
public final class Position {

    private final double x;
    private final double y;
    private final double z;

    private Position(double x, double y, double z) {
        this.x = Validate.finite(x, "x");
        this.y = Validate.finite(y, "y");
        this.z = Validate.finite(z, "z");
    }

    /**
     * @param x x
     * @param y y
     * @param z z
     * @return the position
     * @throws IllegalArgumentException if not finite
     */
    public static Position of(double x, double y, double z) {
        return new Position(x, y, z);
    }

    /** @return x */
    public double x() {
        return x;
    }

    /** @return y */
    public double y() {
        return y;
    }

    /** @return z */
    public double z() {
        return z;
    }

    /**
     * @param dx dx
     * @param dy dy
     * @param dz dz
     * @return the moved position
     */
    public Position add(double dx, double dy, double dz) {
        return new Position(x + dx, y + dy, z + dz);
    }

    /** @return the block containing this position */
    public BlockPosition toBlock() {
        return BlockPosition.of((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) {
            return false;
        }
        Position p = (Position) o;
        return Double.compare(x, p.x) == 0 && Double.compare(y, p.y) == 0 && Double.compare(z, p.z) == 0;
    }

    @Override
    public int hashCode() {
        return (Double.hashCode(x) * 31 + Double.hashCode(y)) * 31 + Double.hashCode(z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
