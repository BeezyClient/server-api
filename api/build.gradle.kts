// No runtime dependencies: this runs inside 1.8.8 servers on Java 8 as well as on Java 25 proxies.

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName = "beezy-server-api"
}
