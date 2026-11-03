// The plugin: Bukkit / Spigot / Paper / Folia, 1.8.8 to latest, Java 8.
//
// Compiled against the oldest API we support (spigot-api 1.8.8) so nothing newer slips in by
// accident; newer methods are reached by reflection. Adventure is bundled for servers that do
// not have it (see README, "Adventure"). Integration tests run the real plugin on MockBukkit
// (paper-api 26.3, Java 25) in their own source set.

plugins {
    id("com.gradleup.shadow")
}

val adventureVersion = property("adventureVersion") as String

dependencies {
    "implementation"(project(":beezy-core"))
    // Bundled into the plugin jar, unrelocated (Gson relocated): see the shadowJar task.
    "implementation"("net.kyori:adventure-api:$adventureVersion")
    "implementation"("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    "implementation"("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    // Not transitive: its bungeecord-chat 1.8-SNAPSHOT dependency is no longer published.
    "compileOnly"("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("plugin.yml") { expand("version" to version) }
}

// One jar to drop in plugins/: plugin, protocol, API, core and Adventure.
//
// Adventure stays in its own package (net.kyori.adventure) so that plugins compiled against the
// real Adventure types can pass components to Beezy. Bukkit's plugin class loader asks the
// server first, so on Paper (which ships Adventure) the server's classes always win and ours
// are never loaded. Gson is relocated: servers ship their own, older Gson (1.8.8 has 2.2.4),
// which would otherwise win over ours for the same reason and break the serializer.
tasks.shadowJar {
    archiveBaseName = "Beezy-Bukkit"
    archiveClassifier = ""
    relocate("com.google.gson", "dev.beezy.libs.gson")
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }
    exclude("module-info.class", "META-INF/versions/*/module-info.class", "META-INF/maven/**", "META-INF/proguard/**")
    mergeServiceFiles()
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// ---- MockBukkit integration tests ---------------------------------------------------------------

val sourceSets = project.extensions.getByType<SourceSetContainer>()
val mainOutput = sourceSets.getByName("main").output
val integrationTest: SourceSet = sourceSets.create("integrationTest") {
    compileClasspath += mainOutput
    runtimeClasspath += mainOutput
}

dependencies {
    "integrationTestImplementation"(project(":beezy-core"))
    "integrationTestImplementation"("io.papermc.paper:paper-api:26.3.build.238-stable")
    "integrationTestImplementation"("org.mockbukkit.mockbukkit:mockbukkit-v26.3:4.119.0")
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter:6.1.3")
    "integrationTestRuntimeOnly"("org.junit.platform:junit-platform-launcher:6.1.3")
}

// MockBukkit and paper-api 26.3 need Java 25 (the plugin itself stays Java 8). A Java 25
// toolchain: Byte Buddy, which MockBukkit uses to load plugins, cannot define classes on 26.
val toolchains = project.extensions.getByType<JavaToolchainService>()
val java25 = toolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }

tasks.named<JavaCompile>("compileIntegrationTestJava") {
    javaCompiler = toolchains.compilerFor { languageVersion = JavaLanguageVersion.of(25) }
    options.release = 25
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs the plugin on MockBukkit (Paper 26.3)."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    javaLauncher = java25
}

tasks.check {
    dependsOn(integrationTestTask)
}
