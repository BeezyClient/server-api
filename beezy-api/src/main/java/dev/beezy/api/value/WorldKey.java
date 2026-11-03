package dev.beezy.api.value;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Locale;

/**
 * A dimension as the client knows it: {@code minecraft:overworld}, {@code minecraft:the_nether},
 * {@code minecraft:the_end}, or a datapack dimension like {@code myserver:lobby}. Not a world
 * folder name: platforms turn their worlds into keys (Bukkit: {@code BeezyBukkit.world(World)}).
 */
@Stable
public final class WorldKey {

    /** {@code minecraft:overworld}. */
    public static final WorldKey OVERWORLD = new WorldKey("minecraft:overworld");
    /** {@code minecraft:the_nether}. */
    public static final WorldKey THE_NETHER = new WorldKey("minecraft:the_nether");
    /** {@code minecraft:the_end}. */
    public static final WorldKey THE_END = new WorldKey("minecraft:the_end");

    private final String key;

    private WorldKey(String key) {
        this.key = key;
    }

    /**
     * @param key {@code namespace:path}, or a bare path for the {@code minecraft} namespace
     * @return the key
     * @throws IllegalArgumentException if empty, over 64 bytes, or not a valid resource key
     */
    public static WorldKey of(String key) {
        Validate.notNull(key, "world key");
        String k = key.indexOf(':') < 0 ? "minecraft:" + key : key;
        Validate.str(k, Protocol.MAX_WORLD, "world key");
        int colon = k.indexOf(':');
        String ns = k.substring(0, colon);
        String path = k.substring(colon + 1);
        if (ns.isEmpty() || path.isEmpty() || !valid(ns, false) || !valid(path, true)) {
            throw new IllegalArgumentException("not a world key: " + key);
        }
        return new WorldKey(k);
    }

    private static boolean valid(String s, boolean path) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || (path && c == '/');
            if (!ok) {
                return false;
            }
        }
        return s.equals(s.toLowerCase(Locale.ROOT));
    }

    /** @return {@code namespace:path} */
    public String asString() {
        return key;
    }

    /** @return the namespace */
    public String namespace() {
        return key.substring(0, key.indexOf(':'));
    }

    /** @return the path */
    public String path() {
        return key.substring(key.indexOf(':') + 1);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WorldKey && ((WorldKey) o).key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }
}
