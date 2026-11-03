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
(connecting, refused, invalid address). It has no telemetry, no update checker, and never
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
3. From then on, the player is a Beezy user and the API and commands can send to them. Messages
   for a feature the player switched off are not sent.

## Configuration (`config.yml`)

| key | default | what it does |
|---|---|---|
| `debug` | `false` | Logs every Hello, transfer result, malformed message and skipped send. |
| `delivery-policy` | `skip` | What API calls do when something cannot be sent (feature off, a client limit, player gone): `skip` drops it and reports it in the result, `throw` throws a `BeezyException`. |
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
| `/beezy transfer <player\|*> <host[:port]>` | `beezy.command.transfer` | Sends the player to another server. You are told how it went (connecting, refused, invalid address, no answer). |
| `/beezy reset <player\|*>` | `beezy.command.reset` | Clears everything the server sent to the player. |

All default to operators. `beezy.admin` grants them all. The commands use the public API, like
any other plugin.

## Java API

### Modules

| module | what | depends on |
|---|---|---|
| `beezy-protocol` | The wire format only: one immutable class per message, `PacketCodec`, the kind registry (`PacketKind`), every field limit (`Protocol`). No platform types, no dependencies. | nothing |
| `beezy-api` | The public, documented interfaces plugins compile against: `BeezyClientApi`, `BeezyUser`, per-module services, handles, value objects, events, exceptions. | `beezy-protocol`, Adventure (compile only) |
| `beezy-core` | The implementation: sessions, what was shown to whom, feature gating, the client's limits, the delivery policy. Talks to the platform only through `PlatformAdapter` and `Scheduler`. Not an API. | `beezy-api` |
| `beezy-bukkit` | The plugin: Bukkit adapter, `BeezyBukkit` helpers, Bukkit events, `ServicesManager` registration, commands, config. | `beezy-core` |
| `examples` | A small sample plugin (`ExamplePlugin`), built but not shipped. | `beezy-api` |

Everything that runs on a server targets Java 8.

### Depending on the API

Nothing is published to a Maven repository yet. The `beezy-protocol` and `beezy-api` modules
have complete Maven publications (POM, sources and javadoc jars, group `dev.beezy`):

- **Locally:** `./gradlew publishToMavenLocal`, then `mavenLocal()` and
  `compileOnly("dev.beezy:beezy-api:1.0.0-SNAPSHOT")`.
- **From GitHub, through JitPack** (this repository has a `jitpack.yml`; it works once a release
  tag exists, which is not the case yet):

  ```kotlin
  repositories { maven("https://jitpack.io") }
  dependencies { compileOnly("com.github.BeezyClient.server-api:beezy-api:<tag>") }
  ```

Use `compileOnly`: the Beezy plugin provides the classes at runtime. Add `depend: [Beezy]` (or
`softdepend`) to your `plugin.yml`. The Bukkit helpers and events are in the plugin jar
(`dev.beezy.bukkit`): compile against it as a file if you want them.

### Getting started

```java
BeezyClientApi beezy = BeezyClientApi.get();          // or the ServicesManager
beezy.events().subscribe(UserHelloEvent.class, event -> {
    BeezyUser user = event.user();                     // this player uses Beezy
    HologramHandle shop = user.holograms().show(Hologram.builder("shop")
            .at(Position.of(10.5, 72, -3.5)).world(WorldKey.OVERWORLD)
            .line(Component.text("Shop", NamedTextColor.GOLD))
            .line("§7Right-click to trade")             // § codes become a component
            .duration(Duration.ofMinutes(10)).build());
    user.transfer("lobby.example.net").thenAccept(r -> getLogger().info("transfer: " + r.status()));
});
```

On Bukkit, `BeezyBukkit` turns Bukkit types into API values, and `BeezyHelloEvent` /
`BeezyTransferResultEvent` are ordinary Bukkit events:

```java
@EventHandler
public void onBeezy(BeezyHelloEvent event) {
    BeezyUser user = event.getUser();
    user.waypoints().show(BeezyBukkit.waypoint("spawn", spawn).name("Spawn").color(Color.GREEN).beam(true).build());
    BeezyBukkit.glow(user, boss, Color.RED);
    user.food().set(BeezyBukkit.food(Material.GOLDEN_CARROT, 6, 14.4));
}
```

See [`examples/`](examples/src/main/java/dev/beezy/examples/ExamplePlugin.java) for a complete
plugin.

### Concepts

