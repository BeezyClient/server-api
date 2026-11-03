package dev.beezy.api.value;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/** A rectangle on the x/z plane, corners in any order, normalized so min is at most max. */
@Stable
public final class Area {

    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;

    private Area(double x1, double z1, double x2, double z2) {
        Validate.finite(x1, "x1");
        Validate.finite(z1, "z1");
        Validate.finite(x2, "x2");
        Validate.finite(z2, "z2");
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    /**
     * @param x1 a corner x
     * @param z1 a corner z
     * @param x2 the other x
     * @param z2 the other z
     * @return the area
     */
    public static Area of(double x1, double z1, double x2, double z2) {
        return new Area(x1, z1, x2, z2);
    }

    /**
     * @param centerX center x
     * @param centerZ center z
     * @param radius half the side, not negative
     * @return a square
     */
    public static Area around(double centerX, double centerZ, double radius) {
        if (!(radius >= 0)) {
            throw new IllegalArgumentException("radius must not be negative");
        }
        return new Area(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius);
    }

    /** @return min x */
    public double minX() {
        return minX;
    }

    /** @return min z */
    public double minZ() {
        return minZ;
    }

    /** @return max x */
    public double maxX() {
        return maxX;
    }

    /** @return max z */
    public double maxZ() {
        return maxZ;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Area)) {
            return false;
        }
        Area a = (Area) o;
        return Double.compare(minX, a.minX) == 0 && Double.compare(minZ, a.minZ) == 0
                && Double.compare(maxX, a.maxX) == 0 && Double.compare(maxZ, a.maxZ) == 0;
    }

    @Override
    public int hashCode() {
        return ((Double.hashCode(minX) * 31 + Double.hashCode(minZ)) * 31 + Double.hashCode(maxX)) * 31 + Double.hashCode(maxZ);
    }

    @Override
    public String toString() {
        return "Area[" + minX + ", " + minZ + " .. " + maxX + ", " + maxZ + "]";
    }
}
