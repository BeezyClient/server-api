package dev.beezy.protocol.packet;

import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.WireReader;
import dev.beezy.protocol.WireWriter;
import dev.beezy.stability.Stable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Kind 150: what the Saturation display shows for items (1-64 entries). */
@Stable
public final class FoodValuesPacket extends AbstractPacket {

    /** One item's food value. */
    @Stable
    public static final class Entry {
        private final String item;
        private final int hunger;
        private final float saturation;

        /**
         * @param item item id, 1-64 bytes
         * @param hunger 0-20
         * @param saturation 0-20, finite
         */
        public Entry(String item, int hunger, float saturation) {
            this.item = Validate.id(item, Protocol.MAX_ITEM, "food item");
            this.hunger = Validate.range(hunger, 0, Protocol.MAX_HUNGER, "food hunger");
            Validate.finite(saturation, "food saturation");
            if (saturation < 0 || saturation > Protocol.MAX_SATURATION) {
                throw new IllegalArgumentException("food saturation is " + saturation + ", must be 0..20");
            }
            this.saturation = saturation;
        }

        /** @return the item id */
        public String item() {
            return item;
        }

        /** @return the hunger */
        public int hunger() {
            return hunger;
        }

        /** @return the saturation */
        public float saturation() {
            return saturation;
        }
    }

    private final List<Entry> entries;

    /** @param entries 1-64 entries */
    public FoodValuesPacket(List<Entry> entries) {
        super(PacketKind.FOOD_VALUES);
        Validate.notNull(entries, "food values");
        Validate.range(entries.size(), 1, Protocol.MAX_FOOD_VALUES, "food value count");
        for (Entry e : entries) {
            Validate.notNull(e, "food value");
        }
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    /** @return the entries */
    public List<Entry> entries() {
        return entries;
    }

    @Override
    public void write(WireWriter out) {
        out.u8(entries.size());
        for (Entry e : entries) {
            out.str(e.item).u8(e.hunger).f32(e.saturation);
        }
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static FoodValuesPacket read(PacketKind kind, WireReader in) {
        int count = Validate.range(in.u8(), 1, Protocol.MAX_FOOD_VALUES, "food value count");
        List<Entry> entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(in.str(), in.u8(), in.f32()));
        }
        return new FoodValuesPacket(entries);
    }
}
