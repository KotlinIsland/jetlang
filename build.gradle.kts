
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.composePlugin)
    alias(libs.plugins.compose)
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
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(compose.desktop.currentOs)
    implementation(compose.uiTooling)
    implementation(compose.preview)
    implementation(compose.material3)
    implementation(libs.coroutines)
    implementation(libs.coroutinesSwing)
    implementation(libs.kudzu)
    testImplementation(kotlin("test"))
    @OptIn(ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testCompileOnly(libs.junitJupiterParams)
}

compose.desktop {
    application {
        mainClass = "jetlang.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "jetlang"
            packageVersion = "1.0.0"

            windows {
                menu = true
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "2f2af9d7-dc22-41bc-93fd-4f6fad917dd8"
            }
        }
    }
}

// TODO: figure out how to test Compose with junit platform
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
