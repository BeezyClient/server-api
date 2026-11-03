plugins {
    id("com.gradleup.shadow") version "9.3.0" apply false
}

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        // spigot-api 1.8.8
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "SpigotMC" }
        // paper-api, only for the MockBukkit integration tests of beezy-bukkit
        maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.4")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Everything that runs on a server targets Java 8 (1.8.8 servers still run on it).
        options.release = 8
        // `--release 8` is obsolete on new JDKs but still supported; keep the build quiet about it.
        options.compilerArgs.addAll(listOf("-Xlint:-options", "-parameters"))
    }

    tasks.withType<Javadoc>().configureEach {
        val o = options as StandardJavadocDocletOptions
        o.encoding = "UTF-8"
        o.addStringOption("Xdoclint:all,-missing", "-quiet")
        o.tags("apiNote:a:API Note:", "implNote:a:Implementation Note:")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
