package dev.beezy.server.api;

import java.util.EnumSet;
import java.util.Set;

/** What a Beezy client says when it joins: its Minecraft version, Beezy version and enabled features. */
public final class Hello {

    private final String minecraftVersion;
    private final String beezyVersion;
    private final int features;

    public Hello(String minecraftVersion, String beezyVersion, int features) {
        this.minecraftVersion = Check.notNull(minecraftVersion, "minecraftVersion");
        this.beezyVersion = Check.notNull(beezyVersion, "beezyVersion");
        this.features = features;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String beezyVersion() {
        return beezyVersion;
    }

    /** The raw {@code u32} feature bits (bits this library does not know yet are kept). */
    public int featureBits() {
        return features;
    }

    public boolean has(Feature feature) {
        return (features & feature.mask()) != 0;
    }

    /** The known features that are on. */
    public Set<Feature> features() {
        EnumSet<Feature> set = EnumSet.noneOf(Feature.class);
        for (Feature f : Feature.values()) {
            if (has(f)) {
                set.add(f);
            }
        }
        return set;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Hello)) {
            return false;
        }
        Hello h = (Hello) o;
        return features == h.features && minecraftVersion.equals(h.minecraftVersion) && beezyVersion.equals(h.beezyVersion);
    }

    @Override
    public int hashCode() {
        return (minecraftVersion.hashCode() * 31 + beezyVersion.hashCode()) * 31 + features;
    }

    @Override
    public String toString() {
        return "Hello{minecraft=" + minecraftVersion + ", beezy=" + beezyVersion + ", features=" + features() + "}";
    }
}
