package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_PRESENCE_FIELD;
import static dev.beezy.server.api.BeezyProtocol.MAX_PRESENCE_STATE;

/**
 * Discord rich presence (kind 70). Shown only if the player enabled Discord Rich Presence in
 * Beezy, and sent to Discord only by their own Discord app.
 *
 * <pre>{@code
 * RichPresence p = RichPresence.builder("Bed Wars").mode("Solo").map("Lighthouse")
 *         .state("In game").team(1, 4).build();
 * }</pre>
 */
public final class RichPresence {

    private final String game;
    private final String mode;
    private final String map;
    private final String state;
    private final int teamSize;
    private final int teamMax;

    private RichPresence(Builder b) {
        this.game = Check.optStr(b.game, MAX_PRESENCE_FIELD, "presence game");
        this.mode = Check.optStr(b.mode, MAX_PRESENCE_FIELD, "presence mode");
        this.map = Check.optStr(b.map, MAX_PRESENCE_FIELD, "presence map");
        this.state = Check.optStr(b.state, MAX_PRESENCE_STATE, "presence state");
        this.teamSize = Check.range(b.teamSize, 0, 255, "presence team size");
        this.teamMax = Check.range(b.teamMax, 0, 255, "presence team max");
    }

    public static Builder builder(String game) {
        return new Builder(game);
    }

    public String game() {
        return game;
    }

    public String mode() {
        return mode;
    }

    public String map() {
        return map;
    }

    public String state() {
        return state;
    }

    public int teamSize() {
        return teamSize;
    }

    /** 0 means no team is shown. */
    public int teamMax() {
        return teamMax;
    }

    public static final class Builder {
        private final String game;
        private String mode = "";
        private String map = "";
        private String state = "";
        private int teamSize;
        private int teamMax;

        private Builder(String game) {
            this.game = game;
        }

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder map(String map) {
            this.map = map;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /** Team size and maximum, 0..255 each; max 0 (the default) shows no team. */
        public Builder team(int size, int max) {
            this.teamSize = size;
            this.teamMax = max;
            return this;
        }

        /** @throws IllegalArgumentException if a field is over its cap or out of range */
        public RichPresence build() {
            return new RichPresence(this);
        }
    }
}
