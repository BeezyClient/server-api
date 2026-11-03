package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_DIMENSION;
import static dev.beezy.server.api.BeezyProtocol.MAX_TEAM_MEMBER_NAME;

import java.util.UUID;

/**
 * One mate in a Team message (kind 60). With a position the client can show the mate even when
 * they are too far away to be loaded; send the team again every second or two to move it.
 *
 * <pre>{@code
 * TeamMember mate = new TeamMember(other.getUniqueId(), other.getName(), 0xFF5555FF)
 *         .withPosition(loc.getX(), loc.getY(), loc.getZ(), "minecraft:overworld");
 * }</pre>
 */
public final class TeamMember {

    private final UUID uuid;
    private final String name;
    private final int color;
    private final boolean hasPosition;
    private final double x;
    private final double y;
    private final double z;
    private final String dimension;

    /** A member without a position. {@code color} is ARGB, 0 for the client's default. */
    public TeamMember(UUID uuid, String name, int color) {
        this(uuid, name, color, false, 0, 0, 0, "");
    }

    private TeamMember(UUID uuid, String name, int color, boolean hasPosition, double x, double y, double z, String dimension) {
        this.uuid = Check.notNull(uuid, "team member uuid");
        this.name = Check.str(name, MAX_TEAM_MEMBER_NAME, "team member name");
        this.color = color;
        this.hasPosition = hasPosition;
        this.x = Check.finite(x, "team member x");
        this.y = Check.finite(y, "team member y");
        this.z = Check.finite(z, "team member z");
        this.dimension = Check.optStr(dimension, MAX_DIMENSION, "team member dimension");
    }

    /** The same member with an exact position; {@code dimension} empty or null means the current one. */
    public TeamMember withPosition(double x, double y, double z, String dimension) {
        return new TeamMember(uuid, name, color, true, x, y, z, dimension);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int color() {
        return color;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public String dimension() {
        return dimension;
    }
}
