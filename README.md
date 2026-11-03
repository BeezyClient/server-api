# Beezy server plugin

Lets a Minecraft server work with players who use **Beezy Client**: waypoints, beams, borders,
holograms, glowing entities, custom nametags, notifications, cooldown bars, timers, its build
height, team mates on the minimap, a Discord rich presence, the Nick Hider name, food values for
custom foods, the TNT fuse length, module settings while the player is on the server, and
transfers to another server. Players without Beezy see nothing and lose nothing.

A server can never *look*: it cannot read a player's settings or see their mods. Everything it
sends is temporary: module settings it overrides are given back exactly as they were when the
player leaves. Every feature can be turned off by the player.

**Privacy:** the plugin only learns that a player uses Beezy, its version, its Minecraft version,
which of the features above they have switched on, and how a transfer this server asked for went
(connecting, refused, invalid address, failed). It has no telemetry, no update checker, and never
connects to anything.

## Install

Works on Bukkit, Spigot, Paper and Folia, **1.8.8 to the latest version**, Java 8 or newer.

1. Put `Beezy-Bukkit-<version>.jar` in `plugins/`.
2. Restart the server. `plugins/Beezy/config.yml` is created.

**Networks (Velocity, BungeeCord):** install the plugin on **each backend server** that should use
Beezy features. Nothing goes on the proxy: the client says Hello again to every backend it is sent
to, and each backend clears what the previous one showed when the player arrives.

After a `/reload`, players already online are asked to say Hello again; if a client misses it,
it says Hello the next time the player joins.

## How it works

1. When a player joins, the server announces the `beezy:server` channel (the standard
   `minecraft:register` / `REGISTER` message) and the plugin sends a **Reset**, which clears
   anything another server of the same network left on screen.
2. A Beezy client answers with a **Hello**: Minecraft version, Beezy version, enabled features.
3. From then on, the player counts as a Beezy player and the API and commands can send to them.
   Messages for a feature the player switched off are not sent.

## Configuration (`config.yml`)

| key | default | what it does |
|---|---|---|
| `debug` | `false` | Logs every Hello, transfer result and malformed message. |
| `rich-presence.game` | `""` | If set, sent as the Discord rich presence of Beezy players on join (e.g. `My Server`). Empty sends nothing. |
| `rich-presence.mode` / `rich-presence.state` | `""` | Optional, at most 32 / 64 bytes. |
| `team-sync.enabled` | `false` | Sends each Beezy player the online members of their scoreboard team, coloured like the team. Not available on Folia. |
| `team-sync.interval-ticks` | `40` | How often (20 ticks = 1 s, minimum 10). |
| `team-sync.positions` | `true` | Includes positions, so far-away mates still show on the minimap. |

Invalid values (a presence field too long) turn that feature off with a warning.

## Commands and permissions

Arguments with spaces can be quoted: `"Bed Wars"`. `*` means every Beezy player online.

| command | permission | |
|---|---|---|
| `/beezy` | `beezy.command.help` | Help. |
| `/beezy check <player>` | `beezy.command.check` | Whether the player uses Beezy, its version and features. |
| `/beezy notify <player\|*> <title> <text...>` | `beezy.command.notify` | Shows a notification (info, 5 s). |
| `/beezy waypoint <player\|*> add <id> <x> <y> <z> [name...]` | `beezy.command.waypoint` | Adds or replaces a waypoint. `~` uses your own coordinates; a waypoint added by a player is tied to their current world, one added from the console shows in any world. |
| `/beezy waypoint <player\|*> remove <id>` / `clear` | `beezy.command.waypoint` | |
| `/beezy beam <player\|*> add <id> <x> <y> <z> [#rrggbb\|color]` | `beezy.command.beam` | Adds or replaces a beam (colour: `#ffaa00` or a chat colour name like `red`). Same `~` and world rules as waypoints. |
| `/beezy beam <player\|*> remove <id>` / `clear` | `beezy.command.beam` | |
| `/beezy transfer <player\|*> <host[:port]>` | `beezy.command.transfer` | Sends the player to another server. You are told how it went (connecting, refused, invalid address, failed). |
| `/beezy reset <player\|*>` | `beezy.command.reset` | Clears everything the server sent to the player. |

