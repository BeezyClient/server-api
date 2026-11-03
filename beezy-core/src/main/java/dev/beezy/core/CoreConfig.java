package dev.beezy.core;

import dev.beezy.api.DeliveryPolicy;
import dev.beezy.protocol.Validate;
import java.time.Clock;
import java.time.Duration;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/** Settings of a {@link BeezyCore}. Defaults match the client's limits. */
public final class CoreConfig {

    final DeliveryPolicy policy;
    final Clock clock;
    final boolean debug;
    final Duration transferTimeout;
    final int notificationBurst;
    final Duration notificationWindow;
    final int transferBurst;
    final Duration transferWindow;
    final Function<Component, String> componentSerializer;

    private CoreConfig(Builder b) {
        this.policy = b.policy;
        this.clock = b.clock;
        this.debug = b.debug;
        this.transferTimeout = b.transferTimeout;
        this.notificationBurst = b.notificationBurst;
        this.notificationWindow = b.notificationWindow;
        this.transferBurst = b.transferBurst;
        this.transferWindow = b.transferWindow;
        this.componentSerializer = b.componentSerializer;
    }

    /** @return the defaults */
    public static CoreConfig defaults() {
        return builder().build();
    }

    /** @return a builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builds a {@link CoreConfig}. */
    public static final class Builder {
        private DeliveryPolicy policy = DeliveryPolicy.SKIP;
        private Clock clock = Clock.systemUTC();
        private boolean debug;
        private Duration transferTimeout = Duration.ofSeconds(30);
        private int notificationBurst = 5;
        private Duration notificationWindow = Duration.ofSeconds(10);
        private int transferBurst = 1;
        private Duration transferWindow = Duration.ofMinutes(1);
        private Function<Component, String> componentSerializer = ComponentJson::gson;

        private Builder() {
        }

        /**
         * @param policy skip or throw
         * @return this
         */
        public Builder policy(DeliveryPolicy policy) {
            this.policy = Validate.notNull(policy, "policy");
            return this;
        }

        /**
         * @param clock the clock (tests)
         * @return this
         */
        public Builder clock(Clock clock) {
            this.clock = Validate.notNull(clock, "clock");
            return this;
        }

        /**
         * @param debug log every skipped send at INFO
         * @return this
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * @param timeout how long a transfer waits for its answer
         * @return this
         */
        public Builder transferTimeout(Duration timeout) {
            this.transferTimeout = Validate.notNull(timeout, "timeout");
            return this;
        }

        /**
         * @param burst how many
         * @param window per how long
         * @return this
         */
        public Builder notificationRate(int burst, Duration window) {
            this.notificationBurst = burst;
            this.notificationWindow = window;
            return this;
        }

        /**
         * @param burst how many
         * @param window per how long
         * @return this
         */
        public Builder transferRate(int burst, Duration window) {
            this.transferBurst = burst;
            this.transferWindow = window;
            return this;
        }

        /**
         * @param serializer component to JSON
         * @return this
         */
        public Builder componentSerializer(Function<Component, String> serializer) {
            this.componentSerializer = Validate.notNull(serializer, "serializer");
            return this;
        }

        /** @return the config */
        public CoreConfig build() {
            return new CoreConfig(this);
        }
    }
}
