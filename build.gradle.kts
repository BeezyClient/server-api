subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        // spigot-api
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "SpigotMC" }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // `--release 8` is obsolete on new JDKs but still supported; keep the build quiet about it.
        options.compilerArgs.add("-Xlint:-options")
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
