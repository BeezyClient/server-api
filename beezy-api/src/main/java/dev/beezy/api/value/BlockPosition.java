package dev.beezy.api.value;

import dev.beezy.stability.Stable;

/** A block position, for waypoints and beams. */
@Stable
public final class BlockPosition {

    private final int x;
    private final int y;
    private final int z;

    private BlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @param x x
     * @param y y
     * @param z z
     * @return the block position
     */
    public static BlockPosition of(int x, int y, int z) {
        return new BlockPosition(x, y, z);
    }

    /** @return x */
    public int x() {
        return x;
    }

    /** @return y */
    public int y() {
        return y;
    }

    /** @return z */
    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockPosition && ((BlockPosition) o).x == x && ((BlockPosition) o).y == y && ((BlockPosition) o).z == z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}
