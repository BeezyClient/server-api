package dev.beezy.server.api;

import static dev.beezy.server.api.BeezyProtocol.MAX_FOOD_ITEM;
import static dev.beezy.server.api.BeezyProtocol.MAX_HUNGER;
import static dev.beezy.server.api.BeezyProtocol.MAX_SATURATION;

/**
 * What Beezy's Saturation display shows for an item (kind 150), for custom foods. The same item
 * replaces; the client keeps at most 256.
 *
 * <pre>{@code
 * FoodValue carrot = new FoodValue("minecraft:golden_carrot", 6, 14.4);
 * }</pre>
 */
public final class FoodValue {

    private final String item;
    private final int hunger;
    private final float saturation;

    /**
     * @param item       item id such as {@code minecraft:golden_carrot} (≤ 64 bytes)
     * @param hunger     0 to 20
     * @param saturation 0 to 20, sent as a 32-bit float
     */
    public FoodValue(String item, int hunger, double saturation) {
        this.item = Check.id(item, MAX_FOOD_ITEM, "food item");
        this.hunger = Check.range(hunger, 0, MAX_HUNGER, "food hunger");
        Check.finite(saturation, "food saturation");
        if (saturation < 0 || saturation > MAX_SATURATION) {
            throw new IllegalArgumentException("food saturation is " + saturation + ", must be 0..20");
        }
        this.saturation = (float) saturation;
    }

    public String item() {
        return item;
    }

    public int hunger() {
        return hunger;
    }

    public float saturation() {
        return saturation;
    }
}
