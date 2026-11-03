package dev.beezy.core;

import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.FoodValue;
import dev.beezy.api.display.HeightLimit;
import dev.beezy.api.display.Notification;
import dev.beezy.api.display.RichPresence;
import dev.beezy.api.display.TeamMember;
import dev.beezy.api.service.FoodService;
import dev.beezy.api.service.HeightLimitService;
import dev.beezy.api.service.NickHiderService;
import dev.beezy.api.service.NotificationService;
import dev.beezy.api.service.RichPresenceService;
import dev.beezy.api.service.SettingsService;
import dev.beezy.api.service.TeamService;
import dev.beezy.api.service.TntService;
import dev.beezy.api.settings.ModuleSettings;
import dev.beezy.api.settings.SettingKey;
import dev.beezy.api.settings.SettingType;
import dev.beezy.api.value.Color;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketCodec;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.Protocol;
import dev.beezy.protocol.Validate;
import dev.beezy.protocol.packet.EmptyPacket;
import dev.beezy.protocol.packet.FoodValuesPacket;
import dev.beezy.protocol.packet.HeightLimitPacket;
import dev.beezy.protocol.packet.ModuleSettingsPacket;
import dev.beezy.protocol.packet.ModuleSettingsReleasePacket;
import dev.beezy.protocol.packet.NickHiderPacket;
import dev.beezy.protocol.packet.NotificationPacket;
import dev.beezy.protocol.packet.RichPresencePacket;
import dev.beezy.protocol.packet.SettingEntry;
import dev.beezy.protocol.packet.TeamPacket;
import dev.beezy.protocol.packet.TntCountdownPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/** The services that hold one value (or one map) per user. State is guarded by the session lock. */
final class ValueServices {

    private ValueServices() {
    }

    /** Something the session forgets on Reset and re-sends on resync. */
    interface Stateful {
        /** Caller holds the lock. */
        void forget();

        /** Caller holds the lock; the feature is checked by the implementation. */
        void resend();
    }

    // ---- notifications ------------------------------------------------------------------------

    static final class Notifications implements NotificationService, Stateful {
        private final Session s;
        private final RateLimiter limiter;

        Notifications(Session s) {
            this.s = s;
            this.limiter = new RateLimiter(s.core.clock(), s.core.config.notificationBurst, s.core.config.notificationWindow);
        }

        @Override
        public SendResult send(Notification n) {
            Validate.notNull(n, "notification");
            byte[] payload = PacketCodec.encode(new NotificationPacket(n.title(), n.text(), n.level().ordinal(),
                    (int) n.duration().getSeconds()));
            synchronized (s) {
                SendResult r = s.check(Feature.NOTIFICATIONS);
                if (!r.isSent()) {
                    return s.refuse(r, Feature.NOTIFICATIONS, "notification", null, 0);
                }
                Duration wait = limiter.tryAcquire();
                if (!wait.isZero()) {
                    return s.refuse(SendResult.RATE_LIMITED, Feature.NOTIFICATIONS, "notification", wait, 0);
                }
                s.send(payload);
                return SendResult.SENT;
            }
        }

        @Override
        public void forget() {
            // the window is about the client's counter, which a Reset does not clear
        }

        @Override
        public void resend() {
        }
    }

    // ---- team ---------------------------------------------------------------------------------

    static final class Team implements TeamService, Stateful {
        private final Session s;
        private List<TeamMember> current = Collections.emptyList();

        Team(Session s) {
            this.s = s;
        }

        private static TeamPacket packet(Collection<TeamMember> members) {
            List<TeamPacket.Member> out = new ArrayList<TeamPacket.Member>(members.size());
            for (TeamMember m : members) {
                Validate.notNull(m, "team member");
                int argb = Wire.color(m.color());
                out.add(m.position().isPresent()
                        ? new TeamPacket.Member(m.uuid(), m.name(), argb, m.position().get().x(), m.position().get().y(),
                                m.position().get().z(), Wire.world(m.world()))
                        : new TeamPacket.Member(m.uuid(), m.name(), argb));
            }
            return new TeamPacket(out);
        }

