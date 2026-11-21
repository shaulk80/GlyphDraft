import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(21)
    jvm()
    android {
        namespace = "com.glyphdraft.engine"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
    }

    // ONNX Runtime has the same Java API on both targets (JVM jar, Android AAR), so the code that uses it is shared
    // by jvm and android in one source set; only the dependency differs.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                // withAndroidTarget() does not match the com.android.kotlin.multiplatform.library plugin.
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    // The fixture tests with the real models run only when these variables are set (scripts/build.ps1 sets the first
    // one on CI). They are task inputs, so a run with models is never replaced by a cached run without them.
    tasks.withType<Test>().configureEach {
        inputs.property("networkTests", providers.environmentVariable("GLYPHDRAFT_NETWORK_TESTS").orElse(""))
        inputs.property("modelsDirectory", providers.environmentVariable("GLYPHDRAFT_MODELS_DIR").orElse(""))
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
        }
        // The JavaCPP presets (Tesseract, Leptonica) have the same Java API on both targets as well. Their native
        // libraries are classifier jars chosen by the app that is built (desktopApp: host OS; androidApp: ABIs).
        jvmMain.dependencies {
            api(libs.onnxruntime.jvm)
            api(libs.tesseract)
            api(libs.leptonica)
        }
        androidMain.dependencies {
            api(libs.onnxruntime.android)
            api(libs.tesseract)
            api(libs.leptonica)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }
        jvmTest.dependencies {
            val platform = rootProject.extra["javacppHostPlatform"]
            runtimeOnly("${libs.javacpp.get()}:$platform")
            runtimeOnly("${libs.tesseract.get()}:$platform")
            runtimeOnly("${libs.leptonica.get()}:$platform")
        }
    }
}
