package dev.beezy.server.bukkit;

import dev.beezy.server.api.BeezyApiCore;
import dev.beezy.server.api.BeezyProtocol;
import dev.beezy.server.api.Feature;
import dev.beezy.server.api.Hello;
import dev.beezy.server.api.TeamMember;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * Sends every Beezy player their scoreboard team mates (online players in the same team of the
 * scoreboard they see), with positions if enabled. Runs on the main thread on a timer.
 */
final class TeamSync implements Runnable {

    private final BeezyApiCore api;
    private final boolean positions;
    /** Players we sent a non-empty team to, so we clear it once when they leave their team. */
    private final Set<UUID> hasTeam = new HashSet<UUID>();

    TeamSync(BeezyApiCore api, boolean positions) {
        this.api = api;
        this.positions = positions;
    }

    void forget(UUID player) {
        hasTeam.remove(player);
    }

    @Override
    public void run() {
        for (UUID id : api.beezyPlayers()) {
            Hello hello = api.helloOf(id);
            Player player = Bukkit.getPlayer(id);
            if (hello == null || player == null || !hello.has(Feature.TEAM)) {
                continue;
            }
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team == null) {
                if (hasTeam.remove(id)) {
                    api.clearTeam(id);
                }
                continue;
            }
            int color = colorOf(team);
            List<TeamMember> members = new ArrayList<TeamMember>();
            for (String entry : team.getEntries()) {
                if (members.size() >= BeezyProtocol.MAX_TEAM_MEMBERS) {
                    break;
                }
                Player mate = Bukkit.getPlayerExact(entry);
                if (mate == null || mate.equals(player)) {
                    continue; // offline, a non-player entry, or themselves
                }
                try {
                    TeamMember m = new TeamMember(mate.getUniqueId(), mate.getName(), color);
                    if (positions) {
                        Location loc = mate.getLocation();
                        m = m.withPosition(loc.getX(), loc.getY(), loc.getZ(), Dimensions.of(loc.getWorld()));
                    }
                    members.add(m);
                } catch (IllegalArgumentException ignored) {
                    // a name over 16 bytes (custom name plugins): skip that mate
                }
            }
            api.sendTeam(id, members);
            hasTeam.add(id);
        }
    }

    /** ARGB of the team color: {@code Team#getColor()} on 1.12+, else the last color in the prefix. */
    private static int colorOf(Team team) {
        ChatColor color = null;
        try {
            Method getColor = team.getClass().getMethod("getColor");
            Object c = getColor.invoke(team);
            if (c instanceof ChatColor) {
                color = (ChatColor) c;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 1.8-1.11: no team color
        }
        if (color == null || !color.isColor()) {
            String last = ChatColor.getLastColors(team.getPrefix());
            for (int i = last.length() - 1; i > 0; i--) {
                ChatColor c = ChatColor.getByChar(last.charAt(i));
                if (c != null && c.isColor()) {
                    color = c;
                    break;
                }
            }
        }
        return color == null || !color.isColor() ? 0 : argb(color);
    }

    /** Vanilla RGB of the 16 chat colors. */
    static int argb(ChatColor color) {
        switch (color) {
            case BLACK: return 0xFF000000;
            case DARK_BLUE: return 0xFF0000AA;
            case DARK_GREEN: return 0xFF00AA00;
            case DARK_AQUA: return 0xFF00AAAA;
            case DARK_RED: return 0xFFAA0000;
            case DARK_PURPLE: return 0xFFAA00AA;
            case GOLD: return 0xFFFFAA00;
            case GRAY: return 0xFFAAAAAA;
            case DARK_GRAY: return 0xFF555555;
            case BLUE: return 0xFF5555FF;
            case GREEN: return 0xFF55FF55;
            case AQUA: return 0xFF55FFFF;
            case RED: return 0xFFFF5555;
            case LIGHT_PURPLE: return 0xFFFF55FF;
            case YELLOW: return 0xFFFFFF55;
            case WHITE: return 0xFFFFFFFF;
            default: return 0;
        }
    }
}
