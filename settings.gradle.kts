// Beezy server plugin: the `beezy:server` protocol (PROTOCOL.md) for Minecraft servers.
//
//   beezy-protocol   wire format only: packets, encoder/decoder, kind registry, validation
//   beezy-api        the public, stable interfaces plugins compile against (Adventure text)
//   beezy-core       the implementation: sessions, state, gating, rate limits; platform SPI
//   beezy-bukkit     the Bukkit / Spigot / Paper / Folia plugin (1.8.8 to latest)
//   examples         a small sample plugin using the API (built, never shipped)

rootProject.name = "beezy-server-api"

include("beezy-protocol", "beezy-api", "beezy-core", "beezy-bukkit", "examples")
