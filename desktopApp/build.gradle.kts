import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "com.glyphdraft.desktop.MainKt"
        nativeDistributions {
            // The app image (createDistributable) needs no installer tooling; installers are a later milestone.
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GlyphDraft"
            packageVersion = providers.gradleProperty("appVersion").get()
        }
    }
}
