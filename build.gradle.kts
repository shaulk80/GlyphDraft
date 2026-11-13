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
        configurations = arrayOf("runtimeClasspath", "jvmRuntimeClasspath", "releaseRuntimeClasspath")
        filters = arrayOf(LicenseBundleNormalizer())
        allowedLicensesFile = rootProject.file("config/allowed-licenses.json")
    }
}

// Warnings are errors for all Kotlin code (the build must have no warnings).
subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}
