plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("maven-publish")
    id("signing")
}

group = "net.dankito.readability4j"
version = "1.0.8"

val mavenArtifactId = "readability4j"

object versions {
    const val kotlin = "1.9.10"
    const val slf4j = "2.0.9"
    const val jsoup = "1.16.1"
    const val jackson = "2.15.2"
    const val logback = "1.4.11"
    const val diffUtils = "4.15"
    const val okHttp = "4.11.0"
    const val junit = "4.13.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}")
    implementation("org.slf4j:slf4j-api:${versions.slf4j}")
    implementation("org.jsoup:jsoup:${versions.jsoup}")

    testImplementation("junit:junit:${versions.junit}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions.jackson}")
    testImplementation("io.github.java-diff-utils:java-diff-utils:${versions.diffUtils}")
    testImplementation("com.squareup.okhttp3:okhttp:${versions.okHttp}")
    testImplementation("ch.qos.logback:logback-core:${versions.logback}")
    testImplementation("ch.qos.logback:logback-classic:${versions.logback}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${versions.kotlin}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// Tasks for generating additional artifacts
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks.named("javadocJar"))
            artifact(tasks.named("sourcesJar"))

            pom {
                name.set(mavenArtifactId)
                description.set("A Kotlin port of Mozilla's Readability. It extracts a website's relevant content and removes all clutter from it.")
                url.set("https://github.com/dankito/Readability4J")

                scm {
                    connection.set("scm:git:git://github.com/dankito/Readability4J.git")
                    developerConnection.set("scm:git:git@github.com:dankito/Readability4J.git")
                    url.set("https://github.com/dankito/Readability4J")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("dankito")
                        name.set("Christian Dankl")
                        email.set("maven@dankito.net")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        project.findProperty("signing.keyId") as String? ?: "",
        project.findProperty("signing.secretKeyRingFile") as String? ?: "",
        project.findProperty("signing.password") as String? ?: ""
    )
    sign(publishing.publications["mavenJava"])
}
