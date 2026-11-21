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

    // The release key (decision 0002) is never in the repo. CI decodes it from a secret on release runs and sets these
    // variables; without all four the release APK is signed with the debug key, which is fine for builds nobody updates from.
    val releaseKey = ReleaseKey.fromEnvironment { providers.environmentVariable(it).orNull }
    if (releaseKey == null) {
        logger.lifecycle("androidApp: no release key in the environment, the release APK is signed with the debug key")
    }

    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile = file(releaseKey.file)
                storePassword = releaseKey.storePassword
                keyAlias = releaseKey.alias
                keyPassword = releaseKey.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (releaseKey != null) "release" else "debug")
        }
    }
}

/** The four variables that describe the release keystore. */
class ReleaseKey(val file: String, val storePassword: String, val alias: String, val keyPassword: String) {
    companion object {
        fun fromEnvironment(read: (String) -> String?): ReleaseKey? {
            val file = read("GLYPHDRAFT_KEYSTORE_FILE")
            val storePassword = read("GLYPHDRAFT_KEYSTORE_PASSWORD")
            val alias = read("GLYPHDRAFT_KEY_ALIAS")
            val keyPassword = read("GLYPHDRAFT_KEY_PASSWORD")
            if (file == null || storePassword == null || alias == null || keyPassword == null) {
                return null
            }
            return ReleaseKey(file, storePassword, alias, keyPassword)
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
