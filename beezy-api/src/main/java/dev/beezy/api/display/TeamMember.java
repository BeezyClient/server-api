package dev.beezy.api.display;

import dev.beezy.api.value.Color;
import dev.beezy.api.value.Position;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Optional;
import java.util.UUID;

/**
 * A team mate on the minimap and direction HUD. A position (sent every second or two) lets a
 * mate far away still show.
 */
@Stable
public final class TeamMember {

    private final UUID uuid;
    private final String name;
    private final Color color;
    private final Position position;
    private final WorldKey world;

    private TeamMember(UUID uuid, String name, Color color, Position position, WorldKey world) {
        this.uuid = Validate.notNull(uuid, "team member uuid");
        this.name = Validate.str(name, Protocol.MAX_TEAM_MEMBER_NAME, "team member name");
        this.color = color;
        this.position = position;
        this.world = world;
    }

    /**
     * @param uuid the player
     * @param name at most 16 bytes
     * @param color null for default
     * @return the member
     */
    public static TeamMember of(UUID uuid, String name, Color color) {
        return new TeamMember(uuid, name, color, null, null);
    }

    /**
     * @param position where
     * @param world in which world
     * @return the same member with a position
     */
    public TeamMember at(Position position, WorldKey world) {
        return new TeamMember(uuid, name, color, Validate.notNull(position, "position"), Validate.notNull(world, "world"));
    }

    /** @return the player */
    public UUID uuid() {
        return uuid;
    }

    /** @return the name */
    public String name() {
        return name;
    }

    /** @return the colour */
    public Optional<Color> color() {
        return Optional.ofNullable(color);
    }

    /** @return the position, if sent */
    public Optional<Position> position() {
        return Optional.ofNullable(position);
    }

    /** @return the world of the position */
    public Optional<WorldKey> world() {
        return Optional.ofNullable(world);
    }
}
