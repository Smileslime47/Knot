plugins {
    kotlin("jvm") version "2.3.0"
}

group = "moe.saikyo47"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    maxHeapSize = "2g"
    minHeapSize = "512m"
}