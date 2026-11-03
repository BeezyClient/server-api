package dev.beezy.api;

import dev.beezy.stability.Internal;
import dev.beezy.stability.Stable;

/** Holds the running {@link BeezyClientApi}, set by the platform plugin while it is enabled. */
@Stable
public final class BeezyProvider {

    private static volatile BeezyClientApi instance;

    private BeezyProvider() {
    }

    /**
     * @return the API
     * @throws IllegalStateException if the Beezy plugin is not enabled
     */
    public static BeezyClientApi get() {
        BeezyClientApi api = instance;
        if (api == null) {
            throw new IllegalStateException("Beezy is not loaded: is the Beezy plugin installed and enabled, and does your plugin depend on it?");
        }
        return api;
    }

    /** @return whether the API is available */
    public static boolean isLoaded() {
        return instance != null;
    }

    /**
     * @param api the API, set by the platform
     * @throws IllegalStateException if one is already set
     */
    @Internal
    public static synchronized void register(BeezyClientApi api) {
        if (instance != null) {
            throw new IllegalStateException("Beezy API already registered");
        }
        instance = api;
    }

    /** Clears the API, on platform shutdown. */
    @Internal
    public static synchronized void unregister() {
        instance = null;
    }
}
