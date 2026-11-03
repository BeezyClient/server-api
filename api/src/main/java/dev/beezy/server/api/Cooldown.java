package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_COOLDOWN_MS;
import static dev.beezy.server.api.BeezyProtocol.MAX_ID;
import static dev.beezy.server.api.BeezyProtocol.MAX_ITEM_ICON;
import static dev.beezy.server.api.BeezyProtocol.MAX_LABEL;

/**
 * A cooldown bar (kind 30). It counts down from when the client receives it and disappears at 0;
 * the client shows at most 16.
 *
 * <pre>{@code
 * Cooldown pearl = new Cooldown("pearl", "Ender Pearl", "minecraft:ender_pearl", 15_000);
 * }</pre>
 */
public final class Cooldown {

    private final String id;
    private final String label;
    private final String itemIcon;
    private final long durationMs;

    /**
     * @param itemIcon   item id such as {@code minecraft:ender_pearl}; empty or null for none
     * @param durationMs 0 to 3 600 000 (one hour)
     * @throws IllegalArgumentException if a field is over its cap or out of range
     */
    public Cooldown(String id, String label, String itemIcon, long durationMs) {
        this.id = Check.id(id, MAX_ID, "cooldown id");
        this.label = Check.str(label, MAX_LABEL, "cooldown label");
        this.itemIcon = Check.optStr(itemIcon, MAX_ITEM_ICON, "cooldown item icon");
        this.durationMs = Check.range(durationMs, 0, MAX_COOLDOWN_MS, "cooldown duration ms");
    }

    /** A cooldown without an icon. */
    public static Cooldown of(String id, String label, long durationMs) {
        return new Cooldown(id, label, "", durationMs);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String itemIcon() {
        return itemIcon;
    }

    public long durationMs() {
        return durationMs;
    }
}
