package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_SETTING_TEXT;

import java.awt.Color;

/** A typed module setting value: bool, int, decimal, text (≤ 64 bytes) or colour. */
public final class SettingValue {

    public enum Type {
        BOOL, INT, DECIMAL, TEXT, COLOR;

        /** Wire value: 0 bool, 1 int, 2 decimal, 3 text, 4 colour. */
        public int id() {
            return ordinal();
        }
    }

    private final Type type;
    private final Object value;

    private SettingValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static SettingValue of(boolean value) {
        return new SettingValue(Type.BOOL, value);
    }

    public static SettingValue of(int value) {
        return new SettingValue(Type.INT, value);
    }

    /** @throws IllegalArgumentException if not finite */
    public static SettingValue of(double value) {
        return new SettingValue(Type.DECIMAL, Check.finite(value, "setting value"));
    }

    /** @throws IllegalArgumentException if null or over 64 bytes */
    public static SettingValue of(String value) {
        return new SettingValue(Type.TEXT, Check.str(value, MAX_SETTING_TEXT, "setting text"));
    }

    /** Sent opaque (alpha ignored). @throws IllegalArgumentException if null */
    public static SettingValue of(Color value) {
        return new SettingValue(Type.COLOR, Check.notNull(value, "setting colour"));
    }

    public Type type() {
        return type;
    }

    /** A {@link Boolean}, {@link Integer}, {@link Double}, {@link String} or {@link Color}, by type. */
    public Object value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SettingValue && type == ((SettingValue) o).type && value.equals(((SettingValue) o).value);
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + value.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