- **Users.** `beezy.user(uuid)` is present once the player said Hello and until they leave.
  `user.client()` has the versions and features; `user.supports(Feature.BEAMS)` tells you in
  advance whether something would be sent.
- **Services.** Each module is a service on the user: `waypoints()`, `beams()`, `borders()`,
  `holograms()`, `glow()`, `nametags()`, `notifications()`, `cooldowns()`, `timers()`, `team()`,
  `richPresence()`, `settings()`, `nickHider()`, `food()`, `tnt()`, `heightLimit()`, plus
  `transfer(address)`, `reset()` and `resync()`.
- **Handles.** What has an id (waypoints, beams, borders, holograms, cooldowns, timers, glow,
  nametags) returns a handle: `update(value)`, `remove()`, `isActive()`, `expiresAt()`,
  `result()`. Showing the same id again replaces it. Timers can `pause()` and `resume()`.
- **Durations** (at most 24 h) are written into the message and handled by the client, which
  removes the thing by itself; the plugin schedules nothing. A handle's `isActive()` is simple
  arithmetic on the send time, and expired entries are dropped the next time they are looked at.
- **Audiences.** `beezy.all()`, `beezy.world(WorldKey)`, `beezy.audience(uuids)`,
  `BeezyAudience.of(users)`, filtered with `.supporting(Feature.X)` or `.filter(...)`:
  `audience.notify(...)`, `showWaypoint/Beam/Border/Hologram(...)` (a `HandleGroup`), `reset()`,
  each returning a per-user `DeliveryReport`.
- **Text.** Hologram and nametag lines are Adventure `Component`s (`ComponentLike`); overloads
  taking `String` read `§` codes (plain text works as is). Each line is at most 4096 bytes as JSON.
- **Values.** Immutable, with builders, checked against the protocol limits when built (string
  caps are UTF-8 bytes): bad input throws `IllegalArgumentException`, nothing is truncated.
  `Color` (opaque RGB; absent means the client's default), `WorldKey` (the dimension id the client
  knows, such as `minecraft:the_nether`; absent means any world), `Position`, `BlockPosition`,
  `Area`, `java.time.Duration`.
- **Settings** are typed: `SettingKey<Integer> MAX_ZOOM = SettingKey.integer("zoom", "max_zoom")`,
  `user.settings().set(MAX_ZOOM, 4)`, `release(...)`, `releaseAll()`, `overrides()`.
- **Transfers** return a `CompletionStage<TransferResult>` (`CONNECTING`, `REFUSED`,
  `INVALID_ADDRESS`); it completes exceptionally with `FeatureUnavailableException`,
  `RateLimitedException`, `UserDisconnectedException` or `TransferTimeoutException` (30 s).
- **Events.** `beezy.events()` delivers `UserHelloEvent`, `TransferResultEvent` and
  `UserDisconnectEvent` on the thread that received the client's message.
- **Thread safety.** Everything can be called from any thread. Sends to one user keep their order.

### Limits and the delivery policy

The plugin enforces the client's own limits before sending, so nothing is silently dropped on
the other side: 5 notifications every 10 seconds, 1 transfer a minute, and at most 64
waypoints, 32 beams, 16 borders, 64 holograms, 16 cooldowns, 8 timers, 128 glowing entities,
128 nametags and 256 food values at once per player.

When something cannot be sent (feature off, over a limit, player gone), `delivery-policy` in
config.yml decides:

- `skip` (default): nothing is thrown. Methods return a `SendResult` (`SENT`,
  `FEATURE_UNAVAILABLE`, `RATE_LIMITED`, `LIMIT_REACHED`, `DISCONNECTED`), handles are inactive
  and say why in `result()`, and `debug: true` logs each one.
- `throw`: the same cases throw `FeatureUnavailableException`, `RateLimitedException`,
  `LimitExceededException` or `UserDisconnectedException` (all `BeezyException`, unchecked).

To check up front instead, use `user.supports(feature)` or `user.require(feature)` (throws
`FeatureUnavailableException`).

### Stability and versions

