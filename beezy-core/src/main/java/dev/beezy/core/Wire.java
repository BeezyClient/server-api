package dev.beezy.core;

import dev.beezy.api.value.Color;
import dev.beezy.api.value.WorldKey;
import dev.beezy.protocol.JsonText;
import dev.beezy.protocol.Protocol;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/** API values to wire fields. */
final class Wire {

    private Wire() {
    }

    static String world(Optional<WorldKey> world) {
        return world.isPresent() ? world.get().asString() : "";
    }

    static int color(Optional<Color> color) {
        return color.isPresent() ? color.get().argb() : 0;
    }

    /** A lifetime in ms: 0 for none, at least 1 ms otherwise (so it never turns into "forever"). */
    static long ms(Optional<Duration> d) {
        return d.isPresent() ? Math.max(1L, d.get().toMillis()) : 0L;
    }

    static List<JsonText> lines(List<Component> lines, Function<Component, String> serializer, String field) {
        List<JsonText> out = new ArrayList<JsonText>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String json = serializer.apply(lines.get(i));
            int len = json.getBytes(StandardCharsets.UTF_8).length;
            if (len > Protocol.MAX_COMPONENT_BYTES) {
                throw new IllegalArgumentException(field + " line " + (i + 1) + " is " + len
                        + " bytes of JSON, at most " + Protocol.MAX_COMPONENT_BYTES + " allowed");
            }
            out.add(JsonText.of(json));
        }
        return out;
    }
}
