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
}
