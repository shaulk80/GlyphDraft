plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.32.0")
    implementation("org.yaml:snakeyaml:2.7")
    implementation("net.sourceforge.tess4j:tess4j:5.21.0")
    // Same Tesseract through JavaCPP: natives exist for Windows, Linux and Android.
    implementation("org.bytedeco:tesseract:5.5.3-1.5.14")
    runtimeOnly("org.bytedeco:tesseract:5.5.3-1.5.14:windows-x86_64")
    runtimeOnly("org.bytedeco:leptonica:1.87.0-1.5.14:windows-x86_64")
    runtimeOnly("org.bytedeco:javacpp:1.5.14:windows-x86_64")
}

application {
    mainClass.set("spike.MainKt")
    // Repository root is the working directory so that data/ and engine/ paths resolve.
    tasks.named<JavaExec>("run") {
        workingDir = rootProject.projectDir.parentFile.parentFile
        // Extra JVM arguments, e.g. -PjvmArg=-Donnxruntime.native.path=C:/x
        providers.gradleProperty("jvmArg").orNull?.let { jvmArgs(it) }
    }
}
