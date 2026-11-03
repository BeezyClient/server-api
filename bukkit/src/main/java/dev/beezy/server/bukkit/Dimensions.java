package dev.beezy.server.bukkit;

import java.lang.reflect.Method;
import org.bukkit.World;

/** The dimension id the client knows a world by, e.g. {@code minecraft:overworld}. */
final class Dimensions {

    private Dimensions() {
    }

    /**
     * On 1.16+ servers that expose {@code World#getKey()} (Paper; recent Spigot) this is the
     * world's key, which is what the client sees. Older servers have no keys, and their clients
     * only know the three vanilla dimensions, so the environment decides.
     */
    static String of(World world) {
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
}
