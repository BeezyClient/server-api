package dev.beezy.api.settings;

import dev.beezy.api.value.Color;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An immutable set of setting overrides, in the order they were set. Setting the same key again
 * replaces the value. Build once and apply to many users.
 *
 * <pre>{@code
 * ModuleSettings s = ModuleSettings.builder()
 *         .set(SettingKey.integer("zoom", "max_zoom"), 4)
 *         .moduleSwitch("freelook", false)
 *         .build();
 * }</pre>
 */
@Stable
public final class ModuleSettings {

    private static final ModuleSettings EMPTY = new ModuleSettings(new LinkedHashMap<SettingKey<?>, Object>());

    private final Map<SettingKey<?>, Object> values;

    private ModuleSettings(Map<SettingKey<?>, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<SettingKey<?>, Object>(values));
    }

    /** @return a builder */
    public static Builder builder() {
        return new Builder();
    }

    /** @return no settings */
    public static ModuleSettings empty() {
        return EMPTY;
    }

    /** @return the values by key; each value has its key's type */
    public Map<SettingKey<?>, Object> values() {
        return values;
    }

    /**
     * @param key a key
     * @param <T> its type
     * @return its value here
     */
    public <T> Optional<T> get(SettingKey<T> key) {
        Object v = values.get(key);
        return v != null && key.type().javaType().isInstance(v) ? Optional.of(key.type().javaType().cast(v)) : Optional.<T>empty();
    }

    /** @return how many */
    public int size() {
        return values.size();
    }

    /** @return whether there are none */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public String toString() {
        return "ModuleSettings" + values;
    }

    /** Builds {@link ModuleSettings}. */
    public static final class Builder {
        private final Map<SettingKey<?>, Object> values = new LinkedHashMap<SettingKey<?>, Object>();

        private Builder() {
        }

        /**
         * @param key a key
         * @param value its value
         * @param <T> the type
         * @return this
         */
        public <T> Builder set(SettingKey<T> key, T value) {
            Validate.notNull(key, "setting key");
            values.remove(key); // keep the latest position
            values.put(key, key.type().validate(value));
            return this;
        }

        /**
         * @param module module id
         * @param on on or off
         * @return this
         */
        public Builder moduleSwitch(String module, boolean on) {
            return set(SettingKey.moduleSwitch(module), on);
        }

        /**
         * @param module module id
         * @param option option id
         * @param value value
         * @return this
         */
        public Builder set(String module, String option, boolean value) {
            return set(SettingKey.bool(module, option), value);
        }

        /**
         * @param module module id
         * @param option option id
         * @param value value
         * @return this
         */
        public Builder set(String module, String option, int value) {
            return set(SettingKey.integer(module, option), value);
        }

        /**
         * @param module module id
         * @param option option id
         * @param value value
         * @return this
         */
        public Builder set(String module, String option, double value) {
            return set(SettingKey.decimal(module, option), value);
        }

        /**
         * @param module module id
         * @param option option id
         * @param value at most 64 bytes
         * @return this
         */
        public Builder set(String module, String option, String value) {
            return set(SettingKey.text(module, option), value);
        }

        /**
         * @param module module id
         * @param option option id
         * @param value value
         * @return this
         */
        public Builder set(String module, String option, Color value) {
            return set(SettingKey.color(module, option), value);
        }

        /**
         * @param other settings to add (later ones replace)
         * @return this
         */
        public Builder setAll(ModuleSettings other) {
            for (Map.Entry<SettingKey<?>, Object> e : other.values.entrySet()) {
                values.remove(e.getKey());
                values.put(e.getKey(), e.getValue());
            }
            return this;
        }

        /** @return the settings */
        public ModuleSettings build() {
            return new ModuleSettings(values);
        }
    }
}
