/**
 * One immutable class per message shape. Constructors validate every field against
 * PROTOCOL.md, so a packet that exists can always be encoded; equality compares the encoded
 * bytes. Removes, clears and entity-keyed messages share {@link dev.beezy.protocol.packet.RemovePacket},
 * {@link dev.beezy.protocol.packet.EmptyPacket} and {@link dev.beezy.protocol.packet.EntityPacket}.
 */
@Stable
package dev.beezy.protocol.packet;

import dev.beezy.stability.Stable;
