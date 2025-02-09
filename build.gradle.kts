
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
version = "1.0.0"

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
    implementation(compose.materialIconsExtended)
    implementation(libs.coroutines)
    implementation(libs.kudzu)
    testImplementation(kotlin("test"))
    @OptIn(ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(libs.coroutinesTest)
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

                iconFile.set(project.file("icon.ico"))
            }
        }
    }
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
