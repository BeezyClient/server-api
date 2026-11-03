package dev.beezy.api;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Set;

/** What a Beezy client said in its Hello: versions and the features that are on. */
@Stable
public final class ClientInfo {

    private final String minecraftVersion;
    private final String beezyVersion;
    private final int featureBits;
    private final Set<Feature> features;

    /**
     * @param minecraftVersion e.g. {@code 1.21.4}
     * @param beezyVersion e.g. {@code 0.2.0}
     * @param featureBits the raw feature field
     */
    public ClientInfo(String minecraftVersion, String beezyVersion, int featureBits) {
        this.minecraftVersion = Validate.notNull(minecraftVersion, "minecraft version");
        this.beezyVersion = Validate.notNull(beezyVersion, "beezy version");
        this.featureBits = featureBits;
        this.features = Feature.fromBits(featureBits);
    }

    /** @return the client's Minecraft version */
    public String minecraftVersion() {
        return minecraftVersion;
    }

    /** @return the Beezy version */
    public String beezyVersion() {
        return beezyVersion;
    }

    /** @return the raw feature bits, including bits this library does not know */
    public int featureBits() {
        return featureBits;
    }

    /** @return the known features that are on */
    public Set<Feature> features() {
        return features;
    }

    /**
     * @param feature a feature
     * @return whether it is on
     */
    public boolean has(Feature feature) {
        return (featureBits & feature.mask()) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientInfo)) {
            return false;
        }
        ClientInfo c = (ClientInfo) o;
        return featureBits == c.featureBits && minecraftVersion.equals(c.minecraftVersion) && beezyVersion.equals(c.beezyVersion);
    }

    @Override
    public int hashCode() {
        return (minecraftVersion.hashCode() * 31 + beezyVersion.hashCode()) * 31 + featureBits;
    }

    @Override
    public String toString() {
        return "ClientInfo{minecraft=" + minecraftVersion + ", beezy=" + beezyVersion + ", features=" + features + "}";
    }
}
