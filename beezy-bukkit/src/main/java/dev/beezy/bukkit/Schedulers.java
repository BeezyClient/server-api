package dev.beezy.bukkit;

import dev.beezy.core.spi.Scheduler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * The core's scheduler (used only for transfer-answer timeouts). Bukkit's scheduler where it
 * exists; Folia's global region scheduler on Folia, reached by reflection because the plugin
 * compiles against 1.8.8.
 */
final class Schedulers {

    private Schedulers() {
    }

    static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static Scheduler forServer(Plugin plugin) {
        if (isFolia()) {
            try {
                return new Folia(plugin);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Folia detected but its scheduler is not reachable", e);
            }
        }
        return new BukkitScheduler(plugin);
    }

    static long ticks(Duration delay) {
        return Math.max(1L, (delay.toMillis() + 49) / 50);
    }

    /** Bukkit's main-thread scheduler; {@code runTaskLater} may be called from any thread. */
    static final class BukkitScheduler implements Scheduler {
        private final Plugin plugin;

        BukkitScheduler(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Task schedule(Duration delay, Runnable task) {
            final BukkitTask t = plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks(delay));
            return t::cancel;
        }
    }

    /** Folia's {@code GlobalRegionScheduler.runDelayed(Plugin, Consumer<ScheduledTask>, long)}. */
    static final class Folia implements Scheduler {
        private final Plugin plugin;
        private final Object global;
        private final Method runDelayed;
        private final Method cancel;

        Folia(Plugin plugin) throws ReflectiveOperationException {
            this.plugin = plugin;
            ClassLoader cl = Bukkit.class.getClassLoader();
            Class<?> globalType = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", false, cl);
            Class<?> taskType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask", false, cl);
            this.global = Server.class.getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
            this.runDelayed = globalType.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            this.cancel = taskType.getMethod("cancel");
        }

        @Override
        public Task schedule(Duration delay, final Runnable task) {
            Consumer<Object> body = t -> task.run();
            final Object handle;
            try {
                handle = runDelayed.invoke(global, plugin, body, ticks(delay));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot schedule on Folia", e);
            }
            return () -> {
                try {
                    cancel.invoke(handle);
                } catch (ReflectiveOperationException ignored) {
                    // already ran or cancelled
                }
            };
        }
    }
}
