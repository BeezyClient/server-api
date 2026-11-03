// Maven publication for the libraries other plugins compile against (beezy-protocol, beezy-api).
// Configured, not published anywhere: `./gradlew publishToMavenLocal` fills ~/.m2, and JitPack
// builds the same publications from a git tag (see README, "Depending on the API").

apply(plugin = "maven-publish")

configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/BeezyClient/server-api")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("ImBuzz")
                        name.set("ImBuzz")
                    }
                }
                scm {
                    url.set("https://github.com/BeezyClient/server-api")
                    connection.set("scm:git:https://github.com/BeezyClient/server-api.git")
                }
            }
        }
    }
}
