package dev.beezy.protocol;

import dev.beezy.stability.Stable;
import java.nio.charset.StandardCharsets;

/**
 * A {@code component} field: a Minecraft JSON text component, already serialized, 1 to 4096
 * UTF-8 bytes. The protocol layer does not parse it; {@code beezy-core} produces it from an
 * Adventure component with {@code GsonComponentSerializer}.
 */
@Stable
public final class JsonText {

    private final String json;

    private JsonText(String json) {
        this.json = json;
    }

    /**
     * @param json JSON text, e.g. {@code {"text":"Shop","color":"gold"}}
     * @return the component field
     * @throws IllegalArgumentException if null, empty or over 4096 UTF-8 bytes
     */
    public static JsonText of(String json) {
        Validate.notNull(json, "component");
        int len = json.getBytes(StandardCharsets.UTF_8).length;
        Validate.range(len, Protocol.MIN_COMPONENT_BYTES, Protocol.MAX_COMPONENT_BYTES, "component JSON length");
        return new JsonText(json);
    }

    /**
     * A plain text component, escaped, for code that has no Adventure.
     *
     * @param text the text
     * @return {@code {"text":"..."}}
     */
    public static JsonText plain(String text) {
        StringBuilder sb = new StringBuilder("{\"text\":\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return of(sb.append("\"}").toString());
    }

    /** @return the JSON */
    public String json() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonText && json.equals(((JsonText) o).json);
    }

    @Override
    public int hashCode() {
        return json.hashCode();
    }

    @Override
    public String toString() {
        return json;
    }
}
