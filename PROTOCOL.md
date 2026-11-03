# Beezy Server API (`beezy:server`, format 1)

What a Minecraft server can show to players using Beezy Client: waypoints, notifications,
cooldowns, timers, its build height, team mates and Discord rich presence. It replaces the old
`beezy:policy` channel, which let a server switch Beezy modules off and no longer exists.

**The rule the whole API follows: a server can show things, never take them away or look.** It
cannot switch a module off, change a player's settings, read their settings or their mods, or
make the client do anything but draw. Every feature can be turned off by the player (Beezy menu >
Server Features), and everything a server sent is forgotten when the player leaves it.

Servers use it through the Beezy plugin (this repository: Paper/Spigot/Folia, installed on each
game server; networks need nothing on the proxy) or by writing the bytes below themselves.

## Channel

- Name: `beezy:server`, the same on every Minecraft version (1.8.9 to 26.x, and Beezy's Horizon
  client). Under 20 characters, the limit of 1.8's plugin message.
- On joining a server, the client sends `minecraft:register` (or `REGISTER` on 1.8) with
  `beezy:server`, then a **Hello**. A server that never answers costs nothing.
- **Networks behind a proxy (Velocity, BungeeCord)**: the player moves between backend servers
  without disconnecting, and only the first backend saw that Hello. So the client sends the Hello
  again **every time it receives a `minecraft:register` (`REGISTER`) that lists `beezy:server`**
  (at most once a second): each backend running the plugin registers the channel when the player
  arrives, and gets its own Hello. No proxy plugin is needed.
- All state from the previous server (waypoints, cooldowns, timers, team, presence, height limit)
  is cleared when the player disconnects, and when the client receives a **Reset**. The plugin
  sends a Reset to every player who joins its server, so nothing from another backend of the same
  network stays on screen.

## Encoding

Every message, both directions:

```
u8   format   1
u8   kind     see the tables
...           the kind's body
```

- Integers are big-endian. `i32`, `u32`, `u16`, `u8`; `f64` is an IEEE 754 double.
- `str` is a `u8` byte length then that many bytes of UTF-8 (so at most 255 bytes; each field has
  its own smaller cap below; caps are in bytes). Control characters and `§` formatting codes (the
  `§` and the character after it) are removed by the client from every string it draws; ids are
  compared as sent and never drawn. An id is never empty.
- `color` is a `u32` ARGB (`0xFF55FF55`). Alpha 0 means "the client's default".
- `uuid` is 16 bytes (most significant half first).
- A whole message is at most **32 768 bytes**. Longer, malformed (cut short, invalid UTF-8),
  unknown `format`, a field over its cap or outside its range (a level, mode or flag with a value
  the table does not list, seconds outside 1-30, min y above max y, a position that is not a finite
  number): the message is dropped whole (nothing half-applies) and the client logs one line.
- Bytes after the end of a known kind's body are ignored, so a later format-1 revision can append a
  field to a kind without breaking older clients. Do not rely on it to carry anything today.
- **Unknown kinds are ignored**, so a newer server works with an older client. A client says in
  its Hello which kinds it understands.

## Client -> server

| kind | name | body |
|---|---|---|
| 1 | Hello | `str` Minecraft version (≤ 32), `str` Beezy version (≤ 32), `u32` features: bit set per feature below that is **available and switched on** by the player |

Feature bits: 0 waypoints, 1 notifications, 2 cooldowns, 3 timers, 4 height limit, 5 team, 6 rich
presence. A server should not send what the player turned off (the client drops it anyway).
"Available" means the part of Beezy that shows it is on too: waypoints need the player's Waypoints
module, height limit the Height Limit module, rich presence the Discord module. The bits are those
of the moment the Hello is sent; the client re-checks them on every message it receives.

The client never sends on a channel the server has not registered (strict servers and anticheats
disconnect for that), so the Hello goes out once the server has sent its own `minecraft:register`
(`REGISTER`) naming `beezy:server`. On Paper/Spigot that happens by itself when a plugin registers
`beezy:server` as an incoming plugin channel; a server written by hand must send it.

The Hello is the only thing the client ever sends. It tells the server that the player uses Beezy
and its version; nothing else (no settings, no mods, no hardware).

## Server -> client

| kind | name | body | limits |
|---|---|---|---|
| 2 | Reset | (empty) | Clears everything this server sent. |
| 10 | Waypoint set | `str` id (≤ 32), `str` name (≤ 32), `i32` x, `i32` y, `i32` z, `str` dimension (≤ 64, e.g. `minecraft:overworld`; empty = any), `color` color, `u8` flags (bit 0: show a beam) | 64 server waypoints; the same id replaces. |
| 11 | Waypoint remove | `str` id | |
| 12 | Waypoints clear | (empty) | |
| 20 | Notification | `str` title (≤ 32), `str` text (≤ 160), `u8` level (0 info, 1 success, 2 warning, 3 error), `u16` seconds (1-30) | At most 3 on screen, at most 5 in 10 s (more are dropped, not queued). `§` codes are removed. |
| 30 | Cooldown set | `str` id (≤ 32), `str` label (≤ 32), `str` item icon (≤ 64, e.g. `minecraft:ender_pearl`; empty = none), `u32` duration ms (≤ 1 h) | 16 at once; counts down from arrival, removed at 0. |
| 31 | Cooldown remove | `str` id | |
| 32 | Cooldowns clear | (empty) | |
| 40 | Timer set | `str` id (≤ 32), `str` label (≤ 32), `u8` mode (0 countdown, 1 stopwatch), `u32` start ms (countdown: time left; stopwatch: time already elapsed), `u8` running (0 paused, 1 running) | 8 at once. |
| 41 | Timer remove | `str` id | |
| 42 | Timers clear | (empty) | |
| 50 | Height limit | `str` dimension (≤ 64; empty = the one the player is in when it arrives), `i32` min y, `i32` max y | For the Height Limit HUD; one per dimension. |
| 51 | Height limit clear | (empty) | |
| 60 | Team | `u8` count (≤ 32), then per member: `uuid`, `str` name (≤ 16), `color` color, `u8` has position, then if 1: `f64` x, `f64` y, `f64` z, `str` dimension (≤ 64) | Replaces the whole team. Members are shown on the minimap and the direction HUD; a position lets a mate far away still show (the server sends it every second or two). |
| 61 | Team clear | (empty) | |
| 70 | Rich presence | `str` game (≤ 32, e.g. "Bed Wars"), `str` mode (≤ 32, "Solo"), `str` map (≤ 32), `str` state (≤ 64, "In game"), `u8` team size, `u8` team max (0 = none) | Shown in the player's Discord status, only if they enabled Discord Rich Presence. |
| 71 | Rich presence clear | (empty) | |

Notes:

- A player never sees another player's data through this API: a server only sends to each player
  what it decides for them.
- Rich presence text goes to Discord only through the player's own Discord app (local IPC), never
  through Beezy's servers.
- Coordinates are block coordinates for waypoints, exact for team positions.
- The client may round, clip or not draw anything that would not fit the screen.

## Example (bytes)

Notification "Welcome", "Have fun!", info, 5 s:

```
01 14 07 'W' 'e' 'l' 'c' 'o' 'm' 'e' 09 'H' 'a' 'v' 'e' ' ' 'f' 'u' 'n' '!' 00 00 05
```

## Versions

Format 1, Beezy mod 0.2 and Horizon builds from November 2026. A future format 2 would be a new
first byte that format-1 clients drop.
