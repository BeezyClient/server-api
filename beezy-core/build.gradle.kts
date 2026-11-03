// The implementation behind beezy-api. Never touches a platform: everything platform-specific
// goes through dev.beezy.core.spi (PlatformAdapter, Scheduler).
description = "Beezy server core: sessions, state tracking, feature gating and rate limits"

val adventureVersion = property("adventureVersion") as String

dependencies {
    "api"(project(":beezy-api"))
    "compileOnlyApi"("net.kyori:adventure-api:$adventureVersion")
    "compileOnlyApi"("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    "compileOnlyApi"("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    "testImplementation"("net.kyori:adventure-api:$adventureVersion")
    "testImplementation"("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    "testImplementation"("net.kyori:adventure-text-serializer-gson:$adventureVersion")
}