All default to operators. `beezy.admin` grants them all.

## Java API

The plugin jar contains the API (`dev.beezy.server.api`); compile against it as `compileOnly`
(the `api` module of this build, `beezy-server-api-<version>.jar`, plus the plugin jar for the
Bukkit helpers) and add `depend: [Beezy]` or `softdepend: [Beezy]` to your `plugin.yml`.

```java
import dev.beezy.server.api.*;
import dev.beezy.server.bukkit.*;
import java.awt.Color;
import java.time.Duration;

BeezyServerApi beezy = Bukkit.getServicesManager().load(BeezyServerApi.class); // or BeezyBukkit.api()

BeezyPlayer p = beezy.player(player.getUniqueId());   // or BeezyBukkit.player(player); null if not Beezy
if (p != null) {
    Hello hello = p.hello();                           // versions and features
    p.sendNotification(Notification.builder("Bed Wars", "Your bed was destroyed!")
            .level(Notification.Level.WARNING).seconds(8).build());
}
```

Wait for the Hello before sending: listen to `BeezyHelloEvent` (fired on the player's thread;
`isFirst()` is false when the same client says Hello again during the visit).

```java
@EventHandler
public void onBeezy(BeezyHelloEvent event) {
    BeezyPlayer p = event.getBeezyPlayer();
    p.sendWaypoint(BeezyBukkit.waypoint("spawn", spawn).name("Spawn").color(Color.GREEN).beam(true).build());
}
```

Conventions:

- **Colours** are `java.awt.Color`, sent opaque. `null` means the client's default colour.
- **Worlds** are the keys the client knows (`minecraft:overworld`, `minecraft:the_nether`, a
  datapack world like `myserver:lobby`); empty or `null` means any world. On Bukkit,
  `BeezyBukkit.world(World)` gives the key, and `BeezyBukkit.waypoint/beam/hologram(id, Location)`
  and `BeezyBukkit.border(id, World)` fill position and world in for you.
- **Durations** (`.duration(Duration)`, at most 24 h) make the client remove waypoints, beams,
  borders and holograms by itself; without one they stay until removed.
- Every send returns `true` if it was sent, `false` if the player is not a Beezy player or
  switched that feature off (`p.has(Feature.BEAMS)` tells you in advance).
- Values are checked against the protocol limits (string caps in UTF-8 bytes, ranges, list
  sizes): bad input throws `IllegalArgumentException`, nothing is truncated.
- The API is thread-safe. Each kind has `send`/`remove`/`clear` (or `reset`) methods on both
  `BeezyServerApi` (taking the player's UUID) and `BeezyPlayer`; `p.reset()` clears everything.

### Waypoints

```java
p.sendWaypoint(Waypoint.builder("flag").name("Enemy flag").at(120, 70, -40).world("minecraft:overworld")
        .color(Color.RED).beam(true).duration(Duration.ofMinutes(2)).build());
p.removeWaypoint("flag");
```

At most 64 per player; the same id replaces.

### Notifications

```java
p.sendNotification(Notification.of("Welcome", "Have fun!"));    // info, 5 s
```

At most 3 on screen and 5 every 10 seconds on the client.

### Cooldowns and timers

```java
p.sendCooldown(new Cooldown("pearl", "Ender Pearl", "minecraft:ender_pearl", 15_000));
p.sendTimer(Timer.countdown("round", "Round ends", 5 * 60_000));
p.sendTimer(Timer.countdown("round", "Round ends", 0).pausedAt(42_000));   // pause
```

16 cooldowns, 8 timers.

### Height limit

```java
p.sendHeightLimit("minecraft:overworld", -64, 320);
BeezyBukkit.sendHeightLimit(p, player.getWorld(), 0, 128);
```

### Team

```java
p.sendTeam(Arrays.asList(BeezyBukkit.teamMember(mate, Color.BLUE)));   // with position
```

Or let the plugin do it from scoreboard teams (`team-sync` in config.yml).

### Rich presence

```java
p.sendRichPresence(RichPresence.builder("Bed Wars").mode("Solo").map("Lighthouse")
        .state("In game").team(1, 4).build());
```

### Beams

```java
p.sendBeam(BeezyBukkit.beam("drop", chest.getLocation()).color(Color.YELLOW)
        .duration(Duration.ofSeconds(30)).build());
```

A beacon-like beam up to the sky, seen from any distance in render range. 32 at once.

### Borders

```java
p.sendBorder(Border.builder("arena").world("minecraft:overworld").area(-50, -50, 50, 50)
        .color(Color.RED).solid(true).onlyWhenNear(true).build());
```

A wall around a rectangle, from the bottom to the top of the world. `solid` stops the player on
the client only: the server must still enforce it. 16 at once.

### Glow

```java
BeezyBukkit.sendGlow(p, boss, Color.ORANGE);      // any Entity, for this player only
p.sendGlow(entityUuid, null);                      // client's default colour
p.removeGlow(entityUuid);
```

128 at once.

### Holograms

```java
p.sendHologram(BeezyBukkit.hologram("shop", npc.getLocation().add(0, 2.2, 0))
        .lines("§6Shop", "Right-click to trade").duration(Duration.ofMinutes(10)).build());
```

1 to 8 lines (`§` codes kept), facing the player. 64 at once.

### Module settings

```java
p.settings().set("zoom", "max_zoom", 4).set("freelook", false).send();   // override while here
p.settings().release("zoom", "max_zoom");                                  // give one back
p.settings().releaseAll();                                                 // give all back
```

Values are booleans, ints, doubles, text (≤ 64 bytes) or `Color`s; `set(module, boolean)` is the
module's on/off switch. The player cannot change an overridden setting while on the server (the
menu says "Set by this server") and gets their own value back when they leave. Unknown modules or
options and out-of-range values are skipped by the client one at a time. For many players, build
once: `ModuleSettings.builder()...build()` and `beezy.overrideSettings(uuid, settings)`.

