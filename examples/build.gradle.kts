// A small sample plugin that uses the API the way a server plugin would. Built (so it keeps
// compiling against the API), never shipped.

val adventureVersion = property("adventureVersion") as String

dependencies {
    "compileOnly"(project(":beezy-api"))
    "compileOnly"("net.kyori:adventure-api:$adventureVersion")
    "compileOnly"("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
}
