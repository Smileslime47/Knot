plugins {
    kotlin("jvm") version "2.3.0"
    `maven-publish`
    signing
}

group = "moe.smileslime47"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    maxHeapSize = "2g"
    minHeapSize = "512m"
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["kotlin"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            artifactId = "Knot"
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("Knot")
                description.set("A Rope Data Structure Library Implemented in Kotlin")
                url.set("https://github.com/Smileslime47/Knot")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("Smile_slime_47")
                        name.set("Smile_slime_47")
                        email.set("Smile_slime_47@outlook.com")
                    }
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/Smileslime47/Knot/issues")
                }
                scm {
                    connection.set("scm:git:git@github.com:Smileslime47/Knot.git")
                    developerConnection.set("scm:git:ssh://github.com/Smileslime47/Knot.git")
                    url.set("https://github.com/Smileslime47/Knot")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalRepo"
            url = uri("${rootProject.buildDir}/repo")
        }
    }
}

signing {
    sign(publishing.publications["release"])
}