The API follows [semantic versioning](https://semver.org) from 1.0.0 on: within a major
version, code compiled against `beezy-api` keeps working. Types and members are marked:

- `@Stable`: covered by that promise;
- `@Experimental`: may change in a minor version;
- `@Internal`: public for technical reasons only, do not use (this covers `beezy-core`);
- `@NonExtendable`: implemented by Beezy; call it, do not implement it (new methods may appear).

The annotations are this project's own (`dev.beezy.stability`), with no extra dependency.
Until 1.0.0 is released, anything may still change.

## Adventure

Text in the API is [Adventure](https://docs.advntr.dev) (`net.kyori.adventure.text.Component`),
so a component built for chat works for a hologram as is. Not every server has Adventure:

- **Paper 1.16.5 and newer** ship Adventure themselves.
- **Spigot, CraftBukkit and older Paper** do not.

So the plugin jar **bundles Adventure 4.17.0** (`adventure-api`, `adventure-key`, the legacy, JSON
and Gson serializers, `examination`, `option`), the newest version we checked that still runs on
Java 8. It is **not relocated**: it keeps the `net.kyori.adventure` package, so other plugins'
real `Component` objects can be passed to Beezy. That is safe because Bukkit's plugin class
loader asks the server first:

- On Paper, every `net.kyori.adventure` class resolves to the server's own copy, and the bundled
  one is never loaded. Beezy only calls methods that exist since Adventure 4.0 (`Component`,
  `GsonComponentSerializer.gson()`, `LegacyComponentSerializer.legacySection()`), so it works with
  whatever version the server ships, old or new (the integration tests run on Paper 26.3 with
  Adventure 5.2).
- On Spigot, the server has no Adventure, so the bundled copy is the only one. Plugins that want
  to pass components should compile against Adventure (`compileOnly`) and not shade their own
  copy, or must relocate theirs and use the `String` overloads instead.

**Gson is relocated** (to `dev.beezy.libs.gson`). Servers ship their own, older Gson (1.8.8 has
2.2.4, too old for the Gson serializer); with the server asked first, it would win over a
bundled one. Relocating ours avoids that. One consequence on Spigot: the bundled
`GsonComponentSerializer`'s methods that expose Gson types (`serializer()`, `populator()`) use
the relocated Gson. Use its `serialize`/`deserialize` only, or your own serializer.

The `beezy-api` artifact declares Adventure as `compileOnlyApi`: your build sees the real types,
and nothing is added to your runtime.

## Without the plugin

The protocol is small and documented in [`PROTOCOL.md`](PROTOCOL.md): plugin channel
`beezy:server`, a format byte, a kind byte, big-endian fields. Any server software can speak it
directly, or reuse `beezy-protocol` (no dependencies) and `beezy-core` with its own
`PlatformAdapter`.

## Build

```
./gradlew build
```

Gradle runs on Java 17 or newer; the integration tests need a **Java 25** toolchain (CI uses
Temurin 25; Gradle finds an installed one). Outputs:

- `beezy-bukkit/build/libs/Beezy-Bukkit-<version>.jar`: the plugin, everything included.
- `beezy-api/build/libs/`, `beezy-protocol/build/libs/`: the libraries with sources and javadoc.

The plugin compiles against spigot-api 1.8.8 so that nothing newer is used by accident; newer
methods (world keys, item keys, team colours, Folia's scheduler) are looked up at runtime.

### Tests

- `beezy-protocol`: every message kind byte for byte against hand-built payloads (including
  component lines), decoding back to the same bytes, every limit, malformed input.
- `beezy-core`: against a fake platform with a manual clock and scheduler: handles (update,
  replace, remove, expiry arithmetic, nothing scheduled for durations), feature gating under
  both policies, count limits, notification and transfer rate limits, transfer completion,
  timeout and disconnect, reset and resync, settings, audiences. JSON is checked against
  Adventure's own `GsonComponentSerializer`.
- `beezy-bukkit` integration tests (`./gradlew integrationTest`, part of `build`): the real
  plugin on [MockBukkit](https://mockbukkit.org) (`mockbukkit-v26.3`, paper-api 26.3): enable,
  channels and service registration, Reset on join, Hello and `BeezyHelloEvent`, the bytes of a
  hologram and a waypoint (through Paper's own Adventure 5), that a duration schedules no task
  and sends nothing when it runs out, command permissions and output, transfer result and
  `BeezyTransferResultEvent`, the answer timeout, and Folia's global region scheduler.

What MockBukkit cannot cover: 1.8.8-1.20 servers (it emulates Paper 26.3 only), Spigot without
Adventure (the bundled copy is tested only as part of the jar build), the
`CraftPlayer#addChannel` / `sendSupportedChannels` reflection (MockBukkit's players have
neither, so the plugin's fallback path runs), cancelling a Folia task (MockBukkit does not
implement `ScheduledTask#cancel`), real Folia threading, and real clients: what Beezy Client
draws from these messages is tested in the client.

## License

MIT: see [`LICENSE`](LICENSE). "Beezy" and "Beezy Client" are names of the Beezy Client project; the licence covers the code, not the names or logos.
