package dev.beezy.core;

import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Beam;
import dev.beezy.api.display.BeamHandle;
import dev.beezy.api.display.Border;
import dev.beezy.api.display.BorderHandle;
import dev.beezy.api.display.Cooldown;
import dev.beezy.api.display.CooldownHandle;
import dev.beezy.api.display.Glow;
import dev.beezy.api.display.GlowHandle;
import dev.beezy.api.display.Hologram;
import dev.beezy.api.display.HologramHandle;
import dev.beezy.api.display.Nametag;
import dev.beezy.api.display.NametagHandle;
import dev.beezy.api.display.Timer;
import dev.beezy.api.display.TimerHandle;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.display.WaypointHandle;
import dev.beezy.api.service.BeamService;
import dev.beezy.api.service.BorderService;
import dev.beezy.api.service.CooldownService;
import dev.beezy.api.service.GlowService;
import dev.beezy.api.service.HologramService;
import dev.beezy.api.service.NametagService;
import dev.beezy.api.service.TimerService;
import dev.beezy.api.service.WaypointService;
import dev.beezy.protocol.Packet;
import dev.beezy.protocol.PacketKind;
import dev.beezy.protocol.packet.BeamSetPacket;
import dev.beezy.protocol.packet.BorderSetPacket;
import dev.beezy.protocol.packet.CooldownSetPacket;
import dev.beezy.protocol.packet.GlowSetPacket;
import dev.beezy.protocol.packet.HologramSetPacket;
import dev.beezy.protocol.packet.NametagSetPacket;
import dev.beezy.protocol.packet.TimerSetPacket;
import dev.beezy.protocol.packet.WaypointSetPacket;
import java.time.Duration;
import java.util.Optional;

/** The element services and their handles. */
final class ElementServices {

    private ElementServices() {
    }

    static long life(Optional<Duration> own, long override) {
        return override >= 0 ? override : Wire.ms(own);
    }

    // ---- waypoints --------------------------------------------------------------------------

    static final class Waypoints extends ElementServiceImpl<Waypoint, WaypointHandle> implements WaypointService {
        Waypoints(Session s) {
            super(s, Feature.WAYPOINTS, "waypoint", 64, PacketKind.WAYPOINT_REMOVE, PacketKind.WAYPOINTS_CLEAR);
        }

        @Override
        Packet setPacket(Waypoint w, long lifetimeMs) {
            return new WaypointSetPacket(w.id(), w.name(), w.position().x(), w.position().y(), w.position().z(),
                    Wire.world(w.world()), Wire.color(w.color()), w.beam() ? WaypointSetPacket.FLAG_BEAM : 0,
                    life(w.duration(), lifetimeMs));
        }

        @Override
        Optional<Duration> lifetime(Waypoint w) {
            return w.duration();
        }

        @Override
        HandleImpl<Waypoint> newHandle(Waypoint w) {
            return new WaypointH(this, w);
        }
    }

    static final class WaypointH extends HandleImpl<Waypoint> implements WaypointHandle {
        WaypointH(Waypoints s, Waypoint v) {
            super(s, v);
        }
    }

    // ---- beams --------------------------------------------------------------------------------

    static final class Beams extends ElementServiceImpl<Beam, BeamHandle> implements BeamService {
        Beams(Session s) {
            super(s, Feature.BEAMS, "beam", 32, PacketKind.BEAM_REMOVE, PacketKind.BEAMS_CLEAR);
        }

        @Override
        Packet setPacket(Beam b, long lifetimeMs) {
            return new BeamSetPacket(b.id(), b.position().x(), b.position().y(), b.position().z(),
                    Wire.world(b.world()), Wire.color(b.color()), life(b.duration(), lifetimeMs));
        }

        @Override
        Optional<Duration> lifetime(Beam b) {
            return b.duration();
        }

        @Override
        HandleImpl<Beam> newHandle(Beam b) {
            return new BeamH(this, b);
        }
    }

    static final class BeamH extends HandleImpl<Beam> implements BeamHandle {
        BeamH(Beams s, Beam v) {
            super(s, v);
        }
    }

    // ---- borders ------------------------------------------------------------------------------

    static final class Borders extends ElementServiceImpl<Border, BorderHandle> implements BorderService {
        Borders(Session s) {
            super(s, Feature.BORDERS, "border", 16, PacketKind.BORDER_REMOVE, PacketKind.BORDERS_CLEAR);
        }

        @Override
        Packet setPacket(Border b, long lifetimeMs) {
            int flags = (b.solid() ? BorderSetPacket.FLAG_SOLID : 0) | (b.onlyWhenNear() ? BorderSetPacket.FLAG_ONLY_WHEN_NEAR : 0);
            return new BorderSetPacket(b.id(), Wire.world(b.world()), b.area().minX(), b.area().minZ(), b.area().maxX(),
                    b.area().maxZ(), Wire.color(b.color()), flags, life(b.duration(), lifetimeMs));
        }

        @Override
        Optional<Duration> lifetime(Border b) {
            return b.duration();
        }

        @Override
        HandleImpl<Border> newHandle(Border b) {
            return new BorderH(this, b);
        }
    }

    static final class BorderH extends HandleImpl<Border> implements BorderHandle {
        BorderH(Borders s, Border v) {
            super(s, v);
        }
    }

    // ---- holograms ----------------------------------------------------------------------------

    static final class Holograms extends ElementServiceImpl<Hologram, HologramHandle> implements HologramService {
        Holograms(Session s) {
            super(s, Feature.HOLOGRAMS, "hologram", 64, PacketKind.HOLOGRAM_REMOVE, PacketKind.HOLOGRAMS_CLEAR);
        }

