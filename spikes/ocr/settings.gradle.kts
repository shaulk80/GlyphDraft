pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Stand-alone throw-away build for M2 task 1; not part of the product build.
// Run from the repository root: .\gradlew -p spikes/ocr run --args="bench"
rootProject.name = "ocr-spike"
