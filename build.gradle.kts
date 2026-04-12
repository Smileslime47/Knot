plugins {
    kotlin("jvm") version "2.3.0"
    `maven-publish`
    signing
}

group = "moe.smileslime47"
version = "1.0-SNAPSHOT"

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

            artifactId = "Knot"
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("Knot")
                description.set("A Rope Data Structure Library Impl emented in Kotlin")
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
                        name.set("Smile slime 47")
                        email.set("Smile_slime_47@outlook.com")
                    }
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
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/service/local/repositories/snapshots/content/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = project.findProperty("ossrhUsername")?.toString()
                password = project.findProperty("ossrhPassword")?.toString()
            }
        }
        maven {
            name = "LocalTest"
            url = uri("${rootProject.buildDir}/repo")
        }
    }
}

signing {
    sign(publishing.publications["release"])
}