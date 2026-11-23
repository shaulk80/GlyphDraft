plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.32.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("spike.MainKt")
    tasks.named<JavaExec>("run") {
        // Repository root is the working directory so that data/ and engine/ paths resolve.
        workingDir = rootProject.projectDir.parentFile.parentFile
        providers.gradleProperty("jvmArg").orNull?.let { jvmArgs(it) }
    }
}
