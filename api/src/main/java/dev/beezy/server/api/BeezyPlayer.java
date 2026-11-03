package dev.beezy.server.api;

import java.awt.Color;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * One Beezy player, as a shortcut over {@link BeezyServerApi}. Holds no state of its own: if the
 * player leaves, sends simply return false.
 */
public final class BeezyPlayer {

    private final BeezyServerApi api;
    private final UUID uuid;

    public BeezyPlayer(BeezyServerApi api, UUID uuid) {
        this.api = Check.notNull(api, "api");
        this.uuid = Check.notNull(uuid, "uuid");
    }

    public UUID uuid() {
        return uuid;
    }

    /** The Hello, or null if the player left. */
    public Hello hello() {
        return api.helloOf(uuid);
    }

    /** True if the player is still a Beezy player here and has {@code feature} switched on. */
    public boolean has(Feature feature) {
        Hello h = hello();
        return h != null && h.has(feature);
    }

    public boolean sendWaypoint(Waypoint waypoint) {
        return api.sendWaypoint(uuid, waypoint);
    }

    public boolean removeWaypoint(String id) {
        return api.removeWaypoint(uuid, id);
    }

    public boolean clearWaypoints() {
        return api.clearWaypoints(uuid);
    }

    public boolean sendNotification(Notification notification) {
        return api.sendNotification(uuid, notification);
    }

    public boolean sendCooldown(Cooldown cooldown) {
        return api.sendCooldown(uuid, cooldown);
    }

    public boolean removeCooldown(String id) {
        return api.removeCooldown(uuid, id);
    }

    public boolean clearCooldowns() {
        return api.clearCooldowns(uuid);
    }

    public boolean sendTimer(Timer timer) {
        return api.sendTimer(uuid, timer);
    }

    public boolean removeTimer(String id) {
        return api.removeTimer(uuid, id);
    }

    public boolean clearTimers() {
        return api.clearTimers(uuid);
    }

    /** @param world world key; empty or null for the one the player is in */
    public boolean sendHeightLimit(String world, int minY, int maxY) {
        return api.sendHeightLimit(uuid, world, minY, maxY);
    }

    public boolean clearHeightLimit() {
        return api.clearHeightLimit(uuid);
    }

    public boolean sendTeam(List<TeamMember> members) {
        return api.sendTeam(uuid, members);
    }

    public boolean clearTeam() {
        return api.clearTeam(uuid);
    }

    public boolean sendRichPresence(RichPresence presence) {
        return api.sendRichPresence(uuid, presence);
    }

    public boolean clearRichPresence() {
        return api.clearRichPresence(uuid);
    }

    public boolean sendBeam(Beam beam) {
        return api.sendBeam(uuid, beam);
    }

    public boolean removeBeam(String id) {
        return api.removeBeam(uuid, id);
    }

    public boolean clearBeams() {
        return api.clearBeams(uuid);
    }

    public boolean sendBorder(Border border) {
        return api.sendBorder(uuid, border);
    }

    public boolean removeBorder(String id) {
        return api.removeBorder(uuid, id);
    }

    public boolean clearBorders() {
        return api.clearBorders(uuid);
    }

    /** @param color null for the client's default */
    public boolean sendGlow(UUID entity, Color color) {
        return api.sendGlow(uuid, entity, color);
    }

    public boolean removeGlow(UUID entity) {
        return api.removeGlow(uuid, entity);
    }

    public boolean clearGlow() {
        return api.clearGlow(uuid);
    }

    public boolean sendHologram(Hologram hologram) {
        return api.sendHologram(uuid, hologram);
    }

    public boolean removeHologram(String id) {
        return api.removeHologram(uuid, id);
    }

    public boolean clearHolograms() {
        return api.clearHolograms(uuid);
    }

