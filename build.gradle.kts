import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.bnorm.robocode") version "0.1.1"
    id("me.champeau.gradle.jmh") version "0.5.2"
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("com.jakewharton.picnic:picnic:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    jmh("org.openjdk.jmh:jmh-core:1.34")
    kaptJmh("org.openjdk.jmh:jmh-generator-annprocess:1.34")
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

jmh {
    jvmArgs.plusAssign(listOf("-Djmh.separateClasspathJAR=true"))
    duplicateClassesStrategy = DuplicatesStrategy.WARN
}

/*
 * Gradle source set, configurations, and tasks for running battles.
 */

val battles by sourceSets.registering
val battlesImplementation by configurations.getting {
    extendsFrom(configurations.robocode.get())
}

val battlesRuntimeOnly by configurations.getting {
    extendsFrom(configurations.robocodeRuntime.get())
}

dependencies {
    battlesImplementation("com.jakewharton.picnic:picnic:0.5.0")

    battlesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    battlesImplementation("com.squareup.retrofit2:retrofit:2.9.0")
    battlesImplementation("com.squareup.okhttp3:okhttp:4.9.3")
    battlesImplementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    battlesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    battlesImplementation(kotlin("test-junit5"))
    battlesImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    battlesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

// TODO Should there be a different task for each different battle?
val runBattles by project.tasks.registering(Test::class) {
    dependsOn("robotBin")
    // TODO Download robots for battles? Or is this part of tests?

    description = "Runs Robocode battles"
    group = "battles"

    useJUnitPlatform()
    testClassesDirs = battles.get().output.classesDirs
    classpath = battles.get().runtimeClasspath

    enableAssertions = false
    debug = false
}
