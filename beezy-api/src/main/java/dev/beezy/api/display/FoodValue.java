package dev.beezy.api.display;

import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.stability.Stable;

/** What the Saturation display shows for an item (custom foods). The same item replaces. */
@Stable
public final class FoodValue {

    private final String item;
    private final int hunger;
    private final float saturation;

    private FoodValue(String item, int hunger, float saturation) {
        this.item = Validate.id(item, Protocol.MAX_ITEM, "food item");
        this.hunger = Validate.range(hunger, 0, Protocol.MAX_HUNGER, "food hunger");
        if (!(saturation >= 0 && saturation <= Protocol.MAX_SATURATION)) {
            throw new IllegalArgumentException("food saturation is " + saturation + ", must be 0..20");
        }
        this.saturation = saturation;
    }

    /**
     * @param item item id, e.g. {@code minecraft:golden_carrot}
     * @param hunger 0-20
     * @param saturation 0-20
     * @return the value
     */
    public static FoodValue of(String item, int hunger, double saturation) {
        return new FoodValue(item, hunger, (float) saturation);
    }

    /** @return the item id */
    public String item() {
        return item;
    }

    /** @return the hunger points */
    public int hunger() {
        return hunger;
    }

    /** @return the saturation */
    public float saturation() {
        return saturation;
    }
}
