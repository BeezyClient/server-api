# Beezy Server API (`beezy:server`, format 1)

What a Minecraft server can do for players using Beezy Client: waypoints, beams, borders,
holograms, glowing entities, nametags, notifications, cooldowns, timers, its build height, team
mates, Discord rich presence, nick hider, food values, TNT fuse length, module settings while the
player is there, and transfers to another server. It replaces the old `beezy:policy` channel.

**The rules the whole API follows.**

- A server can **never look**: the client tells it nothing but the Hello (Beezy is here, its
  version, which features are on) and the result of a transfer. No settings, no mod list, no
  hardware.
- **Everything is temporary**: what a server sent, including module settings it overrode, is
  forgotten or put back exactly as it was when the player leaves (disconnect, or a Reset).
- **The player decides**: every feature has its own switch (Beezy menu > Server Features). A
  switched-off feature is not announced in the Hello and its messages are dropped. Settings a
  server overrides show "Set by this server" in the menu.

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

- Integers are big-endian. `i32`, `u32`, `u16`, `u8`; `f32` and `f64` are IEEE 754 single and double.
- `str` is a `u8` byte length then that many bytes of UTF-8 (so at most 255 bytes; each field has
  its own smaller cap below; caps are in bytes). Control characters and `§` formatting codes (the
  `§` and the character after it) are removed by the client from every string it draws; ids are
  compared as sent and never drawn. An id is never empty.
- `color` is a `u32` ARGB (`0xFF55FF55`). Alpha 0 means "the client's default".
- `uuid` is 16 bytes (most significant half first).
- `world` is a `str` (≤ 64): the id of the dimension the client is in, as the client knows it
  (`minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`, or a datapack dimension
  like `myserver:lobby`). It is not the server's world folder name: the plugin turns a Bukkit
  `World` into this id. Empty means "any world" (or "the one the player is in", where noted).
- `duration` is a `u32` of milliseconds after which the client removes the thing by itself; 0 means
  "until removed". At most 24 h. Counted from when the message arrives.
- `pos` is three `f64` (x, y, z), finite.
- `component` is a text component: a `u16` byte length (1-4096) then that many bytes of UTF-8
  JSON, in Minecraft's JSON text format (what Adventure's `GsonComponentSerializer` writes:
  `text`, `extra`, `color` as a name or `#rrggbb`, `bold`, `italic`, `underlined`,
  `strikethrough`, `obfuscated`, `font`, `translate`/`with`, `keybind`, `score` and `selector`
  are shown as plain text). As in vanilla, a bare JSON string is a plain text component and an
  array is its first element with the rest appended as `extra`: Adventure writes plain text as
  `"Steve"` and plain children as strings (`"extra":["50%"]`). The client draws what its Minecraft version can: hex colours become
  the nearest named colour before 1.16, an unknown font is the default one, click and hover
  events are ignored (nothing in this API is clickable). Invalid JSON or nesting deeper than 16
  levels drops the whole message, like any malformed field.
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
presence, 7 beams, 8 borders, 9 glow, 10 holograms, 11 module settings, 12 nametags, 13 nick hider,
14 food values (saturation), 15 TNT countdown, 16 transfer. A server should not send what the player turned off (the client drops it anyway).
"Available" means the part of Beezy that shows it is on too: waypoints need the player's Waypoints
module, height limit the Height Limit module, rich presence the Discord module. The bits are those
of the moment the Hello is sent; the client re-checks them on every message it receives.

The client never sends on a channel the server has not registered (strict servers and anticheats
disconnect for that), so the Hello goes out once the server has sent its own `minecraft:register`
(`REGISTER`) naming `beezy:server`. On Paper/Spigot that happens by itself when a plugin registers
`beezy:server` as an incoming plugin channel; a server written by hand must send it.