    /**
     * Module settings for this player:
     * {@code settings().set("zoom", "max_zoom", 4).set("freelook", false).send()} overrides them,
     * {@code settings().release("zoom", "max_zoom")} and {@code settings().releaseAll()} give them back.
     */
    public Settings settings() {
        return new Settings();
    }

    public boolean overrideSettings(ModuleSettings settings) {
        return api.overrideSettings(uuid, settings);
    }

    public boolean releaseSettings(Collection<SettingKey> keys) {
        return api.releaseSettings(uuid, keys);
    }

    public boolean releaseAllSettings() {
        return api.releaseAllSettings(uuid);
    }

    public boolean sendNametag(Nametag nametag) {
        return api.sendNametag(uuid, nametag);
    }

    public boolean resetNametag(UUID target) {
        return api.resetNametag(uuid, target);
    }

    public boolean clearNametags() {
        return api.clearNametags(uuid);
    }

    public boolean sendNick(String nick) {
        return api.sendNick(uuid, nick);
    }

    public boolean clearNick() {
        return api.clearNick(uuid);
    }

    public boolean sendFoodValues(List<FoodValue> values) {
        return api.sendFoodValues(uuid, values);
    }

    public boolean sendFoodValues(FoodValue... values) {
        return api.sendFoodValues(uuid, Arrays.asList(values));
    }

    public boolean clearFoodValues() {
        return api.clearFoodValues(uuid);
    }

    public boolean sendTntCountdown(int ticks) {
        return api.sendTntCountdown(uuid, ticks);
    }

    public boolean sendTntCountdown(Duration fuse) {
        return api.sendTntCountdown(uuid, fuse);
    }

    public boolean clearTntCountdown() {
        return api.clearTntCountdown(uuid);
    }

    /** @return the request's id, echoed in the client's {@link TransferResult}; null if nothing was sent */
    public String transfer(String address) {
        return api.transfer(uuid, address);
    }

    public boolean transfer(String id, String address) {
        return api.transfer(uuid, id, address);
    }

    public boolean reset() {
        return api.reset(uuid);
    }

    /** Collects module settings for this player; {@link #send()} overrides them all in one message. */
    public final class Settings {
        private final ModuleSettings.Builder builder = ModuleSettings.builder();

        private Settings() {
        }

        /** Switches the module on or off. */
        public Settings set(String module, boolean on) {
            builder.set(module, on);
            return this;
        }

        public Settings set(String module, String option, boolean value) {
            builder.set(module, option, value);
            return this;
        }

        public Settings set(String module, String option, int value) {
            builder.set(module, option, value);
            return this;
        }

        public Settings set(String module, String option, double value) {
            builder.set(module, option, value);
            return this;
        }

        /** Text, at most 64 bytes. */
        public Settings set(String module, String option, String value) {
            builder.set(module, option, value);
            return this;
        }

        public Settings set(String module, String option, Color value) {
            builder.set(module, option, value);
            return this;
        }

        public Settings set(SettingKey key, SettingValue value) {
            builder.set(key, value);
            return this;
        }

        /** Overrides everything set so far. @throws IllegalArgumentException if nothing (or over 64) was set */
        public boolean send() {
            return api.overrideSettings(uuid, builder.build());
        }

        /** Gives the module's on/off switch back to the player, now. */
        public boolean release(String module) {
            return api.releaseSettings(uuid, Arrays.asList(SettingKey.of(module)));
        }

        /** Gives that option back to the player, now. */
        public boolean release(String module, String option) {
            return api.releaseSettings(uuid, Arrays.asList(SettingKey.of(module, option)));
        }

        /** Gives those settings back to the player, now. */
        public boolean release(SettingKey... keys) {
            return api.releaseSettings(uuid, Arrays.asList(keys));
        }

        /** Gives every setting this server overrode back to the player, now. */
        public boolean releaseAll() {
            return api.releaseAllSettings(uuid);
        }
    }

    @Override
    public String toString() {
        return "BeezyPlayer{" + uuid + "}";
    }
}
