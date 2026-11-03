package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_NAMETAG_LINE;
import static dev.beezy.server.api.BeezyProtocol.MAX_NAMETAG_LINES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The nametag shown above another player, for one viewer only (kind 130): 0 to 5 lines, top
 * first, {@code §} codes kept. No lines hides it. The client keeps at most 128.
 *
 * <pre>{@code
 * beezy.sendNametag(viewer, Nametag.of(target.getUniqueId(), "§c[RED]", target.getName()));
 * beezy.sendNametag(viewer, Nametag.hidden(target.getUniqueId()));
 * }</pre>
 */
public final class Nametag {

    private final UUID player;
    private final List<String> lines;

    /** @throws IllegalArgumentException if there are more than 5 lines or one is over 64 bytes */
    public Nametag(UUID player, List<String> lines) {
        this.player = Check.notNull(player, "nametag player");
        Check.notNull(lines, "nametag lines");
        Check.range(lines.size(), 0, MAX_NAMETAG_LINES, "nametag line count");
        List<String> copy = new ArrayList<String>(lines.size());
        for (String line : lines) {
            copy.add(Check.str(line, MAX_NAMETAG_LINE, "nametag line"));
        }
        this.lines = Collections.unmodifiableList(copy);
    }

    public static Nametag of(UUID player, String... lines) {
        return new Nametag(player, Arrays.asList(Check.notNull(lines, "nametag lines")));
    }

    /** No nametag at all above that player. */
    public static Nametag hidden(UUID player) {
        return new Nametag(player, Collections.<String>emptyList());
    }

    /** The player whose nametag this is. */
    public UUID player() {
        return player;
    }

    public List<String> lines() {
        return lines;
    }
}
