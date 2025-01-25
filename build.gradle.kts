plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.qodana") version "2024.3.4"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(23)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
tasks.test {
    useJUnitPlatform()
}
