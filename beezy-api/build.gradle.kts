// The public API plugins compile against. Adventure is `compileOnlyApi`: callers see the real
// types, and at runtime they come from the server (Paper) or from the Beezy plugin (Spigot).
description = "Beezy server API: the stable interfaces plugins use to talk to Beezy Client players"

apply(from = rootProject.file("gradle/publishing.gradle.kts"))

val adventureVersion = property("adventureVersion") as String

dependencies {
    "api"(project(":beezy-protocol"))
    "compileOnlyApi"("net.kyori:adventure-api:$adventureVersion")
    "compileOnlyApi"("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    "testImplementation"("net.kyori:adventure-api:$adventureVersion")
    "testImplementation"("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
}
