package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_SETTINGS;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Module settings a server overrides while the player stays on it (kind 120). The player cannot
 * change them meanwhile and the menu says "Set by this server"; leaving, a Reset or a release
 * gives them back. The client skips, one at a time, unknown modules or options, a wrong type and
 * values outside the setting's own range.
 *
 * <pre>{@code
 * player.settings().set("zoom", "max_zoom", 4).set("freelook", false).send();
 * // or, built once and sent to many:
 * ModuleSettings s = ModuleSettings.builder().set("zoom", "max_zoom", 4).set("freelook", false).build();
 * beezy.overrideSettings(id, s);
 * }</pre>
 */
public final class ModuleSettings {

    private final Map<SettingKey, SettingValue> entries;

    private ModuleSettings(Map<SettingKey, SettingValue> entries) {
        Check.range(entries.size(), 1, MAX_SETTINGS, "module setting count");
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<SettingKey, SettingValue>(entries));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 1 to 64 entries, in the order they were set. */
    public Map<SettingKey, SettingValue> entries() {
        return entries;
    }

    /**
     * Collects settings; setting the same module and option again replaces the earlier value.
     * {@code set(module, value)} is the module's on/off switch.
     */
    public static final class Builder {
        private final Map<SettingKey, SettingValue> entries = new LinkedHashMap<SettingKey, SettingValue>();

        Builder() {
        }

        public Builder set(SettingKey key, SettingValue value) {
            entries.put(Check.notNull(key, "setting key"), Check.notNull(value, "setting value"));
            return this;
        }

        /** Switches the module on or off. */
        public Builder set(String module, boolean on) {
            return set(SettingKey.of(module), SettingValue.of(on));
        }

        public Builder set(String module, String option, boolean value) {
            return set(SettingKey.of(module, option), SettingValue.of(value));
        }

        public Builder set(String module, String option, int value) {
            return set(SettingKey.of(module, option), SettingValue.of(value));
        }

        public Builder set(String module, String option, double value) {
            return set(SettingKey.of(module, option), SettingValue.of(value));
        }

        /** Text, at most 64 bytes. */
        public Builder set(String module, String option, String value) {
            return set(SettingKey.of(module, option), SettingValue.of(value));
        }

        public Builder set(String module, String option, Color value) {
            return set(SettingKey.of(module, option), SettingValue.of(value));
        }

        /** @throws IllegalArgumentException if nothing was set or more than 64 settings were */
        public ModuleSettings build() {
            return new ModuleSettings(entries);
        }
    }
}
