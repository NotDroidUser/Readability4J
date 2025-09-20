import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

plugins {
    java
    signing
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

group = "net.dankito.readability4j"
version = "2.0.0-beta"

val mavenArtifactId = "readability4j"


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jsoup)
    implementation(libs.slf4j.api)
    implementation(libs.jackson.kotlin) //for LD-Json

    testImplementation(libs.junit)
    testImplementation(libs.htmlunit)
    testImplementation(libs.jackson.kotlin)
    testImplementation(libs.java.diff.utils)
    testImplementation(libs.okHttp3)
    testImplementation(libs.logback.core)
    testImplementation(libs.logback.classic)
    testImplementation(libs.kotlin.test)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<KotlinJvmCompilerOptions>> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
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
