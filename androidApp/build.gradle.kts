plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.glyphdraft.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.glyphdraft.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = providers.gradleProperty("appVersion").get()
        // ONNX Runtime ships four ABIs (about 15 MB each); phones are arm64, the emulator on x86_64 PCs is x86_64.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    packaging {
        resources {
            // The JavaCPP jars carry GraalVM native-image metadata with the same path in each; Android does not use it.
            excludes += "META-INF/native-image/**"
            // The same jars also carry C headers, CMake and pkg-config files under lib/<abi>/. The installer extracts
            // everything under lib/<abi>/ as a native library and rejects the APK ("Failed to extract native libraries").
            excludes += listOf("lib/*/include/**", "lib/*/lib/**", "lib/*/tesseract", "lib/*/tesseract/**", "lib/*/share/**")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // M0: signed with the debug key so the APK installs. A release key is a later decision (docs/decisions).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":engine"))
    implementation(libs.androidx.activity.compose)
    // Native libraries of the JavaCPP presets, one jar per ABI of the filter above.
    for (platform in listOf("android-arm64", "android-x86_64")) {
        runtimeOnly("${libs.javacpp.get()}:$platform")
        runtimeOnly("${libs.tesseract.get()}:$platform")
        runtimeOnly("${libs.leptonica.get()}:$platform")
    }
}
