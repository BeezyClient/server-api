package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.settings.ModuleSettings;
import dev.beezy.api.settings.SettingKey;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Overrides the user's module settings while they stay on this server. They cannot change an
 * overridden setting meanwhile (the menu says "Set by this server") and get their own value back
 * when they leave or it is released. Unknown modules or options and out-of-range values are
 * skipped by the client one at a time.
 *
 * <pre>{@code
 * user.settings().set(SettingKey.integer("zoom", "max_zoom"), 4);
 * user.settings().apply(ModuleSettings.builder().moduleSwitch("freelook", false).build());
 * user.settings().releaseAll();
 * }</pre>
 */
@Stable
@NonExtendable
public interface SettingsService {

    /**
     * @param key the setting
     * @param value its value
     * @param <T> the type
     * @return the outcome
     */
    <T> SendResult set(SettingKey<T> key, T value);

    /**
     * @param settings 1-64 settings
     * @return the outcome
     */
    SendResult apply(ModuleSettings settings);

    /**
     * @param keys 1-64 settings to give back
     * @return the outcome
     */
    SendResult release(Collection<? extends SettingKey<?>> keys);

    /**
     * @param keys 1-64 settings to give back
     * @return the outcome
     */
    default SendResult release(SettingKey<?>... keys) {
        return release(Arrays.asList(keys));
    }

    /** @return the outcome of giving every setting back */
    SendResult releaseAll();

    /** @return what this server currently overrides */
    ModuleSettings overrides();

    /**
     * @param key a setting
     * @param <T> its type
     * @return the overriding value, if any
     */
    <T> Optional<T> override(SettingKey<T> key);
}