        @Override
        public SendResult set(Collection<TeamMember> members) {
            Validate.notNull(members, "team members");
            final List<TeamMember> copy = Collections.unmodifiableList(new ArrayList<TeamMember>(members));
            return s.deliver(Feature.TEAM, "team", packet(copy), () -> current = copy);
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.TEAM, "team", EmptyPacket.of(PacketKind.TEAM_CLEAR), this::forget);
        }

        @Override
        public List<TeamMember> current() {
            synchronized (s) {
                return current;
            }
        }

        @Override
        public void forget() {
            current = Collections.emptyList();
        }

        @Override
        public void resend() {
            if (!current.isEmpty() && s.check(Feature.TEAM).isSent()) {
                s.send(PacketCodec.encode(packet(current)));
            }
        }
    }

    // ---- rich presence ------------------------------------------------------------------------

    static final class Presence implements RichPresenceService, Stateful {
        private final Session s;
        private RichPresence current;

        Presence(Session s) {
            this.s = s;
        }

        private static Packet packet(RichPresence p) {
            return new RichPresencePacket(p.game(), p.mode(), p.map(), p.state(), p.teamSize(), p.teamMax());
        }

        @Override
        public SendResult set(final RichPresence presence) {
            Validate.notNull(presence, "rich presence");
            return s.deliver(Feature.RICH_PRESENCE, "rich presence", packet(presence), () -> current = presence);
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.RICH_PRESENCE, "rich presence", EmptyPacket.of(PacketKind.RICH_PRESENCE_CLEAR), this::forget);
        }

        @Override
        public Optional<RichPresence> current() {
            synchronized (s) {
                return Optional.ofNullable(current);
            }
        }

        @Override
        public void forget() {
            current = null;
        }

        @Override
        public void resend() {
            if (current != null && s.check(Feature.RICH_PRESENCE).isSent()) {
                s.send(PacketCodec.encode(packet(current)));
            }
        }
    }

    // ---- nick hider ---------------------------------------------------------------------------

    static final class Nick implements NickHiderService, Stateful {
        private final Session s;
        private String current;

        Nick(Session s) {
            this.s = s;
        }

        @Override
        public SendResult set(final String nick) {
            return s.deliver(Feature.NICK_HIDER, "nick", new NickHiderPacket(nick), () -> current = nick);
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.NICK_HIDER, "nick", EmptyPacket.of(PacketKind.NICK_HIDER_CLEAR), this::forget);
        }

        @Override
        public Optional<String> current() {
            synchronized (s) {
                return Optional.ofNullable(current);
            }
        }

        @Override
        public void forget() {
            current = null;
        }

        @Override
        public void resend() {
            if (current != null && s.check(Feature.NICK_HIDER).isSent()) {
                s.send(PacketCodec.encode(new NickHiderPacket(current)));
            }
        }
    }

    // ---- food values --------------------------------------------------------------------------

    static final class Food implements FoodService, Stateful {
        /** Items the client keeps at once. */
        static final int LIMIT = 256;
        private final Session s;
        private final Map<String, FoodValue> current = new LinkedHashMap<String, FoodValue>();

        Food(Session s) {
            this.s = s;
        }

        private static FoodValuesPacket packet(Collection<FoodValue> values) {
            List<FoodValuesPacket.Entry> out = new ArrayList<FoodValuesPacket.Entry>(values.size());
            for (FoodValue v : values) {
                Validate.notNull(v, "food value");
                out.add(new FoodValuesPacket.Entry(v.item(), v.hunger(), v.saturation()));
            }
            return new FoodValuesPacket(out);
        }

        @Override
        public SendResult set(Collection<FoodValue> values) {
            Validate.notNull(values, "food values");
            final List<FoodValue> copy = new ArrayList<FoodValue>(values);
            byte[] payload = PacketCodec.encode(packet(copy));
            synchronized (s) {
                SendResult r = s.check(Feature.FOOD_VALUES);
                if (r.isSent()) {
                    int added = 0;
                    for (FoodValue v : copy) {
                        if (!current.containsKey(v.item())) {
                            added++;
                        }
                    }
                    if (current.size() + added > LIMIT) {
                        r = SendResult.LIMIT_REACHED;
                    }
                }
                if (!r.isSent()) {
                    return s.refuse(r, Feature.FOOD_VALUES, "food values", null, LIMIT);
                }
                s.send(payload);
                for (FoodValue v : copy) {
                    current.put(v.item(), v);
                }
                return SendResult.SENT;
            }
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.FOOD_VALUES, "food values", EmptyPacket.of(PacketKind.FOOD_VALUES_CLEAR), this::forget);
        }

        @Override
        public Map<String, FoodValue> current() {
            synchronized (s) {
                return Collections.unmodifiableMap(new LinkedHashMap<String, FoodValue>(current));
            }
        }

        @Override
        public void forget() {
            current.clear();
        }

        @Override
        public void resend() {
            if (current.isEmpty() || !s.check(Feature.FOOD_VALUES).isSent()) {
                return;
            }
            List<FoodValue> all = new ArrayList<FoodValue>(current.values());
            for (int i = 0; i < all.size(); i += Protocol.MAX_FOOD_VALUES) {
                s.send(PacketCodec.encode(packet(all.subList(i, Math.min(all.size(), i + Protocol.MAX_FOOD_VALUES)))));
            }
        }
    }

    // ---- TNT ----------------------------------------------------------------------------------

    static final class Tnt implements TntService, Stateful {
        private final Session s;
        private int current;

        Tnt(Session s) {
            this.s = s;
        }

        @Override
        public SendResult setFuseTicks(final int ticks) {
            return s.deliver(Feature.TNT_COUNTDOWN, "tnt countdown", new TntCountdownPacket(ticks), () -> current = ticks);
        }

        @Override
        public SendResult setFuse(Duration fuse) {
            Validate.notNull(fuse, "tnt fuse");
            long ticks = (fuse.toMillis() + 25) / 50;
            return setFuseTicks((int) Validate.range(ticks, Protocol.MIN_TNT_TICKS, Protocol.MAX_TNT_TICKS, "tnt ticks"));
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.TNT_COUNTDOWN, "tnt countdown", EmptyPacket.of(PacketKind.TNT_COUNTDOWN_CLEAR), this::forget);
        }

        @Override
        public OptionalInt current() {
            synchronized (s) {
                return current == 0 ? OptionalInt.empty() : OptionalInt.of(current);
            }
        }

        @Override
        public void forget() {
            current = 0;
        }

        @Override
        public void resend() {
            if (current != 0 && s.check(Feature.TNT_COUNTDOWN).isSent()) {
                s.send(PacketCodec.encode(new TntCountdownPacket(current)));
            }
        }
    }

    // ---- height limit -------------------------------------------------------------------------

    static final class Height implements HeightLimitService, Stateful {
        private final Session s;
        private final Map<String, HeightLimit> current = new LinkedHashMap<String, HeightLimit>();

        Height(Session s) {
            this.s = s;
        }

        private static Packet packet(HeightLimit l) {
            return new HeightLimitPacket(Wire.world(l.world()), l.minY(), l.maxY());
        }

        @Override
        public SendResult set(final HeightLimit limit) {
            Validate.notNull(limit, "height limit");
            return s.deliver(Feature.HEIGHT_LIMIT, "height limit", packet(limit), () -> current.put(Wire.world(limit.world()), limit));
        }

        @Override
        public SendResult clear() {
            return s.deliver(Feature.HEIGHT_LIMIT, "height limit", EmptyPacket.of(PacketKind.HEIGHT_LIMIT_CLEAR), this::forget);
        }

        @Override
        public Collection<HeightLimit> current() {
            synchronized (s) {
                return Collections.unmodifiableList(new ArrayList<HeightLimit>(current.values()));
            }
        }

        @Override
        public void forget() {
            current.clear();
        }

        @Override
        public void resend() {
            if (!current.isEmpty() && s.check(Feature.HEIGHT_LIMIT).isSent()) {
                for (HeightLimit l : current.values()) {
                    s.send(PacketCodec.encode(packet(l)));
                }
            }
        }
    }

    // ---- module settings ----------------------------------------------------------------------

    static final class Settings implements SettingsService, Stateful {
        private final Session s;
        private final Map<SettingKey<?>, Object> current = new LinkedHashMap<SettingKey<?>, Object>();

        Settings(Session s) {
            this.s = s;
        }

        static SettingEntry entry(SettingKey<?> k, Object v) {
            SettingType<?> t = k.type();
            if (t == SettingType.BOOL) {
                return SettingEntry.bool(k.module(), k.option(), (Boolean) v);
            } else if (t == SettingType.INT) {
                return SettingEntry.integer(k.module(), k.option(), (Integer) v);
            } else if (t == SettingType.DECIMAL) {
                return SettingEntry.decimal(k.module(), k.option(), (Double) v);
            } else if (t == SettingType.TEXT) {
                return SettingEntry.text(k.module(), k.option(), (String) v);
            }
            return SettingEntry.color(k.module(), k.option(), ((Color) v).argb());
        }

        private static ModuleSettingsPacket packet(Map<SettingKey<?>, Object> values) {
            List<SettingEntry> entries = new ArrayList<SettingEntry>(values.size());
            for (Map.Entry<SettingKey<?>, Object> e : values.entrySet()) {
                entries.add(entry(e.getKey(), e.getValue()));
            }
            return new ModuleSettingsPacket(entries);
        }

        @Override
        public <T> SendResult set(SettingKey<T> key, T value) {
            return apply(ModuleSettings.builder().set(key, value).build());
        }

        @Override
        public SendResult apply(ModuleSettings settings) {
            Validate.notNull(settings, "module settings");
            final Map<SettingKey<?>, Object> values = settings.values();
            return s.deliver(Feature.MODULE_SETTINGS, "module settings", packet(values), () -> {
                for (Map.Entry<SettingKey<?>, Object> e : values.entrySet()) {
                    current.remove(e.getKey());
                    current.put(e.getKey(), e.getValue());
                }
            });
        }

        @Override
        public SendResult release(Collection<? extends SettingKey<?>> keys) {
            Validate.notNull(keys, "setting keys");
            Validate.range(keys.size(), 1, Protocol.MAX_SETTINGS, "setting key count");
            final List<SettingKey<?>> copy = new ArrayList<SettingKey<?>>(keys);
            List<ModuleSettingsReleasePacket.Key> wire = new ArrayList<ModuleSettingsReleasePacket.Key>(copy.size());
            for (SettingKey<?> k : copy) {
                wire.add(new ModuleSettingsReleasePacket.Key(Validate.notNull(k, "setting key").module(), k.option()));
            }
            return s.deliver(Feature.MODULE_SETTINGS, "module settings", new ModuleSettingsReleasePacket(wire), () -> {
                for (SettingKey<?> k : copy) {
                    current.remove(k);
                }
            });
        }

        @Override
        public SendResult releaseAll() {
            return s.deliver(Feature.MODULE_SETTINGS, "module settings", ModuleSettingsReleasePacket.all(), this::forget);
        }

        @Override
        public ModuleSettings overrides() {
            synchronized (s) {
                ModuleSettings.Builder b = ModuleSettings.builder();
                for (Map.Entry<SettingKey<?>, Object> e : current.entrySet()) {
                    put(b, e.getKey(), e.getValue());
                }
                return b.build();
            }
        }

        private static <T> void put(ModuleSettings.Builder b, SettingKey<T> key, Object value) {
            b.set(key, key.type().javaType().cast(value));
        }

        @Override
        public <T> Optional<T> override(SettingKey<T> key) {
            synchronized (s) {
                Object v = current.get(key);
                return v != null && key.type().javaType().isInstance(v) ? Optional.of(key.type().javaType().cast(v)) : Optional.<T>empty();
            }
        }

        @Override
        public void forget() {
            current.clear();
        }

        @Override
        public void resend() {
            if (current.isEmpty() || !s.check(Feature.MODULE_SETTINGS).isSent()) {
                return;
            }
            List<Map.Entry<SettingKey<?>, Object>> all = new ArrayList<Map.Entry<SettingKey<?>, Object>>(current.entrySet());
            for (int i = 0; i < all.size(); i += Protocol.MAX_SETTINGS) {
                Map<SettingKey<?>, Object> chunk = new LinkedHashMap<SettingKey<?>, Object>();
                for (Map.Entry<SettingKey<?>, Object> e : all.subList(i, Math.min(all.size(), i + Protocol.MAX_SETTINGS))) {
                    chunk.put(e.getKey(), e.getValue());
                }
                s.send(PacketCodec.encode(packet(chunk)));
            }
        }
    }
}
