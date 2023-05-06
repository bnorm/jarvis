import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":jarvis"))
    implementation(compose.desktop.currentOs)

    implementation("com.jakewharton.picnic:picnic:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
        )
    }
}

compose.desktop {
    application {
        mainClass = "bnorm.VisualKt"
    }
}
