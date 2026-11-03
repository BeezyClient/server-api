package dev.beezy.server.bukkit;

import dev.beezy.server.api.Beam;
import dev.beezy.server.api.BeezyPlayer;
import dev.beezy.server.api.BeezyServerApi;
import dev.beezy.server.api.Border;
import dev.beezy.server.api.FoodValue;
import dev.beezy.server.api.Hologram;
import dev.beezy.server.api.Nametag;
import dev.beezy.server.api.TeamMember;
import dev.beezy.server.api.Waypoint;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Bukkit shortcuts over the platform-neutral API: worlds, locations, entities, players and
 * materials turned into what the client sees.
 *
 * <pre>{@code
 * BeezyPlayer p = BeezyBukkit.player(player);
 * if (p != null) {
 *     p.sendWaypoint(BeezyBukkit.waypoint("spawn", spawnLocation).name("Spawn").color(Color.GREEN).build());
 *     BeezyBukkit.sendGlow(p, boss, Color.RED);
 * }
 * }</pre>
 */
public final class BeezyBukkit {

    private BeezyBukkit() {
    }

    /** The API from the services manager, or null if the Beezy plugin is not enabled. */
    public static BeezyServerApi api() {
        return Bukkit.getServicesManager().load(BeezyServerApi.class);
    }

    /** The player's Beezy view, or null if they are not a Beezy player (or the plugin is off). */
    public static BeezyPlayer player(Player player) {
        BeezyServerApi api = api();
        return api == null ? null : api.player(player.getUniqueId());
    }

    // ---- worlds ----------------------------------------------------------------------------

    /**
     * The key the client knows a world by, e.g. {@code minecraft:overworld}. On servers that
     * expose {@code World#getKey()} (Paper 1.16.5+) this is the world's key. Older servers have no
     * keys, and their clients only know the three vanilla dimensions, so the environment decides.
     */
    public static String world(World world) {
        if (world == null) {
            return "";
        }
        try {
            Method getKey = world.getClass().getMethod("getKey");
            Object key = getKey.invoke(world);
            if (key != null) {
                return key.toString();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no keys on this server version
        }
        switch (world.getEnvironment()) {
            case NETHER:
                return "minecraft:the_nether";
            case THE_END:
                return "minecraft:the_end";
            default:
                return "minecraft:overworld";
        }
    }

    // ---- builders from locations -----------------------------------------------------------

    /** A waypoint builder at that block, in that location's world. */
    public static Waypoint.Builder waypoint(String id, Location at) {
        return Waypoint.builder(id).at(at.getBlockX(), at.getBlockY(), at.getBlockZ()).world(world(at.getWorld()));
    }

    /** A beam builder from that block up, in that location's world. */
    public static Beam.Builder beam(String id, Location at) {
        return Beam.builder(id).at(at.getBlockX(), at.getBlockY(), at.getBlockZ()).world(world(at.getWorld()));
    }

    /** A hologram builder at that exact position, in that location's world. */
    public static Hologram.Builder hologram(String id, Location at) {
        return Hologram.builder(id).at(at.getX(), at.getY(), at.getZ()).world(world(at.getWorld()));
    }

    /** A border builder in that world. */
    public static Border.Builder border(String id, World world) {
        return Border.builder(id).world(world(world));
    }

    /** A border builder around two corners (their x and z), in the first corner's world. */
    public static Border.Builder border(String id, Location corner1, Location corner2) {
        return border(id, corner1.getWorld()).area(corner1.getX(), corner1.getZ(), corner2.getX(), corner2.getZ());
    }

    /** A team member with their exact position. */
    public static TeamMember teamMember(Player mate, Color color) {
        Location loc = mate.getLocation();
        return new TeamMember(mate.getUniqueId(), mate.getName(), color)
                .withPosition(loc.getX(), loc.getY(), loc.getZ(), world(loc.getWorld()));
    }

    // ---- sends taking Bukkit types -----------------------------------------------------------

    /** @param world null for the one the player is in */
    public static boolean sendHeightLimit(BeezyPlayer viewer, World world, int minY, int maxY) {
        return viewer.sendHeightLimit(world == null ? "" : world(world), minY, maxY);
    }

    /** Outlines that entity for {@code viewer} only; {@code color} null for the client's default. */
    public static boolean sendGlow(BeezyPlayer viewer, Entity entity, Color color) {
        return viewer.sendGlow(entity.getUniqueId(), color);
    }

    public static boolean removeGlow(BeezyPlayer viewer, Entity entity) {
        return viewer.removeGlow(entity.getUniqueId());
    }

    /** Replaces the nametag above {@code target} for {@code viewer} only; no lines hides it. */
    public static boolean sendNametag(BeezyPlayer viewer, Player target, String... lines) {
        return viewer.sendNametag(Nametag.of(target.getUniqueId(), lines));
    }

    public static boolean resetNametag(BeezyPlayer viewer, Player target) {
        return viewer.resetNametag(target.getUniqueId());
    }

    // ---- items -------------------------------------------------------------------------------

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
     * The item id the client knows a material by, e.g. {@code minecraft:golden_carrot}: its key on
     * 1.13+, its 1.8-1.12 item id before that.
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

    /** A food value for that material. */
    public static FoodValue food(Material material, int hunger, double saturation) {
        return new FoodValue(item(material), hunger, saturation);
    }

    /** Sends food values built with {@link #food}. */
    public static boolean sendFoodValues(BeezyPlayer viewer, FoodValue... values) {
        return viewer.sendFoodValues(Arrays.asList(values));
    }
}
