package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/**
 * The player's Discord rich presence while on this server (shown only if they enabled Discord
 * Rich Presence; it goes through their own Discord app, never Beezy's servers).
 */
@Stable
public final class RichPresence {

    private final String game;
    private final String mode;
    private final String map;
    private final String state;
    private final int teamSize;
    private final int teamMax;

    private RichPresence(Builder b) {
        this.game = Validate.str(b.game, Protocol.MAX_PRESENCE_FIELD, "presence game");
        this.mode = Validate.str(b.mode, Protocol.MAX_PRESENCE_FIELD, "presence mode");
        this.map = Validate.str(b.map, Protocol.MAX_PRESENCE_FIELD, "presence map");
        this.state = Validate.str(b.state, Protocol.MAX_PRESENCE_STATE, "presence state");
        this.teamSize = Validate.range(b.teamSize, 0, 255, "presence team size");
        this.teamMax = Validate.range(b.teamMax, 0, 255, "presence team max");
    }

    /**
     * @param game at most 32 bytes, e.g. "Bed Wars"
     * @return a builder
     */
    public static Builder builder(String game) {
        return new Builder(game);
    }

    /** @return the game */
    public String game() {
        return game;
    }

    /** @return the mode */
    public String mode() {
        return mode;
    }

    /** @return the map */
    public String map() {
        return map;
    }

    /** @return the state */
    public String state() {
        return state;
    }

    /** @return the team size */
    public int teamSize() {
        return teamSize;
    }

    /** @return the team max, 0 for none */
    public int teamMax() {
        return teamMax;
    }

    /** Builds a {@link RichPresence}. */
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

        /**
         * @param mode at most 32 bytes
         * @return this
         */
        public Builder mode(String mode) {
            this.mode = mode == null ? "" : mode;
            return this;
        }

        /**
         * @param map at most 32 bytes
         * @return this
         */
        public Builder map(String map) {
            this.map = map == null ? "" : map;
            return this;
        }

        /**
         * @param state at most 64 bytes
         * @return this
         */
        public Builder state(String state) {
            this.state = state == null ? "" : state;
            return this;
        }

        /**
         * @param size 0-255
         * @param max 0-255
         * @return this
         */
        public Builder team(int size, int max) {
            this.teamSize = size;
            this.teamMax = max;
            return this;
        }

        /** @return the presence */
        public RichPresence build() {
            return new RichPresence(this);
        }
    }
}
