import com.github.jk1.license.filter.LicenseBundleNormalizer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.license.report) apply false
}

// `checkLicense` (run in every module) fails when a runtime dependency has a license that is not in
// config/allowed-licenses.json. New licenses are added there only after a deliberate review.
// The plugin is applied per module because it cannot safely resolve other modules' configurations.
subprojects {
    pluginManager.apply("com.github.jk1.dependency-license-report")
    extensions.configure<com.github.jk1.license.LicenseReportExtension> {
        configurations = arrayOf("runtimeClasspath", "jvmRuntimeClasspath", "androidRuntimeClasspath", "releaseRuntimeClasspath")
        filters = arrayOf(LicenseBundleNormalizer())
        allowedLicensesFile = rootProject.file("config/allowed-licenses.json")
    }
}

// The JavaCPP platform name of the OS the build runs on (desktop packages are built on their own OS). JavaCPP
// libraries are added per platform as classifier jars; only the host's jars go into the app image.
extra["javacppHostPlatform"] =
    run {
        val os = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "windows" else "linux"
        val arch = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "arm64" else "x86_64"
        "$os-$arch"
    }

// Warnings are errors for all Kotlin code (the build must have no warnings).
subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}