        @Override
        Packet setPacket(Hologram h, long lifetimeMs) {
            return new HologramSetPacket(h.id(), h.position().x(), h.position().y(), h.position().z(), Wire.world(h.world()),
                    Wire.lines(h.lines(), session.core.serializer(), "hologram"), life(h.duration(), lifetimeMs));
        }

        @Override
        Optional<Duration> lifetime(Hologram h) {
            return h.duration();
        }

        @Override
        HandleImpl<Hologram> newHandle(Hologram h) {
            return new HologramH(this, h);
        }
    }

    static final class HologramH extends HandleImpl<Hologram> implements HologramHandle {
        HologramH(Holograms s, Hologram v) {
            super(s, v);
        }
    }

    // ---- cooldowns ----------------------------------------------------------------------------

    static final class Cooldowns extends ElementServiceImpl<Cooldown, CooldownHandle> implements CooldownService {
        Cooldowns(Session s) {
            super(s, Feature.COOLDOWNS, "cooldown", 16, PacketKind.COOLDOWN_REMOVE, PacketKind.COOLDOWNS_CLEAR);
        }

        @Override
        Packet setPacket(Cooldown c, long lifetimeMs) {
            long ms = lifetimeMs >= 0 ? lifetimeMs : Math.max(1, c.duration().toMillis());
            return new CooldownSetPacket(c.id(), c.label(), c.icon().orElse(""), ms);
        }

        @Override
        Optional<Duration> lifetime(Cooldown c) {
            return Optional.of(c.duration());
        }

        @Override
        HandleImpl<Cooldown> newHandle(Cooldown c) {
            return new CooldownH(this, c);
        }
    }

    static final class CooldownH extends HandleImpl<Cooldown> implements CooldownHandle {
        CooldownH(Cooldowns s, Cooldown v) {
            super(s, v);
        }
    }

    // ---- timers -------------------------------------------------------------------------------

    static final class Timers extends ElementServiceImpl<Timer, TimerHandle> implements TimerService {
        Timers(Session s) {
            super(s, Feature.TIMERS, "timer", 8, PacketKind.TIMER_REMOVE, PacketKind.TIMERS_CLEAR);
        }

        @Override
        Packet setPacket(Timer t, long lifetimeMs) {
            return new TimerSetPacket(t.id(), t.label(),
                    t.mode() == Timer.Mode.COUNTDOWN ? TimerSetPacket.MODE_COUNTDOWN : TimerSetPacket.MODE_STOPWATCH,
                    t.start().toMillis(), t.isRunning());
        }

        @Override
        Packet resendPacket(HandleImpl<Timer> h, long remainingMs) {
            return setPacket(h.value.withStart(((TimerH) h).currentLocked()), -1);
        }

        @Override
        Optional<Duration> lifetime(Timer t) {
            return Optional.empty();
        }

        @Override
        HandleImpl<Timer> newHandle(Timer t) {
            return new TimerH(this, t);
        }
    }

    static final class TimerH extends HandleImpl<Timer> implements TimerHandle {
        TimerH(Timers s, Timer v) {
            super(s, v);
        }

        Duration currentLocked() {
            Timer t = value;
            if (!active || !t.isRunning() || sentAt == null) {
                return t.start();
            }
            Duration elapsed = Duration.between(sentAt, service.session.core.now());
            if (t.mode() == Timer.Mode.STOPWATCH) {
                return t.start().plus(elapsed);
            }
            Duration left = t.start().minus(elapsed);
            return left.isNegative() ? Duration.ZERO : left;
        }

        @Override
        public Duration current() {
            synchronized (service.session) {
                return currentLocked();
            }
        }

        @Override
        public SendResult pause() {
            Timer next;
            synchronized (service.session) {
                next = value.withStart(currentLocked()).paused();
            }
            return update(next);
        }

        @Override
        public SendResult resume() {
            Timer next;
            synchronized (service.session) {
                next = value.withStart(currentLocked()).running();
            }
            return update(next);
        }
    }

    // ---- glow ---------------------------------------------------------------------------------

    static final class Glows extends ElementServiceImpl<Glow, GlowHandle> implements GlowService {
        Glows(Session s) {
            super(s, Feature.GLOW, "glow", 128, PacketKind.GLOW_REMOVE, PacketKind.GLOW_CLEAR);
        }

        @Override
        Packet setPacket(Glow g, long lifetimeMs) {
            return new GlowSetPacket(g.entity(), Wire.color(g.color()));
        }

        @Override
        Optional<Duration> lifetime(Glow g) {
            return Optional.empty();
        }

        @Override
        HandleImpl<Glow> newHandle(Glow g) {
            return new GlowH(this, g);
        }
    }

    static final class GlowH extends HandleImpl<Glow> implements GlowHandle {
        GlowH(Glows s, Glow v) {
            super(s, v);
        }
    }

    // ---- nametags -----------------------------------------------------------------------------

    static final class Nametags extends ElementServiceImpl<Nametag, NametagHandle> implements NametagService {
        Nametags(Session s) {
            super(s, Feature.NAMETAGS, "nametag", 128, PacketKind.NAMETAG_RESET, PacketKind.NAMETAGS_CLEAR);
        }

        @Override
        Packet setPacket(Nametag n, long lifetimeMs) {
            return new NametagSetPacket(n.target(), Wire.lines(n.lines(), session.core.serializer(), "nametag"));
        }

        @Override
        Optional<Duration> lifetime(Nametag n) {
            return Optional.empty();
        }

        @Override
        HandleImpl<Nametag> newHandle(Nametag n) {
            return new NametagH(this, n);
        }
    }

    static final class NametagH extends HandleImpl<Nametag> implements NametagHandle {
        NametagH(Nametags s, Nametag v) {
            super(s, v);
        }
    }

}
