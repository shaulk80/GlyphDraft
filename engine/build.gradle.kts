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

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
        }
        jvmMain.dependencies {
            api(libs.onnxruntime.jvm)
        }
        androidMain.dependencies {
            api(libs.onnxruntime.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }
    }
}
