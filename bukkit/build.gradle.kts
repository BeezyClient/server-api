// Compiled against the oldest API we support (1.8.8) so nothing newer slips in by accident;
// newer-only methods are reached by reflection. Java 8 bytecode for the same reason.

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

dependencies {
    implementation(project(":api"))
    // Not transitive: its bungeecord-chat 1.8-SNAPSHOT dependency is no longer published, and we
    // use none of the libraries it pulls in (the server provides them at runtime anyway).
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("plugin.yml") { expand("version" to version) }
}

// One jar to drop in plugins/: the plugin plus the api classes, so other plugins can
// compileOnly against `api` and find it at runtime.
tasks.jar {
    archiveBaseName = "Beezy-Bukkit"
    dependsOn(":api:jar")
    from(provider { zipTree(project(":api").tasks.named<Jar>("jar").get().archiveFile) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
