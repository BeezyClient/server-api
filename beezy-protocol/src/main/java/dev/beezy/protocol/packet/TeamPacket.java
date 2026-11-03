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
import java.util.UUID;

/** Kind 60: replaces the whole team (0-32 members). */
@Stable
public final class TeamPacket extends AbstractPacket {

    /** One team member; position fields are present only when {@link #hasPosition()}. */
    @Stable
    public static final class Member {
        private final UUID uuid;
        private final String name;
        private final int argb;
        private final boolean hasPosition;
        private final double x;
        private final double y;
        private final double z;
        private final String world;

        /**
         * A member without a position.
         *
         * @param uuid the player
         * @param name at most 16 bytes
         * @param argb colour
         */
        public Member(UUID uuid, String name, int argb) {
            this(uuid, name, argb, false, 0, 0, 0, "");
        }

        /**
         * A member with a position.
         *
         * @param uuid the player
         * @param name at most 16 bytes
         * @param argb colour
         * @param x finite
         * @param y finite
         * @param z finite
         * @param world dimension id
         */
        public Member(UUID uuid, String name, int argb, double x, double y, double z, String world) {
            this(uuid, name, argb, true, x, y, z, world);
        }

        private Member(UUID uuid, String name, int argb, boolean hasPosition, double x, double y, double z, String world) {
            this.uuid = Validate.notNull(uuid, "team member uuid");
            this.name = Validate.str(name, Protocol.MAX_TEAM_MEMBER_NAME, "team member name");
            this.argb = argb;
            this.hasPosition = hasPosition;
            this.x = Validate.finite(x, "team member x");
            this.y = Validate.finite(y, "team member y");
            this.z = Validate.finite(z, "team member z");
            this.world = Validate.str(world, Protocol.MAX_WORLD, "team member world");
        }

        /** @return the player */
        public UUID uuid() {
            return uuid;
        }

        /** @return the name */
        public String name() {
            return name;
        }

        /** @return the colour */
        public int argb() {
            return argb;
        }

        /** @return whether a position follows */
        public boolean hasPosition() {
            return hasPosition;
        }

        /** @return x */
        public double x() {
            return x;
        }

        /** @return y */
        public double y() {
            return y;
        }

        /** @return z */
        public double z() {
            return z;
        }

        /** @return the world */
        public String world() {
            return world;
        }
    }

    private final List<Member> members;

    /** @param members 0-32 members */
    public TeamPacket(List<Member> members) {
        super(PacketKind.TEAM);
        Validate.notNull(members, "team members");
        Validate.range(members.size(), 0, Protocol.MAX_TEAM_MEMBERS, "team member count");
        for (Member m : members) {
            Validate.notNull(m, "team member");
        }
        this.members = Collections.unmodifiableList(new ArrayList<Member>(members));
    }

    /** @return the members */
    public List<Member> members() {
        return members;
    }

    @Override
    public void write(WireWriter out) {
        out.u8(members.size());
        for (Member m : members) {
            out.uuid(m.uuid).str(m.name).i32(m.argb).u8(m.hasPosition ? 1 : 0);
            if (m.hasPosition) {
                out.f64(m.x).f64(m.y).f64(m.z).str(m.world);
            }
        }
    }

    /**
     * @param kind the kind
     * @param in the body
     * @return the packet
     */
    public static TeamPacket read(PacketKind kind, WireReader in) {
        int count = Validate.range(in.u8(), 0, Protocol.MAX_TEAM_MEMBERS, "team member count");
        List<Member> members = new ArrayList<Member>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = in.uuid();
            String name = in.str();
            int argb = in.i32();
            int has = Validate.range(in.u8(), 0, 1, "team member has position");
            members.add(has == 1
                    ? new Member(uuid, name, argb, in.f64(), in.f64(), in.f64(), in.str())
                    : new Member(uuid, name, argb));
        }
        return new TeamPacket(members);
    }
}
