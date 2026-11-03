package dev.beezy.api.value;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Locale;
import net.kyori.adventure.text.format.TextColor;

/**
 * An opaque RGB colour. Where a colour is optional, leaving it out means "the client's default".
 * Converts to and from Adventure's {@link TextColor}.
 */
@Stable
public final class Color {

    /** Chat colour black. */
    public static final Color BLACK = rgb(0x000000);
    /** Chat colour dark blue. */
    public static final Color DARK_BLUE = rgb(0x0000AA);
    /** Chat colour dark green. */
    public static final Color DARK_GREEN = rgb(0x00AA00);
    /** Chat colour dark aqua. */
    public static final Color DARK_AQUA = rgb(0x00AAAA);
    /** Chat colour dark red. */
    public static final Color DARK_RED = rgb(0xAA0000);
    /** Chat colour dark purple. */
    public static final Color DARK_PURPLE = rgb(0xAA00AA);
    /** Chat colour gold. */
    public static final Color GOLD = rgb(0xFFAA00);
    /** Chat colour gray. */
    public static final Color GRAY = rgb(0xAAAAAA);
    /** Chat colour dark gray. */
    public static final Color DARK_GRAY = rgb(0x555555);
    /** Chat colour blue. */
    public static final Color BLUE = rgb(0x5555FF);
    /** Chat colour green. */
    public static final Color GREEN = rgb(0x55FF55);
    /** Chat colour aqua. */
    public static final Color AQUA = rgb(0x55FFFF);
    /** Chat colour red. */
    public static final Color RED = rgb(0xFF5555);
    /** Chat colour light purple. */
    public static final Color LIGHT_PURPLE = rgb(0xFF55FF);
    /** Chat colour yellow. */
    public static final Color YELLOW = rgb(0xFFFF55);
    /** Chat colour white. */
    public static final Color WHITE = rgb(0xFFFFFF);

    private static final String[] NAMES = {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
    };
    private static final int[] VALUES = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };

    private final int rgb;

    private Color(int rgb) {
        this.rgb = rgb & 0xFFFFFF;
    }

    /**
     * @param rgb {@code 0xRRGGBB}; higher bits are ignored
     * @return the colour
     */
    public static Color rgb(int rgb) {
        return new Color(rgb);
    }

    /**
     * @param r 0-255
     * @param g 0-255
     * @param b 0-255
     * @return the colour
     */
    public static Color of(int r, int g, int b) {
        Validate.range(r, 0, 255, "red");
        Validate.range(g, 0, 255, "green");
        Validate.range(b, 0, 255, "blue");
        return new Color(r << 16 | g << 8 | b);
    }

    /**
     * @param hex {@code #rrggbb} or {@code rrggbb}
     * @return the colour
     * @throws IllegalArgumentException if not a hex colour
     */
    public static Color hex(String hex) {
        Validate.notNull(hex, "hex colour");
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) {
            throw new IllegalArgumentException("not a #rrggbb colour: " + hex);
        }
        try {
            return new Color(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a #rrggbb colour: " + hex);
        }
    }

    /**
     * @param name a chat colour name such as {@code red} or {@code dark_aqua} (any case)
     * @return the colour
     * @throws IllegalArgumentException if no chat colour has that name
     */
    public static Color named(String name) {
        String n = Validate.notNull(name, "colour name").toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(n)) {
                return new Color(VALUES[i]);
            }
        }
        throw new IllegalArgumentException("not a colour name: " + name);
    }

    /**
     * @param text {@code #rrggbb} or a chat colour name
     * @return the colour
     * @throws IllegalArgumentException if neither
     */
    public static Color parse(String text) {
        Validate.notNull(text, "colour");
        if (text.startsWith("#")) {
            return hex(text);
        }
        try {
            return named(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a colour: " + text + " (use #rrggbb or a name like red)");
        }
    }

    /**
     * @param color an Adventure colour
     * @return the same colour
     */
    public static Color from(TextColor color) {
        return new Color(Validate.notNull(color, "colour").value());
    }

    /** @return {@code 0xRRGGBB} */
    public int rgb() {
        return rgb;
    }

    /** @return {@code 0xFFRRGGBB}, as sent */
    public int argb() {
        return 0xFF000000 | rgb;
    }

    /** @return {@code #rrggbb} */
    public String asHexString() {
        return String.format("#%06x", rgb);
    }

    /** @return this colour as an Adventure colour */
    public TextColor asTextColor() {
        return TextColor.color(rgb);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Color && ((Color) o).rgb == rgb;
    }

    @Override
    public int hashCode() {
        return rgb;
    }

    @Override
    public String toString() {
        return asHexString();
    }
}
