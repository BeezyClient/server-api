package dev.beezy.api.display;

import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.Stable;
import java.util.Optional;

/** The build height of a dimension, for the Height Limit HUD. One per dimension. */
@Stable
public final class HeightLimit {

    private final WorldKey world;
    private final int minY;
    private final int maxY;

    private HeightLimit(WorldKey world, int minY, int maxY) {
        if (minY > maxY) {
            throw new IllegalArgumentException("height limit min y " + minY + " is above max y " + maxY);
        }
        this.world = world;
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * @param world the dimension
     * @param minY lowest
     * @param maxY highest
     * @return the limit
     */
    public static HeightLimit of(WorldKey world, int minY, int maxY) {
        return new HeightLimit(world, minY, maxY);
    }

    /**
     * @param minY lowest
     * @param maxY highest
     * @return a limit for the world the player is in when it arrives
     */
    public static HeightLimit currentWorld(int minY, int maxY) {
        return new HeightLimit(null, minY, maxY);
    }

    /** @return the world, empty for the player's current one */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }

    /** @return min y */
    public int minY() {
        return minY;
    }

    /** @return max y */
    public int maxY() {
        return maxY;
    }
}
