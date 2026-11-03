package dev.beezy.protocol;

import dev.beezy.stability.Stable;

/** Who sends a kind of packet. */
@Stable
public enum Direction {
    /** Client to server: Hello and Transfer result. */
    SERVERBOUND,
    /** Server to client: everything else. */
    CLIENTBOUND
}
