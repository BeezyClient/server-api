package dev.beezy.api.settings;

import dev.beezy.api.value.Color;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/**
 * The type of a setting value, as the protocol knows it.
 *
 * @param <T> the Java type
 */
@Stable
public final class SettingType<T> {

    /** On/off. */
    public static final SettingType<Boolean> BOOL = new SettingType<Boolean>("bool", Boolean.class);
    /** A 32-bit integer. */
    public static final SettingType<Integer> INT = new SettingType<Integer>("int", Integer.class);
    /** A finite decimal. */
    public static final SettingType<Double> DECIMAL = new SettingType<Double>("decimal", Double.class);
    /** Text, at most 64 bytes. */
    public static final SettingType<String> TEXT = new SettingType<String>("text", String.class);
    /** A colour. */
    public static final SettingType<Color> COLOR = new SettingType<Color>("color", Color.class);

    private final String name;
    private final Class<T> javaType;

    private SettingType(String name, Class<T> javaType) {
        this.name = name;
        this.javaType = javaType;
    }

    /** @return the Java type of values */
    public Class<T> javaType() {
        return javaType;
    }

    /**
     * @param value a value
     * @return the value, checked
     * @throws IllegalArgumentException if null, not finite (decimal) or over 64 bytes (text)
     */
    public T validate(T value) {
        Validate.notNull(value, "setting value");
        if (!javaType.isInstance(value)) {
            throw new IllegalArgumentException("setting value " + value + " is not a " + name);
        }
        if (this == DECIMAL) {
            Validate.finite((Double) value, "setting value");
        } else if (this == TEXT) {
            Validate.str((String) value, Protocol.MAX_SETTING_TEXT, "setting text");
        }
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
