package dev.beezy.bukkit;

import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.Feature;
import dev.beezy.api.display.TeamMember;
import dev.beezy.api.value.Color;
import dev.beezy.protocol.Protocol;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * Sends every Beezy player their scoreboard team mates (online players in the same team of the
 * scoreboard they see), with positions if enabled. Main thread, on a timer; not on Folia.
 */
final class TeamSync implements Runnable {

    private final Server server;
    private final BeezyClientApi api;
    private final boolean positions;
    /** Users we sent a non-empty team to, so we clear it once when they leave their team. */
    private final Set<UUID> hasTeam = new HashSet<UUID>();

    TeamSync(Server server, BeezyClientApi api, boolean positions) {
        this.server = server;
        this.api = api;
        this.positions = positions;
    }

    void forget(UUID player) {
        hasTeam.remove(player);
    }

    @Override
    public void run() {
        for (BeezyUser user : api.all().supporting(Feature.TEAM)) {
            Player player = server.getPlayer(user.uniqueId());
            if (player == null) {
                continue;
            }
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team == null) {
                if (hasTeam.remove(user.uniqueId())) {
                    user.team().clear();
                }
                continue;
            }
            Color color = colorOf(team);
            List<TeamMember> members = new ArrayList<TeamMember>();
            for (String entry : team.getEntries()) {
                if (members.size() >= Protocol.MAX_TEAM_MEMBERS) {
                    break;
                }
                Player mate = server.getPlayerExact(entry);
                if (mate == null || mate.equals(player)) {
                    continue; // offline, a non-player entry, or themselves
                }
                try {
                    members.add(positions ? BeezyBukkit.teamMember(mate, color) : TeamMember.of(mate.getUniqueId(), mate.getName(), color));
                } catch (IllegalArgumentException ignored) {
                    // a name over 16 bytes (custom name plugins): skip that mate
                }
            }
            user.team().set(members);
            hasTeam.add(user.uniqueId());
        }
    }

    /** {@code Team#getColor()} on 1.12+, else the last colour in the prefix; null if none. */
    private static Color colorOf(Team team) {
        ChatColor color = null;
        try {
            Method getColor = team.getClass().getMethod("getColor");
            Object c = getColor.invoke(team);
            if (c instanceof ChatColor) {
                color = (ChatColor) c;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 1.8-1.11: no team colour
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
        return color == null ? null : BeezyBukkit.color(color).orElse(null);
    }
}
