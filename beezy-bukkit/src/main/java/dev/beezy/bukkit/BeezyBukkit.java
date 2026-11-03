package dev.beezy.bukkit;

import dev.beezy.api.BeezyAudience;
import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.display.Beam;
import dev.beezy.api.display.Border;
import dev.beezy.api.display.FoodValue;
import dev.beezy.api.display.GlowHandle;
import dev.beezy.api.display.HeightLimit;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.NametagHandle;
import dev.beezy.api.display.TeamMember;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.value.BlockPosition;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.Position;
import dev.beezy.api.value.WorldKey;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Bukkit types to Beezy values: worlds, locations, entities, players, materials and colours.
 *
 * <pre>{@code
 * BeezyBukkit.user(player).ifPresent(user -> {
 *     user.waypoints().show(BeezyBukkit.waypoint("spawn", spawn).name("Spawn").color(Color.GREEN).build());
 *     BeezyBukkit.glow(user, boss, Color.RED);
 * });
 * }</pre>
 */
@Stable
public final class BeezyBukkit {

    private BeezyBukkit() {
    }

    /**
     * @return the API
     * @throws IllegalStateException if the Beezy plugin is not enabled
     */
    public static BeezyClientApi api() {
        return BeezyClientApi.get();
    }

    /**
     * @param player a player
     * @return their Beezy session, if they use Beezy
     */
    public static Optional<BeezyUser> user(Player player) {
        return api().user(player.getUniqueId());
    }

    /**
     * @param players players
     * @return the Beezy users among them
     */
    public static BeezyAudience audience(Collection<? extends Player> players) {
        List<UUID> ids = new ArrayList<UUID>(players.size());
        for (Player p : players) {
            ids.add(p.getUniqueId());
        }
        return api().audience(ids);
    }

    /**
     * @param world a world
     * @return the Beezy users in it
     */
    public static BeezyAudience audience(World world) {
        return api().world(world(world));
    }

    // ---- worlds and positions --------------------------------------------------------------------

    /**
     * The key the client knows a world by. On servers with {@code World#getKey()} (Paper 1.16.5+)
     * the world's key; before that clients only know the vanilla dimensions, so the environment
     * decides.
     *
     * @param world a world
     * @return its key
     */
    public static WorldKey world(World world) {
        try {
            Object key = world.getClass().getMethod("getKey").invoke(world);
            if (key != null) {
                return WorldKey.of(key.toString());
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no keys on this server version
        }
        switch (world.getEnvironment()) {
            case NETHER:
                return WorldKey.THE_NETHER;
            case THE_END:
                return WorldKey.THE_END;
            default:
                return WorldKey.OVERWORLD;
        }
    }

    /**
     * @param location a location
     * @return its exact position
     */
    public static Position position(Location location) {
        return Position.of(location.getX(), location.getY(), location.getZ());
    }

    /**
     * @param location a location
     * @return its block
     */
    public static BlockPosition block(Location location) {
        return BlockPosition.of(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * @param id id
     * @param at the block and world
     * @return a waypoint builder
     */
    public static Waypoint.Builder waypoint(String id, Location at) {
        return Waypoint.builder(id).at(block(at)).world(world(at.getWorld()));
    }

    /**
     * @param id id
     * @param at the block and world
     * @return a beam builder
     */
    public static Beam.Builder beam(String id, Location at) {
        return Beam.builder(id).at(block(at)).world(world(at.getWorld()));
    }

    /**
     * @param id id
     * @param at the exact position and world
     * @return a hologram builder
     */
    public static Hologram.Builder hologram(String id, Location at) {
        return Hologram.builder(id).at(position(at)).world(world(at.getWorld()));
    }

    /**
     * @param id id
     * @param world the world
     * @return a border builder
     */
    public static Border.Builder border(String id, World world) {
        return Border.builder(id).world(world(world));
    }

    /**
     * @param id id
     * @param corner1 a corner
     * @param corner2 the other
     * @return a border builder in the first corner's world
     */
    public static Border.Builder border(String id, Location corner1, Location corner2) {
        return border(id, corner1.getWorld()).area(corner1.getX(), corner1.getZ(), corner2.getX(), corner2.getZ());
    }

    /**
     * @param world the world
     * @param minY lowest
     * @param maxY highest
     * @return the height limit
     */
    public static HeightLimit heightLimit(World world, int minY, int maxY) {
        return HeightLimit.of(world(world), minY, maxY);
    }

    /**
     * @param mate the player
     * @param color null for default
     * @return a team member with their position
     */
    public static TeamMember teamMember(Player mate, Color color) {
        return TeamMember.of(mate.getUniqueId(), mate.getName(), color).at(position(mate.getLocation()), world(mate.getWorld()));
    }

    // ---- entities ---------------------------------------------------------------------------------

    /**
     * @param viewer who sees it
     * @param entity the entity
     * @param color null for default
     * @return the handle
     */
    public static GlowHandle glow(BeezyUser viewer, Entity entity, Color color) {
        return viewer.glow().glow(entity.getUniqueId(), color);
    }

    /**
     * @param viewer who sees it
     * @param target whose nametag
     * @param lines 0-5 {@code §} strings
     * @return the handle
     */
    public static NametagHandle nametag(BeezyUser viewer, Player target, String... lines) {
        return viewer.nametags().set(target.getUniqueId(), lines);
    }

    /**
     * @param viewer who sees it
     * @param target whose nametag
     * @param lines 0-5 components
     * @return the handle
     */
    public static NametagHandle nametag(BeezyUser viewer, Player target, ComponentLike... lines) {
        return viewer.nametags().set(target.getUniqueId(), lines);
    }

    // ---- colours ----------------------------------------------------------------------------------

    /**
     * @param color a Bukkit colour
     * @return the same colour
     */
    public static Color color(org.bukkit.Color color) {
        return Color.rgb(color.asRGB());
    }

    /**
     * @param color a chat colour
     * @return its RGB, or empty for formats like bold
     */
    public static Optional<Color> color(ChatColor color) {
        if (!color.isColor()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Color.named(color.name()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ---- items ------------------------------------------------------------------------------------

    /** 1.8-1.12 food materials whose name is not their item id. */
    private static final Map<String, String> LEGACY_ITEMS = new HashMap<String, String>();

    static {
        String[] pairs = {
            "MUSHROOM_SOUP", "mushroom_stew", "PORK", "porkchop", "GRILLED_PORK", "cooked_porkchop",
            "RAW_FISH", "fish", "RAW_BEEF", "beef", "RAW_CHICKEN", "chicken", "CARROT_ITEM", "carrot",
            "POTATO_ITEM", "potato",
        };
        for (int i = 0; i < pairs.length; i += 2) {
            LEGACY_ITEMS.put(pairs[i], pairs[i + 1]);
        }
    }

    /**
     * @param material a material
     * @return the item id the client knows: its key on 1.13+, its 1.8-1.12 id before
     */
    public static String item(Material material) {
        try {
            Object key = material.getClass().getMethod("getKey").invoke(material);
            if (key != null) {
                return key.toString();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 1.8-1.12: no keys
        }
        String legacy = LEGACY_ITEMS.get(material.name());
        return "minecraft:" + (legacy != null ? legacy : material.name().toLowerCase(Locale.ROOT));
    }

    /**
     * @param material the food
     * @param hunger 0-20
     * @param saturation 0-20
     * @return the food value
     */
    public static FoodValue food(Material material, int hunger, double saturation) {
        return FoodValue.of(item(material), hunger, saturation);
    }
}
