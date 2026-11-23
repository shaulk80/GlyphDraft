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

// Stand-alone throw-away build for M3 task 1; not part of the product build.
// Run from the repository root: .\gradlew -p spikes/translation run --args="bench en-de"
rootProject.name = "translation-spike"
