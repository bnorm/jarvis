import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.bnorm.robocode")
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("com.jakewharton.picnic:picnic:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.robocodeRun {
    jvmArgs = (jvmArgs ?: emptyList()) + listOf(
        "-Ddebug=true",
        "-DNOSECURITY=true",
        "-Dsun.io.useCanonCaches=false"
    )
}

robocode {
    downloadVersion = "1.9.3.9"
    robots {
        register("Jarvis") {
            classPath = "bnorm.Jarvis"
            version = project.version.toString()
        }
        register("JarvisT") {
            classPath = "bnorm.JarvisT"
            version = project.version.toString()
            description = "Jarvis - Targeting Only"
        }
        register("JarvisM") {
            classPath = "bnorm.JarvisM"
            version = project.version.toString()
            description = "Jarvis - Movement Only"
        }
    }
}

/*
 * Gradle source set, configurations, and tasks for running battles.
 */

val battles by sourceSets.registering
val battlesImplementation by configurations.getting
val battlesRuntimeOnly by configurations.getting

dependencies {
    battlesImplementation("com.jakewharton.picnic:picnic:0.6.0")
    battlesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.0")
    battlesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    battlesImplementation(kotlin("test-junit5"))
    battlesImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    battlesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

val runBattles by project.tasks.registering(Test::class) {
    dependsOn("robotBin")

    description = "Runs Robocode battles"
    group = "battles"

    useJUnitPlatform()
    testClassesDirs = battles.get().output.classesDirs
    classpath = battles.get().runtimeClasspath

    testLogging.showStandardStreams = true
}
