# Beezy server plugin

Lets a Minecraft server show things to players who use **Beezy Client**: waypoints,
notifications, cooldown bars, timers, its build height, team mates on the minimap and a Discord
rich presence. Players without Beezy see nothing and lose nothing.

A server can only *show* things. It cannot switch a Beezy module off, change or read a player's
settings, or see their mods. Every feature can be turned off by the player, and everything the
server sent is forgotten when they leave.

**Privacy:** the plugin only learns that a player uses Beezy, its version, its Minecraft version
and which of the features above they have switched on. It has no telemetry, no update checker,
and never connects to anything.

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
| `debug` | `false` | Logs every Hello and every malformed message. |
| `welcome.enabled` | `true` | Sends a notification to Beezy players when they join (once per visit). |
| `welcome.title` / `welcome.text` | `Welcome` / `This server supports Beezy features.` | At most 32 / 160 bytes. |
| `welcome.seconds` | `5` | 1 to 30. |
| `rich-presence.game` | `""` | If set, sent as the Discord rich presence of Beezy players on join (e.g. `My Server`). Empty sends nothing. |
| `rich-presence.mode` / `rich-presence.state` | `""` | Optional, at most 32 / 64 bytes. |
| `team-sync.enabled` | `false` | Sends each Beezy player the online members of their scoreboard team, coloured like the team. Not available on Folia. |
| `team-sync.interval-ticks` | `40` | How often (20 ticks = 1 s, minimum 10). |
| `team-sync.positions` | `true` | Includes positions, so far-away mates still show on the minimap. |

Invalid values (a title too long, seconds out of range) turn that feature off with a warning.

## Commands and permissions

Arguments with spaces can be quoted: `"Bed Wars"`. `*` means every Beezy player online.

| command | permission | |
|---|---|---|
| `/beezy` | `beezy.command.help` | Help. |
| `/beezy check <player>` | `beezy.command.check` | Whether the player uses Beezy, its version and features. |
| `/beezy notify <player\|*> <title> <text...>` | `beezy.command.notify` | Shows a notification (info, 5 s). |
| `/beezy waypoint <player\|*> add <id> <x> <y> <z> [name...]` | `beezy.command.waypoint` | Adds or replaces a waypoint. `~` uses your own coordinates; a waypoint added by a player is tied to their current world, one added from the console shows in any dimension. |
| `/beezy waypoint <player\|*> remove <id>` / `clear` | `beezy.command.waypoint` | |
| `/beezy reset <player\|*>` | `beezy.command.reset` | Clears everything the server sent to the player. |

All default to operators. `beezy.admin` grants them all.

## Java API

The plugin jar contains the API (`dev.beezy.server.api`); compile against it as `compileOnly`
(the `api` module of this build, `beezy-server-api-<version>.jar`) and add `depend: [Beezy]` or
`softdepend: [Beezy]` to your `plugin.yml`.

```java
import dev.beezy.server.api.*;

BeezyServerApi beezy = Bukkit.getServicesManager().load(BeezyServerApi.class);

UUID id = player.getUniqueId();
if (beezy.isBeezy(id)) {
    Hello hello = beezy.helloOf(id);          // versions and features
    beezy.sendNotification(id, Notification.builder("Bed Wars", "Your bed was destroyed!")
            .level(Notification.Level.WARNING).seconds(8).build());
}
```

Wait for the Hello before sending: listen to `BeezyHelloEvent` (fired on the player's thread;
`isFirst()` is false when the same client says Hello again during the visit).

```java
@EventHandler
public void onBeezy(BeezyHelloEvent event) {
    BeezyPlayer p = event.getBeezyPlayer();
    p.sendWaypoint(Waypoint.builder("spawn").name("Spawn").at(0, 64, 0)
            .dimension("minecraft:overworld").color(0xFF55FF55).beam(true).build());
    p.sendHeightLimit("minecraft:overworld", -64, 320);
    p.sendRichPresence(RichPresence.builder("Bed Wars").mode("Solo").map("Lighthouse")
            .state("In game").team(1, 4).build());
}
```

More:

```java
beezy.sendCooldown(id, new Cooldown("pearl", "Ender Pearl", "minecraft:ender_pearl", 15_000));
beezy.sendTimer(id, Timer.countdown("round", "Round ends", 5 * 60_000));
beezy.sendTimer(id, Timer.countdown("round", "Round ends", 0).pausedAt(42_000));   // pause
beezy.sendTeam(id, Arrays.asList(
        new TeamMember(mate.getUniqueId(), mate.getName(), 0xFF5555FF)
                .withPosition(loc.getX(), loc.getY(), loc.getZ(), "minecraft:overworld")));
beezy.removeWaypoint(id, "spawn");
beezy.reset(id);                                                                      // clear all
```

- Every send returns `true` if it was sent, `false` if the player is not a Beezy player or
  switched that feature off.
- Values are checked against the protocol limits (string caps in UTF-8 bytes, ranges, 32 team
  members): bad input throws `IllegalArgumentException`, nothing is truncated.
- The API is thread-safe.
- Client-side limits apply: 64 waypoints, 16 cooldowns, 8 timers, at most 3 notifications on
  screen and 5 per 10 seconds.

The `api` module has no dependencies and targets Java 8, so other platforms can reuse it:
`BeezyCodec` encodes and decodes the bytes, `BeezyApiCore` keeps track of Hellos and sends
through a one-method `Transport`.

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
