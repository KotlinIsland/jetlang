plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

group = "com.kotlinisland"
version = "0.1-SNAPSHOT"

kotlin {
    jvmToolchain(23)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(libs.kudzu)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}
