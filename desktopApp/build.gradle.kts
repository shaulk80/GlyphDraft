import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
}

// The ONNX Runtime jar carries natives for Windows, Linux and macOS (about 55 MB, mostly macOS debug symbols).
// The app image gets a copy with only the natives of the OS it is built on (packages are built on their own OS).
val onnxruntimeSource by configurations.creating {
    isTransitive = false
    isCanBeConsumed = false
}

val hostNativeFolder =
    run {
        val os = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "win" else "linux"
        val arch = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "aarch64" else "x64"
        "$os-$arch"
    }

val trimOnnxruntime =
    tasks.register<Jar>("trimOnnxruntime") {
        archiveFileName = "onnxruntime-trimmed.jar"
        destinationDirectory = layout.buildDirectory.dir("trimmed")
        from(provider { zipTree(onnxruntimeSource.singleFile) }) {
            exclude("ai/onnxruntime/native/**")
        }
        from(provider { zipTree(onnxruntimeSource.singleFile) }) {
            include("ai/onnxruntime/native/$hostNativeFolder/**")
        }
    }

configurations.runtimeClasspath {
    exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime")
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(compose.desktop.currentOs)
    implementation(libs.coroutines.swing)
    implementation(libs.filekit.dialogs)
    onnxruntimeSource(libs.onnxruntime.jvm)
    runtimeOnly(files(trimOnnxruntime))
    val javacppPlatform = rootProject.extra["javacppHostPlatform"]
    runtimeOnly("${libs.javacpp.get()}:$javacppPlatform")
    runtimeOnly("${libs.tesseract.get()}:$javacppPlatform")
    runtimeOnly("${libs.leptonica.get()}:$javacppPlatform")
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
            // What the installer and the app list show (the icons are drawn by scripts/icons/GenerateIcons.java).
            description = "Reads the text in an image on your device and can translate it. No account, images stay on the device."
            vendor = "GlyphDraft"
            copyright = "Copyright 2026 GlyphDraft contributors. Apache License 2.0."
            licenseFile.set(rootProject.file("LICENSE"))
            windows {
                iconFile.set(project.file("icons/glyphdraft.ico"))
                menuGroup = "GlyphDraft"
                shortcut = true
                // Stays the same for every version, so that a newer MSI upgrades the installed one.
                upgradeUuid = "c79dceb5-f4ec-49b2-aef6-e3dea0f3deff"
            }
            linux {
                iconFile.set(project.file("icons/glyphdraft.png"))
                packageName = "glyphdraft"
                appCategory = "Graphics"
                menuGroup = "Graphics"
                shortcut = true
            }
            // Needed by FileKit (JNA, D-Bus) in the jlink runtime; list from :desktopApp:suggestRuntimeModules.
            modules("java.instrument", "jdk.security.auth", "jdk.unsupported")
            // No hsperfdata file in the system temp folder: the app writes only into its data folder.
            jvmArgs("-XX:-UsePerfData")
        }
    }
}
