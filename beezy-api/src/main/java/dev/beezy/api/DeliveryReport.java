package dev.beezy.api;

import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

/** The outcome of sending one thing to an audience: a {@link SendResult} per user. */
@Stable
public final class DeliveryReport {

    private final Map<UUID, SendResult> results;

    /** @param results per user, in audience order */
    public DeliveryReport(Map<UUID, SendResult> results) {
        this.results = Collections.unmodifiableMap(new LinkedHashMap<UUID, SendResult>(Validate.notNull(results, "results")));
    }

    /** @return the result per user */
    public Map<UUID, SendResult> results() {
        return results;
    }

    /** @return how many users were targeted */
    public int total() {
        return results.size();
    }

    /** @return how many got it */
    public int sent() {
        return count(SendResult.SENT);
    }

    /**
     * @param result a result
     * @return how many users had it
     */
    public int count(SendResult result) {
        int n = 0;
        for (SendResult r : results.values()) {
            if (r == result) {
                n++;
            }
        }
        return n;
    }

    /** @return counts by result */
    public Map<SendResult, Integer> counts() {
        Map<SendResult, Integer> m = new EnumMap<SendResult, Integer>(SendResult.class);
        for (SendResult r : results.values()) {
            Integer c = m.get(r);
            m.put(r, c == null ? 1 : c + 1);
        }
        return m;
    }

    @Override
    public String toString() {
        return "DeliveryReport" + counts();
    }
}
