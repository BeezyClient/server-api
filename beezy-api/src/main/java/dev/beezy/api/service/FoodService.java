package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.FoodValue;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/** Food values for the Saturation display (custom foods): 256 items at once; the same item replaces. */
@Stable
@NonExtendable
public interface FoodService {

    /**
     * @param values 1-64 values per call
     * @return the outcome
     */
    SendResult set(Collection<FoodValue> values);

    /**
     * @param values 1-64 values
     * @return the outcome
     */
    default SendResult set(FoodValue... values) {
        return set(Arrays.asList(values));
    }

    /** @return the outcome */
    SendResult clear();

    /** @return the values sent, by item id */
    Map<String, FoodValue> current();
}
