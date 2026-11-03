// Beezy server plugin: the `beezy:server` protocol (docs/SERVER-API.md) for servers.
//
//   api       plain Java 8 library: encoder/decoder, value classes, the BeezyServerApi facade
//   bukkit    Bukkit / Spigot / Paper / Folia plugin (1.8.8 to latest), shades `api`

rootProject.name = "beezy-server-api"

include("api", "bukkit")