### Nametags

```java
BeezyBukkit.sendNametag(p, target, "§c[RED]", target.getName());   // 0-5 lines; none hides it
BeezyBukkit.resetNametag(p, target);                                // back to vanilla
```

For that viewer only. 128 at once.

### Nick hider

```java
p.sendNick("Steve");   // the name Beezy's Nick Hider shows for the player, for servers with /nick
p.clearNick();
```

### Food values (Saturation display)

```java
BeezyBukkit.sendFoodValues(p, BeezyBukkit.food(Material.GOLDEN_CARROT, 6, 14.4));
p.sendFoodValues(new FoodValue("myserver:energy_bar", 4, 9.6));
```

For custom foods: what Beezy's Saturation display shows. The same item replaces.

### TNT countdown

```java
p.sendTntCountdown(Duration.ofSeconds(2));   // or p.sendTntCountdown(40) in ticks; vanilla is 80
```

### Transfer

```java
String id = p.transfer("lobby.example.net:25565");   // null if not sent

@EventHandler
public void onResult(BeezyTransferResultEvent event) {
    if (event.getId().equals(id) && event.getStatus() == TransferResult.Status.REFUSED) {
        // the player switched transfers off: fall back to your proxy's own command
    }
}
```

Works on every Minecraft version, like the vanilla 1.20.5+ transfer. The client answers
`CONNECTING`, `REFUSED` (transfers off), `INVALID_ADDRESS` (or more than one a minute) or
`FAILED`; only answers to transfers this server asked for fire the event.

### Other platforms

The `api` module has no dependencies and targets Java 8, so other platforms can reuse it:
`BeezyCodec` encodes and decodes the bytes, `BeezyApiCore` keeps track of Hellos and transfers
and sends through a one-method `Transport`.

## Without the plugin

The protocol is small and documented in [`PROTOCOL.md`](PROTOCOL.md): plugin
channel `beezy:server`, a format byte, a kind byte, big-endian fields. Any server software can
speak it directly.

## Build

```
./gradlew build
```

- `api/build/libs/beezy-server-api-<version>.jar`: the API alone (with unit tests).
- `bukkit/build/libs/Beezy-Bukkit-<version>.jar`: the plugin, API included.

The plugin compiles against spigot-api 1.8.8 so that nothing newer is used by accident; newer
methods (world keys, team colours) are looked up at runtime.

## License

MIT: see [`LICENSE`](LICENSE). "Beezy" and "Beezy Client" are names of the Beezy Client project; the licence covers the code, not the names or logos.