| 3 | Transfer result | `str` id (the request's, ≤ 32), `u8` status: 0 connecting (sent just before the client leaves), 1 refused (the player switched transfers off), 2 invalid address or too soon. If the new server cannot be reached the player ends up on the multiplayer screen; the old server is not told. |

The Hello and the transfer result are the only things the client ever sends.

## Server -> client

| kind | name | body | limits |
|---|---|---|---|
| 2 | Reset | (empty) | Clears everything this server sent. |
| 10 | Waypoint set | `str` id (≤ 32), `str` name (≤ 32), `i32` x, `i32` y, `i32` z, `world` world (empty = any), `color` color, `u8` flags (bit 0: show a beam), `duration` | 64 server waypoints; the same id replaces. |
| 11 | Waypoint remove | `str` id | |
| 12 | Waypoints clear | (empty) | |
| 20 | Notification | `str` title (≤ 32), `str` text (≤ 160), `u8` level (0 info, 1 success, 2 warning, 3 error), `u16` seconds (1-30) | At most 3 on screen, at most 5 in 10 s (more are dropped, not queued). `§` codes are removed. |
| 30 | Cooldown set | `str` id (≤ 32), `str` label (≤ 32), `str` item icon (≤ 64, e.g. `minecraft:ender_pearl`; empty = none), `u32` duration ms (≤ 1 h) | 16 at once; counts down from arrival, removed at 0. |
| 31 | Cooldown remove | `str` id | |
| 32 | Cooldowns clear | (empty) | |
| 40 | Timer set | `str` id (≤ 32), `str` label (≤ 32), `u8` mode (0 countdown, 1 stopwatch), `u32` start ms (countdown: time left; stopwatch: time already elapsed), `u8` running (0 paused, 1 running) | 8 at once. |
| 41 | Timer remove | `str` id | |
| 42 | Timers clear | (empty) | |
| 50 | Height limit | `world` (empty = the one the player is in when it arrives), `i32` min y, `i32` max y | For the Height Limit HUD; one per dimension. |
| 51 | Height limit clear | (empty) | |
| 60 | Team | `u8` count (≤ 32), then per member: `uuid`, `str` name (≤ 16), `color` color, `u8` has position, then if 1: `pos`, `world` | Replaces the whole team. Members are shown on the minimap and the direction HUD; a position lets a mate far away still show (the server sends it every second or two). |
| 61 | Team clear | (empty) | |
| 70 | Rich presence | `str` game (≤ 32, e.g. "Bed Wars"), `str` mode (≤ 32, "Solo"), `str` map (≤ 32), `str` state (≤ 64, "In game"), `u8` team size, `u8` team max (0 = none) | Shown in the player's Discord status, only if they enabled Discord Rich Presence. |
| 71 | Rich presence clear | (empty) | |
| 80 | Beam set | `str` id (≤ 32), `i32` x, `i32` y, `i32` z, `world`, `color`, `duration` | A beacon-like beam from that block up to the sky, seen from any distance in render range. 32 at once. |
| 81 | Beam remove | `str` id | |
| 82 | Beams clear | (empty) | |
| 90 | Border set | `str` id (≤ 32), `world`, `f64` min x, `f64` min z, `f64` max x, `f64` max z, `color`, `u8` flags (bit 0: solid, the client stops the player walking through it; bit 1: only show when near, within 16 blocks), `duration` | A wall from the bottom to the top of the world around that rectangle. 16 at once. Solid is client-side only: the server must still enforce it. min ≤ max. |
| 91 | Border remove | `str` id | |
| 92 | Borders clear | (empty) | |
| 100 | Glow set | `uuid` entity, `color` | That entity is drawn with an outline of that colour for this player only. 128 at once. |
| 101 | Glow remove | `uuid` entity | |
| 102 | Glow clear | (empty) | |
| 110 | Hologram set | `str` id (≤ 32), `pos`, `world`, `u8` count (1-8), then that many `component` lines, `duration` | Floating text lines, top first, facing the player. 64 at once. |
| 111 | Hologram remove | `str` id | |
| 112 | Holograms clear | (empty) | |
| 120 | Module settings | `u8` count (1-64), then per entry: `str` module (≤ 32, a Beezy module id such as `zoom`), `str` option (≤ 32, a setting id of that module; empty = the module's on/off switch), `u8` type, value: type 0 bool (`u8` 0/1), 1 int (`i32`), 2 decimal (`f64`), 3 text (`str`, ≤ 64), 4 colour (`color`) | Overrides those settings while the player stays on this server; the player cannot change them meanwhile and the menu says "Set by this server". Unknown modules or options, a wrong type, or a value outside the setting's own range are skipped (one at a time, not the whole message). Sending the same module and option again replaces it. The Server Features module (`server_features`) cannot be set: those are the player's own switches over this API. |
| 121 | Module settings release | `u8` count (0-64), then per entry: `str` module, `str` option (empty = the switch) | Gives those settings back to the player (their own value again). Count 0 releases all. |
| 130 | Nametag set | `uuid` player, `u8` count (0-5), then that many `component` lines | Replaces the nametag above that player, top line first, for this player only. Count 0 hides it. 128 at once. |
| 131 | Nametag reset | `uuid` player | Back to the vanilla nametag. |
| 132 | Nametags clear | (empty) | |
| 140 | Nick hider | `str` nick (≤ 16) | The name Beezy's Nick Hider shows for the player themselves, in place of the one it would choose (for servers with `/nick`). Only applies when the player has Nick Hider on. |
| 141 | Nick hider clear | (empty) | |
| 150 | Food values | `u8` count (1-64), then per entry: `str` item (≤ 64, e.g. `minecraft:golden_carrot`), `u8` hunger (0-20), `f32` saturation (0-20, finite) | What Beezy's Saturation display shows for those items (custom foods). The same item replaces. 256 at once. |
| 151 | Food values clear | (empty) | |
| 160 | TNT countdown | `u16` ticks (1-1200) | The fuse length the TNT Timer module counts from on this server (vanilla: 80). |
| 161 | TNT countdown clear | (empty) | |
| 170 | Transfer | `str` id (≤ 32), `str` address (≤ 255, `host` or `host:port`) | The client leaves this server and joins that address, as the vanilla 1.20.5+ transfer does, on every version. It answers with a Transfer result. At most one a minute; more are refused as invalid. |

Notes:

- A player never sees another player's data through this API: a server only sends to each player
  what it decides for them.
- Rich presence text goes to Discord only through the player's own Discord app (local IPC), never
  through Beezy's servers.
- Coordinates are block coordinates for waypoints and beams, exact for team positions, holograms
  and borders.
- Things with a `duration` disappear by themselves on the client: the server does not need a task
  to remove them.
- The client may round, clip or not draw anything that would not fit the screen.

## Example (bytes)

Notification "Welcome", "Have fun!", info, 5 s:

```
01 14 07 'W' 'e' 'l' 'c' 'o' 'm' 'e' 09 'H' 'a' 'v' 'e' ' ' 'f' 'u' 'n' '!' 00 00 05
```

## Versions

Format 1, Beezy mod 0.2 and Horizon builds from November 2026. A future format 2 would be a new
first byte that format-1 clients drop.
